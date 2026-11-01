package com.cc_rc.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * 音频播放指令 /ccrc sound 的通用客户端音效实例。
 *
 * 特点：
 *  - 无距离衰减（Attenuation.NONE）、无距离限制：任意距离/任意位置都能完整听到；
 *  - 音量取指令参数（/ccrc sound 的 <音量>，默认 1.0）；
 *  - SoundSource 用 MASTER（主音量），玩家可自行调节总响度；
 *  - 一次性播放（looping=false），音源固定为播放瞬间的玩家位置，tick 无需跟随更新。
 */
public class CommandSoundInstance extends AbstractTickableSoundInstance {

    public CommandSoundInstance(SoundEvent event, float volume) {
        super(event, SoundSource.MASTER, RandomSource.create());
        this.volume = volume;                       // 指令指定音量（默认 1.0）
        this.attenuation = SoundInstance.Attenuation.NONE;  // 无距离衰减
        this.looping = false;                       // 一次性：播完自动结束
        // 音源固定为当前玩家位置（无衰减，无需跟随移动）
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            this.x = mc.player.getX();
            this.y = mc.player.getY();
            this.z = mc.player.getZ();
        }
    }

    @Override
    public void tick() {
        // 一次性音效：无需跟随/更新，播完自动结束
    }
}