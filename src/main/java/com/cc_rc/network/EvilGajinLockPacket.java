package com.cc_rc.network;

import com.cc_rc.client.EvilGajinLockClientHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端→客户端数据包：通知客户端在某只邪恶盖金实体位置播放「进攻D点」语音一次。
 * 与 RWR 循环音一样采用客户端实例播放（无距离衰减），解决服务端 playSound
 * 默认 16 格衰减导致玩家离实体较远时听不到的问题。
 *
 * 服务端加载安全：本包不直接引用任何客户端类（此前 handle() 内联 lambda 引用了
 * LockCalloutSound，其父链 AbstractTickableSoundInstance→SoundInstance 为客户端专属类，
 * 专用服务器加载包类时沿父链解析到 SoundInstance 导致 NoClassDefFoundError 崩溃）。
 * 播放逻辑已移入 com.cc_rc.client.EvilGajinLockClientHandler，handle() 仅通过
 * 无参方法引用派发（方法引用惰性解析，专用服务器不会加载客户端类）。
 */
public class EvilGajinLockPacket {
    private final int entityId;

    /** 客户端派发暂存：handle() 先把数据包暂存，再由客户端处理器取走（仅在客户端主线程读写）。 */
    private static EvilGajinLockPacket PENDING;

    public EvilGajinLockPacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(EvilGajinLockPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
    }

    public static EvilGajinLockPacket decode(FriendlyByteBuf buf) {
        return new EvilGajinLockPacket(buf.readInt());
    }

    public static void handle(EvilGajinLockPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 仅客户端执行：先暂存数据包，再通过无参方法引用交给客户端处理器（避免包类直接引用客户端类导致服务端崩溃）
            PENDING = msg;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> EvilGajinLockClientHandler::dispatchPending);
        });
        ctx.get().setPacketHandled(true);
    }

    public int getEntityId() {
        return entityId;
    }

    /** 客户端处理器取走暂存的包（一次性）。 */
    public static EvilGajinLockPacket consumePending() {
        EvilGajinLockPacket p = PENDING;
        PENDING = null;
        return p;
    }
}