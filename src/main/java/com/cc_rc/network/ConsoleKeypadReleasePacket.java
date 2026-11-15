package com.cc_rc.network;

import com.cc_rc.block.console.ConsoleBusBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端→服务端：按钮（弹起时间为 0）松手释放。
 *
 * 弹起时间为 0 的按钮语义是「按住一直按下、松手弹起」：按下由普通右键交互完成，
 * 服务端把 state 置为 {@link ConsoleBusBlockEntity.CompEntry#HOLD_UNTIL_RELEASE}；
 * 玩家松开右键时由客户端发本包，服务端清零 state 并回写绑定变量。
 */
public class ConsoleKeypadReleasePacket {

    private final BlockPos busPos;
    private final int compId;

    public ConsoleKeypadReleasePacket(BlockPos busPos, int compId) {
        this.busPos = busPos;
        this.compId = compId;
    }

    public static void encode(ConsoleKeypadReleasePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.busPos);
        buf.writeVarInt(msg.compId);
    }

    public static ConsoleKeypadReleasePacket decode(FriendlyByteBuf buf) {
        return new ConsoleKeypadReleasePacket(buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(ConsoleKeypadReleasePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(msg.busPos);
            if (!(be instanceof ConsoleBusBlockEntity bus)) {
                return;
            }
            ConsoleBusBlockEntity.CompEntry existing = bus.findComp(msg.compId);
            if (existing == null || bus.getOrigin() == null) {
                return;
            }
            // 距离校验：以服务端保存的组件位置为准，防止远处伪造包
            Vec3 point = com.cc_rc.block.console.ConsoleSurface.toWorld(bus.getOrigin(),
                    bus.getSizeX(), bus.getSizeY(), bus.getSizeZ(), existing.face,
                    bus.getBusFacing(), existing.u, existing.v, 0.0D);
            if (player.distanceToSqr(point) > 64.0D) {
                return;
            }
            bus.releaseKeypad(msg.compId);
        });
        ctx.get().setPacketHandled(true);
    }
}
