package com.cc_rc.network;

import com.cc_rc.client.EvilGajinRwrClientHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端→客户端数据包：通知客户端开始/停止播放某只邪恶盖金的追击循环音（RWR）。
 * start=true → 客户端在实体位置循环播放 evil_gajin_rwr（长音，约 3 分钟，循环直到 stop）；
 * start=false → 停止该实体的追击循环音（目标丢失/切换/实体死亡时由服务端或客户端兜底停止）。
 */
public class EvilGajinRwrPacket {
    private final int entityId;
    private final boolean start;

    public EvilGajinRwrPacket(int entityId, boolean start) {
        this.entityId = entityId;
        this.start = start;
    }

    public static void encode(EvilGajinRwrPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeBoolean(msg.start);
    }

    public static EvilGajinRwrPacket decode(FriendlyByteBuf buf) {
        return new EvilGajinRwrPacket(buf.readInt(), buf.readBoolean());
    }

    public static void handle(EvilGajinRwrPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 仅客户端执行：开始/停止该实体的追击循环音
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (msg.start) {
                    EvilGajinRwrClientHandler.startRwr(msg.entityId);
                } else {
                    EvilGajinRwrClientHandler.stopRwr(msg.entityId);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
