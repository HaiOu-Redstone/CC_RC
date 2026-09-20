package com.cc_rc.client;

import com.cc_rc.CcRc;
import com.cc_rc.ModSounds;
import com.cc_rc.block.server_faas.ServerFaasBlock;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * F.A.A.S服务器 循环环境音效管理器（客户端）。
 *
 * 原实现（animateTick 概率 playLocalSound）有两处缺陷：概率触发导致时有时无；
 * 音效时长较长时多次触发互相重叠。这里改为"循环音效实例 + 距离开关"：
 *   - 每 20 tick（1 秒）扫描玩家周围 16 格内的 ServerFaasBlock；
 *   - 玩家在范围内且该方块尚未有音效实例 → 创建 looping 的
 *     FaasLoopingSound 并交给 SoundManager 持续循环播放（杜绝重叠/断续）；
 *   - 每 tick 校验：方块已被破坏、玩家远离、或离开世界/维度 → 停止并移除实例。
 * 作用域仅客户端（Dist.CLIENT），服务端不加载本类。
 */
@Mod.EventBusSubscriber(modid = CcRc.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerFaasSoundHandler {

    // 扫描半径（格）：与音效线性衰减距离一致，超出基本听不到
    private static final int SCAN_RADIUS = 16;

    // 扫描间隔（tick）：每 20 tick（1 秒）全量扫描一次，避免每 tick 开销
    private static final int SCAN_INTERVAL = 20;

    /** 当前活跃的循环音效：方块坐标 -> 音效实例 */
    private static final Map<BlockPos, FaasLoopingSound> ACTIVE_SOUNDS = new HashMap<>();

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            // 不在世界内（标题界面/死亡重生前等）：立即清理全部实例，防止音效残留
            stopAll();
            return;
        }

        // 每 tick 手动刷新各活跃音效的音量（距离衰减）。
        // 原因：SoundEngine 对循环音效（looping）的音量只在播放/低频 tick 时按初始距离设定，
        // 玩家移动后不会随距离变化，导致听感上"没有衰减"；这里改为完全手动控制。
        Player player = mc.player;
        for (FaasLoopingSound sound : ACTIVE_SOUNDS.values()) {
            sound.updateVolume(player);
        }

        // 每 SCAN_INTERVAL tick 才做全量扫描
        if (++tickCounter % SCAN_INTERVAL != 0) return;

        BlockPos center = mc.player.blockPosition();
        // 停止距离 = 扫描半径 + 4 格裕量：16 格衰减到无声后再多留 4 格，避免边界来回抖动
        double maxDistSq = (SCAN_RADIUS + 4.0) * (SCAN_RADIUS + 4.0);

        // 1) 清理失效实例：方块被破坏 / 玩家已远离 / 跨维度后旧坐标变为非 FAAS
        Iterator<Map.Entry<BlockPos, FaasLoopingSound>> iterator = ACTIVE_SOUNDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, FaasLoopingSound> entry = iterator.next();
            BlockPos pos = entry.getKey();
            FaasLoopingSound sound = entry.getValue();
            boolean tooFar = pos.distSqr(center) > maxDistSq;
            boolean gone = !(level.getBlockState(pos).getBlock() instanceof ServerFaasBlock);
            if (tooFar || gone) {
                sound.stopLoop();
                iterator.remove();
            }
        }

        // 2) 新发现：扫描范围内存在 FAAS 方块但尚无音效 → 创建并循环播放
        BlockPos min = center.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS);
        BlockPos max = center.offset(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            // betweenClosed 迭代器复用一个可变 BlockPos，作为 key 前必须不可变
            BlockPos immutable = pos.immutable();
            if (ACTIVE_SOUNDS.containsKey(immutable)) continue;
            BlockState state = level.getBlockState(immutable);
            if (!(state.getBlock() instanceof ServerFaasBlock)) continue;
            FaasLoopingSound sound = new FaasLoopingSound(level, ModSounds.SERVER_NOISE.get(), immutable, player);
            ACTIVE_SOUNDS.put(immutable, sound);
            mc.getSoundManager().play(sound);
        }
    }

    /** 停止并清空全部循环音效实例（离开世界/无玩家时调用）。 */
    private static void stopAll() {
        for (FaasLoopingSound sound : ACTIVE_SOUNDS.values()) {
            sound.stopLoop();
        }
        ACTIVE_SOUNDS.clear();
    }

    /**
     * 循环音效实例：固定在方块中心播放 server_noise，跟随方块存在性自动停止。
     * looping=true 使 SoundManager 无缝循环该音频，避免原实现"概率触发 + 时长重叠"问题。
     * 归类为 SoundSource.BLOCKS（受游戏设置「音效/方块」滑块控制音量）。
     * 距离衰减不再依赖 SoundEngine（循环音效的低频 tick 不会随玩家移动更新音量），
     * 而是由 updateVolume() 每 tick 手动按玩家与方块距离计算线性衰减（16 格内 100%→0%）。
     */
    private static final class FaasLoopingSound extends AbstractTickableSoundInstance {
        // 线性衰减范围（格）：与扫描半径一致，超出后音量归零
        private static final float FADE_RANGE = 16.0F;

        // 基础音量：满格时 100%
        private static final float BASE_VOLUME = 1.0F;

        private final ClientLevel level;
        private final BlockPos pos;

        private FaasLoopingSound(ClientLevel level, SoundEvent sound, BlockPos pos, Player player) {
            super(sound, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.level = level;
            this.pos = pos;
            this.looping = true;
            this.delay = 0;
            // 初始音量 = 创建时玩家与方块的实际衰减音量（修复：原固定 BASE_VOLUME=1.0，
            // 玩家在 16 格边缘进入范围也满音量开播 → 每次进入/重建都“炸响”一声；
            // 现在 edge 处接近 0，开播即小声，随后由 updateVolume() 平滑收敛）
            double dist = Math.sqrt(player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            this.volume = (float) Mth.clamp(1.0 - dist / FADE_RANGE, 0.0, 1.0) * BASE_VOLUME;
            this.pitch = 1.0F;
            // 关键：标记为相对播放 + 关闭引擎衰减，让 SoundEngine 不再按距离改音量，
            // 音量完全由 updateVolume() 手动控制，保证玩家移动时衰减必然生效。
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.x = pos.getX() + 0.5;
            this.y = pos.getY() + 0.5;
            this.z = pos.getZ() + 0.5;
        }

        /**
         * 按玩家与音源的距离直接设置音量（绝对设置，无 lerp 平滑）：
         * SoundEngine 每 tick 读取实例的 volume 字段并应用（反编译确认 m_120326_ 每 tick
         * 调用 m_120324_ 计算音量），故静止时音量严格恒定、移动时严格跟随距离，
         * 杜绝任何缓升缓降/周期性起伏。由管理器在每 tick 客户端事件中调用。
         */
        void updateVolume(Player player) {
            double dist = Math.sqrt(player.distanceToSqr(x, y, z));
            this.volume = (float) Mth.clamp(1.0 - dist / FADE_RANGE, 0.0, 1.0) * BASE_VOLUME;
        }

        @Override
        public SoundSource getSource() {
            // 方块类音源：音量跟随「音效/方块」滑块，而非主音量
            return SoundSource.BLOCKS;
        }

        @Override
        public SoundInstance.Attenuation getAttenuation() {
            // 引擎衰减已关闭（relative=true），衰减由 updateVolume() 手动实现
            return SoundInstance.Attenuation.NONE;
        }

        @Override
        public void tick() {
            // 方块被破坏/替换后自动停止本实例（SoundManager 随后将其移出播放队列）
            if (!(level.getBlockState(pos).getBlock() instanceof ServerFaasBlock)) {
                this.stopLoop();
            }
        }

        /** 外部触发的停止入口：stop() 为 protected，包一层公开方法供管理器调用。 */
        private void stopLoop() {
            this.stop();
        }
    }
}