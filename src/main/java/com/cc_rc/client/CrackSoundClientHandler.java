package com.cc_rc.client;

import com.cc_rc.CcRc;
import com.cc_rc.ModSounds;
import com.cc_rc.block.password_inputer.PasswordInputerBlock;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 破解音效客户端处理器（Dist.CLIENT）
 *
 * 收到 CrackSoundPacket(start=true) → 在密码输入器位置播放循环破解音效并登记；
 * 收到 start=false → 停止并移除对应音效。
 * 玩家退出/切换维度时统一清理全部实例。
 * 音效自身兜底：方块消失或玩家远离（>32 格）自动停止，防止异常残留。
 */
@Mod.EventBusSubscriber(modid = CcRc.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CrackSoundClientHandler {

    // 方块坐标 -> 正在播放的循环破解音效
    private static final Map<BlockPos, CrackLoopingSound> ACTIVE = new HashMap<>();

    /** 开始播放破解音效（若该位置已在播放则先停止旧实例）。 */
    public static void startCrack(BlockPos pos) {
        stopCrack(pos);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level instanceof ClientLevel) {
            ClientLevel clientLevel = (ClientLevel) mc.level;
            CrackLoopingSound sound = new CrackLoopingSound(clientLevel, pos);
            ACTIVE.put(pos, sound);
            mc.getSoundManager().play(sound);
        }
    }

    /** 停止并移除指定位置的破解音效。 */
    public static void stopCrack(BlockPos pos) {
        CrackLoopingSound sound = ACTIVE.remove(pos);
        if (sound != null) {
            sound.stopLoop();
        }
    }

    /** 停止全部破解音效（退出世界/切换维度时调用）。 */
    public static void stopAll() {
        Iterator<Map.Entry<BlockPos, CrackLoopingSound>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            it.next().getValue().stopLoop();
            it.remove();
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        stopAll();
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        stopAll();
    }

    /**
     * 循环破解音效：固定于密码输入器方块中心播放，收到停止包或兜底条件触发时结束。
     */
    private static final class CrackLoopingSound extends AbstractTickableSoundInstance {
        private final ClientLevel level;
        private final BlockPos pos;

        private CrackLoopingSound(ClientLevel level, BlockPos pos) {
            super(ModSounds.PASSWORD_CRACK.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.level = level;
            this.pos = pos;
            this.looping = true;
            this.delay = 0;
            this.volume = 1.0F;
            this.pitch = 1.0F;
            this.relative = false;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            this.x = pos.getX() + 0.5;
            this.y = pos.getY() + 0.5;
            this.z = pos.getZ() + 0.5;
        }

        @Override
        public void tick() {
            // 兜底：玩家离线/方块已非密码输入器/玩家远离时自动停止并清理
            Player player = Minecraft.getInstance().player;
            boolean stale = player == null
                    || !(level.getBlockState(pos).getBlock() instanceof PasswordInputerBlock)
                    || player.distanceToSqr(x, y, z) > 32.0 * 32.0;
            if (stale) {
                this.stopLoop();
                ACTIVE.remove(pos);
            }
        }

        void stopLoop() {
            this.stop();
        }
    }
}