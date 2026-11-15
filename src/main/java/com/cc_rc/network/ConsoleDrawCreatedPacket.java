package com.cc_rc.network;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端→客户端：新建图案的回执。
 *
 * 属性界面在玩家第一次选择类型 / 贴图时发送创建请求，服务端创建成功后用本包
 * 回传新图形的 id（失败时为 -1），界面据此从「未创建」切换到编辑状态。
 * 由 {@code ConsoleDrawScreen#tick} 取走，避免网络包直接引用客户端界面类。
 */
public class ConsoleDrawCreatedPacket {

    private final BlockPos pos;
    private final int shapeId;

    /** 客户端暂存（只在客户端主线程读写）。 */
    private static ConsoleDrawCreatedPacket PENDING;

    public ConsoleDrawCreatedPacket(BlockPos pos, int shapeId) {
        this.pos = pos;
        this.shapeId = shapeId;
    }

    public static void encode(ConsoleDrawCreatedPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeVarInt(msg.shapeId);
    }

    public static ConsoleDrawCreatedPacket decode(FriendlyByteBuf buf) {
        // 失败回执写的是 -1，VarInt 对负数按无符号编码，这里读回 int 仍为 -1
        return new ConsoleDrawCreatedPacket(buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(ConsoleDrawCreatedPacket msg,
                              Supplier<NetworkEvent.Context> ctx) {
        // enqueueWork 保证在主线程执行，界面可以安全读取
        ctx.get().enqueueWork(() -> PENDING = msg);
        ctx.get().setPacketHandled(true);
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getShapeId() {
        return shapeId;
    }

    /** 客户端处理器取走暂存的回执（一次性）。 */
    public static ConsoleDrawCreatedPacket consumePending() {
        ConsoleDrawCreatedPacket p = PENDING;
        PENDING = null;
        return p;
    }
}
