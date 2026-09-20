package com.cc_rc.network;

import com.cc_rc.CcRc;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 简单网络通道（C2S：左键投掷包子）。
 * 客户端在攻击键事件中发送 ThrowBaoZiPacket，服务端据此生成包子弹射物。
 */
public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CcRc.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static void init() {
        CHANNEL.registerMessage(0, ThrowBaoZiPacket.class,
                ThrowBaoZiPacket::encode,
                ThrowBaoZiPacket::decode,
                ThrowBaoZiPacket::handle);
        // 破解音效开始/停止（S2C，服务端通知客户端播放/停止密码输入器破解音效）
        CHANNEL.registerMessage(1, CrackSoundPacket.class,
                CrackSoundPacket::encode,
                CrackSoundPacket::decode,
                CrackSoundPacket::handle);
        // 编辑工具提交方块显示文字（C2S，客户端把输入框文本写入目标 ITextDisplay 方块）
        CHANNEL.registerMessage(2, EditTextPacket.class,
                EditTextPacket::encode,
                EditTextPacket::decode,
                EditTextPacket::handle);
        // 邪恶盖金追击循环音开始/停止（S2C，服务端在锁定的玩家目标改变时通知客户端播放/停止循环音）
        CHANNEL.registerMessage(3, EvilGajinRwrPacket.class,
                EvilGajinRwrPacket::encode,
                EvilGajinRwrPacket::decode,
                EvilGajinRwrPacket::handle);
        // 「进攻D点」语音客户端播放（S2C，服务端在锁定玩家/周期打断时通知客户端无衰减播放一次）
        CHANNEL.registerMessage(4, EvilGajinLockPacket.class,
                EvilGajinLockPacket::encode,
                EvilGajinLockPacket::decode,
                EvilGajinLockPacket::handle);
    }
}
