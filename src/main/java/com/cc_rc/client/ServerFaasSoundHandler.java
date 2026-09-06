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

        // 每 SCAN_INTERVAL tick 才做全量扫描
        if (++tickCounter % SCAN_INTERVAL != 0) return;

        BlockPos center = mc.player.blockPosition();
        double maxDistSq = (SCAN_RADIUS + 8.0) * (SCAN_RADIUS + 8.0);

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
            FaasLoopingSound sound = new FaasLoopingSound(level, ModSounds.SERVER_NOISE.get(), immutable);
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
     */
    private static final class FaasLoopingSound extends AbstractTickableSoundInstance {
        private final ClientLevel level;
        private final BlockPos pos;

        private FaasLoopingSound(ClientLevel level, SoundEvent sound, BlockPos pos) {
            super(sound, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.level = level;
            this.pos = pos;
            this.looping = true;
            this.delay = 0;
            this.volume = 1.0F;
            this.pitch = 1.0F;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            this.x = pos.getX() + 0.5;
            this.y = pos.getY() + 0.5;
            this.z = pos.getZ() + 0.5;
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