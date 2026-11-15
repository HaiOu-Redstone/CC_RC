package com.cc_rc.network;

import com.cc_rc.block.console.ConsoleBusBlockEntity;
import com.cc_rc.block.console.ConsoleSurface;
import com.cc_rc.item.ConsoleCompItem;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端→服务端：请求打开某个已放置组件的编辑界面。
 *
 * 客户端用编辑工具右键命中组件时发送；服务端以自己保存的组件数据为准，
 * 校验成型状态与距离后打开组件设置界面（代号 / 位置 / 删除）。
 */
public class ConsoleCompRequestPacket {

    private final BlockPos busPos;
    private final int compId;
    private final int faceOrd;
    private final float u;
    private final float v;

    public ConsoleCompRequestPacket(BlockPos busPos, int compId, int faceOrd, float u, float v) {
        this.busPos = busPos;
        this.compId = compId;
        this.faceOrd = faceOrd;
        this.u = u;
        this.v = v;
    }

    public static void encode(ConsoleCompRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.busPos);
        buf.writeVarInt(msg.compId);
        buf.writeVarInt(msg.faceOrd);
        buf.writeFloat(msg.u);
        buf.writeFloat(msg.v);
    }

    public static ConsoleCompRequestPacket decode(FriendlyByteBuf buf) {
        return new ConsoleCompRequestPacket(buf.readBlockPos(), buf.readVarInt(),
                buf.readVarInt(), buf.readFloat(), buf.readFloat());
    }

    public static void handle(ConsoleCompRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
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
            ConsoleBusBlockEntity.State state = bus.getFormState();
            if (state == ConsoleBusBlockEntity.State.UNFORMED) {
                hint(player, "message.cc_rc.console.need_form");
                return;
            }
            if (state == ConsoleBusBlockEntity.State.FULL) {
                hint(player, "message.cc_rc.console.comp_locked");
                return;
            }

            // 以服务端保存的组件为准（客户端数据可能过期）
            ConsoleBusBlockEntity.CompEntry entry = bus.findComp(msg.compId);
            if (entry == null) {
                return;
            }

            Direction busFacing = bus.getBusFacing();
            Vec3 point = ConsoleSurface.toWorld(bus.getOrigin(), bus.getSizeX(), bus.getSizeY(),
                    bus.getSizeZ(), entry.face, busFacing, entry.u, entry.v, 0.0D);
            if (player.distanceToSqr(point) > 64.0D) {
                return;
            }
            ConsoleCompItem.openCompMenu(player, bus, entry);
        });
        ctx.get().setPacketHandled(true);
    }

    private static void hint(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
