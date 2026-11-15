package com.cc_rc.network;

import com.cc_rc.block.console.ConsoleBusBlockEntity;
import com.cc_rc.block.console.ConsoleSurface;
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
 * 客户端→服务端：提交控制台组件的附带文字内容与样式。
 *
 * 附带文字的位置相对组件固定、大小固定，因此不携带坐标 / 锚点 / 缩放；
 * 空文本 = 清除附带文字。
 */
public class ConsoleCompTextPacket {

    private final BlockPos busPos;
    private final int compId;
    private final String text;
    private final int color;
    private final boolean bold;
    private final boolean italic;
    private final boolean underline;
    private final boolean strikethrough;

    public ConsoleCompTextPacket(BlockPos busPos, int compId, String text, int color,
                                 boolean bold, boolean italic, boolean underline,
                                 boolean strikethrough) {
        this.busPos = busPos;
        this.compId = compId;
        this.text = text == null ? "" : text;
        this.color = color;
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
        this.strikethrough = strikethrough;
    }

    public static void encode(ConsoleCompTextPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.busPos);
        buf.writeVarInt(msg.compId);
        buf.writeUtf(msg.text, 128);
        buf.writeInt(msg.color);
        buf.writeBoolean(msg.bold);
        buf.writeBoolean(msg.italic);
        buf.writeBoolean(msg.underline);
        buf.writeBoolean(msg.strikethrough);
    }

    public static ConsoleCompTextPacket decode(FriendlyByteBuf buf) {
        return new ConsoleCompTextPacket(buf.readBlockPos(), buf.readVarInt(),
                buf.readUtf(128), buf.readInt(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(ConsoleCompTextPacket msg, Supplier<NetworkEvent.Context> ctx) {
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
            if (bus.getOrigin() == null) {
                return;
            }
            ConsoleBusBlockEntity.CompEntry entry = bus.findComp(msg.compId);
            if (entry == null) {
                return;
            }

            // 距离校验：防止远处伪造包
            Vec3 point = ConsoleSurface.toWorld(bus.getOrigin(), bus.getSizeX(), bus.getSizeY(),
                    bus.getSizeZ(), entry.face, bus.getBusFacing(), entry.u, entry.v, 0.0D);
            if (player.distanceToSqr(point) > 64.0D) {
                return;
            }

            int status = bus.applyCompText(msg.compId, msg.text, msg.color,
                    msg.bold, msg.italic, msg.underline, msg.strikethrough);
            switch (status) {
                case ConsoleBusBlockEntity.COMP_NOT_FORMED -> hint(player, "message.cc_rc.console.need_form");
                case ConsoleBusBlockEntity.COMP_LOCKED -> hint(player, "message.cc_rc.console.comp_locked");
                default -> {
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /** 快捷提示（动作栏）。 */
    private static void hint(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
