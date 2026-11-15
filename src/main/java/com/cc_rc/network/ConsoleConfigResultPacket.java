package com.cc_rc.network;

import com.cc_rc.client.ConsoleConfigClientHandler;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端→客户端：控制柜配置文件加载结果回执。
 *
 * 结果码与 {@link com.cc_rc.block.console.ConsoleBusBlockEntity#applyConfigJson} 的返回值一致。
 * 服务端加载安全：本包不直接引用客户端类，界面派发放在
 * com.cc_rc.client.ConsoleConfigClientHandler。
 */
public class ConsoleConfigResultPacket {

    private final BlockPos pos;
    private final int result;

    /** 客户端派发暂存（仅在客户端主线程读写）。 */
    private static ConsoleConfigResultPacket PENDING;

    public ConsoleConfigResultPacket(BlockPos pos, int result) {
        this.pos = pos;
        this.result = result;
    }

    public static void encode(ConsoleConfigResultPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeVarInt(msg.result);
    }

    public static ConsoleConfigResultPacket decode(FriendlyByteBuf buf) {
        return new ConsoleConfigResultPacket(buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(ConsoleConfigResultPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            PENDING = msg;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> ConsoleConfigClientHandler::dispatchPending);
        });
        ctx.get().setPacketHandled(true);
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getResult() {
        return result;
    }

    /** 客户端处理器取走暂存的包（一次性）。 */
    public static ConsoleConfigResultPacket consumePending() {
        ConsoleConfigResultPacket p = PENDING;
        PENDING = null;
        return p;
    }
}
