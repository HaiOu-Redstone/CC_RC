package com.cc_rc.network;

import com.cc_rc.client.CrackSoundClientHandler;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端→客户端数据包：通知客户端开始/停止播放破解音效。
 * start=true → 在密码输入器位置循环播放 password_crack 音效；
 * start=false → 停止该位置的破解音效（破解中断/成功/方块破坏时发出）。
 */
public class CrackSoundPacket {
    private final BlockPos pos;
    private final boolean start;

    public CrackSoundPacket(BlockPos pos, boolean start) {
        this.pos = pos;
        this.start = start;
    }

    public static void encode(CrackSoundPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeBoolean(msg.start);
    }

    public static CrackSoundPacket decode(FriendlyByteBuf buf) {
        return new CrackSoundPacket(buf.readBlockPos(), buf.readBoolean());
    }

    public static void handle(CrackSoundPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 仅客户端执行：开始/停止破解循环音效
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (msg.start) {
                    CrackSoundClientHandler.startCrack(msg.pos);
                } else {
                    CrackSoundClientHandler.stopCrack(msg.pos);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}