package com.cc_rc.client;

import com.cc_rc.network.PlaySoundPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 音频播放指令 /ccrc sound 的客户端处理器（仅 Dist.CLIENT 加载）：
 * 收到 PlaySoundPacket 后，在客户端播放指定的声音（无距离衰减、音量按指令参数）。
 *
 * 由 PlaySoundPacket.handle() 通过无参方法引用 dispatchPending() 派发调用——
 * 方法引用惰性解析，专用服务器不会加载本类。
 */
public class PlaySoundClientHandler {

    /** 从 PlaySoundPacket 取包并执行播放（由客户端主线程调用）。 */
    public static void dispatchPending() {
        PlaySoundPacket msg = PlaySoundPacket.consumePending();
        if (msg == null || Minecraft.getInstance().level == null) {
            return;
        }
        // 声音注册表查找：服务端已校验过存在性，这里再兜底一次
        SoundEvent event = ForgeRegistries.SOUND_EVENTS.getValue(msg.getSoundId());
        if (event == null) {
            return;
        }
        // 无距离衰减 + 指令音量播放（主音量 MASTER）
        Minecraft.getInstance().getSoundManager()
                .play(new CommandSoundInstance(event, msg.getVolume()));
    }
}