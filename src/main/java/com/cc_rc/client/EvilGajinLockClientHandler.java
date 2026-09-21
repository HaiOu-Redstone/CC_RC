package com.cc_rc.client;

import com.cc_rc.ModSounds;
import com.cc_rc.network.EvilGajinLockPacket;
import net.minecraft.client.Minecraft;

/**
 * 「进攻D点」语音客户端处理器（仅 Dist.CLIENT 加载）：
 * 收到 EvilGajinLockPacket 后在指定邪恶盖金实体位置播放一次性语音（无距离衰减）。
 *
 * 由 EvilGajinLockPacket.handle() 通过无参方法引用 dispatchPending() 派发调用——
 * 方法引用惰性解析，专用服务器不会加载本类，从而避免包类直接引用
 * LockCalloutSound（其父链含客户端专属类 SoundInstance）导致的服务端 NoClassDefFoundError 崩溃。
 */
public class EvilGajinLockClientHandler {

    /** 从 EvilGajinLockPacket 取包并执行（由客户端主线程调用）。 */
    public static void dispatchPending() {
        EvilGajinLockPacket msg = EvilGajinLockPacket.consumePending();
        if (msg == null || Minecraft.getInstance().level == null) {
            return;
        }
        // 在实体位置播放「进攻D点」一次（无距离衰减，任意距离清晰可闻）
        Minecraft.getInstance().getSoundManager()
                .play(new LockCalloutSound(ModSounds.EVIL_GAJIN_LOCK.get(), msg.getEntityId()));
    }
}
