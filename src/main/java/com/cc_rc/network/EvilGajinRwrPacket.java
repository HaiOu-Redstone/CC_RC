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
 *
 * 服务端加载安全：本包不直接引用任何客户端类——播放逻辑放在
 * com.cc_rc.client.EvilGajinRwrClientHandler，handle() 仅通过无参方法引用派发
 * （方法引用惰性解析，专用服务器加载包类时不会触碰客户端类）。
 */
public class EvilGajinRwrPacket {
    private final int entityId;
    private final boolean start;

    /** 客户端派发暂存：handle() 先把数据包暂存，再由客户端处理器取走（仅在客户端主线程读写）。 */
    private static EvilGajinRwrPacket PENDING;

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
            // 仅客户端执行：先暂存数据包，再通过无参方法引用交给客户端处理器（避免包类直接引用客户端类导致服务端崩溃）
            PENDING = msg;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> EvilGajinRwrClientHandler::dispatchPending);
        });
        ctx.get().setPacketHandled(true);
    }

    public int getEntityId() {
        return entityId;
    }

    public boolean isStart() {
        return start;
    }

    /** 客户端处理器取走暂存的包（一次性）。 */
    public static EvilGajinRwrPacket consumePending() {
        EvilGajinRwrPacket p = PENDING;
        PENDING = null;
        return p;
    }
}
