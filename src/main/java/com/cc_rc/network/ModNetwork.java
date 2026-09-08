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
    }
}
