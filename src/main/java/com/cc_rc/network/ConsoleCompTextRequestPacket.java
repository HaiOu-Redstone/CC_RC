package com.cc_rc.network;

import com.cc_rc.block.console.ConsoleBusBlockEntity;
import com.cc_rc.block.console.ConsoleSurface;
import com.cc_rc.item.EditToolItem;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端→服务端：请求编辑某个已放置组件的「附带文字」。
 *
 * 客户端用编辑工具右键命中组件时发送；服务端以自己保存的组件数据为准，
 * 校验成型状态与玩家距离后打开普通文字编辑界面（无位置 / 缩放等选项）。
 */
public class ConsoleCompTextRequestPacket {

    private final BlockPos busPos;
    private final int compId;

    public ConsoleCompTextRequestPacket(BlockPos busPos, int compId) {
        this.busPos = busPos;
        this.compId = compId;
    }

    public static void encode(ConsoleCompTextRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.busPos);
        buf.writeVarInt(msg.compId);
    }

    public static ConsoleCompTextRequestPacket decode(FriendlyByteBuf buf) {
        return new ConsoleCompTextRequestPacket(buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(ConsoleCompTextRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
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
            if (bus.getOrigin() == null) {
                return;
            }

            // 距离校验：防止远处伪造包
            Vec3 point = ConsoleSurface.toWorld(bus.getOrigin(), bus.getSizeX(), bus.getSizeY(),
                    bus.getSizeZ(), entry.face, bus.getBusFacing(), entry.u, entry.v, 0.0D);
            if (player.distanceToSqr(point) > 64.0D) {
                return;
            }
            EditToolItem.openCompTextMenu(player, bus, entry);
        });
        ctx.get().setPacketHandled(true);
    }

    private static void hint(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
