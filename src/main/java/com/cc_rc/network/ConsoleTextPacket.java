package com.cc_rc.network;

import com.cc_rc.block.console.ConsoleBusBlockEntity;
import com.cc_rc.block.console.ConsoleSurface;
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
 * 客户端→服务端：提交控制台结构表面文字的内容、样式、锚点位置与缩放。
 *
 * textId >= 0 → 修改该条已有文字（内容为空表示删除）；
 * textId = -1 → 在 face/u/v 处新建一条文字。
 *
 * 服务端会重新校验成型状态、玩家距离，并把锚点夹在表面矩形内后写入总线方块实体。
 */
public class ConsoleTextPacket {

    private final BlockPos busPos;
    private final int textId;
    private final int faceOrd;
    private final float u;
    private final float v;
    private final String text;
    private final int color;
    private final boolean bold;
    private final boolean italic;
    private final boolean underline;
    private final boolean strikethrough;
    private final int anchorH;
    private final int anchorV;
    private final float scale;

    public ConsoleTextPacket(BlockPos busPos, int textId, int faceOrd, float u, float v, String text,
                             int color, boolean bold, boolean italic, boolean underline,
                             boolean strikethrough, int anchorH, int anchorV, float scale) {
        this.busPos = busPos;
        this.textId = textId;
        this.faceOrd = faceOrd;
        this.u = u;
        this.v = v;
        this.text = text == null ? "" : text;
        this.color = color;
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
        this.strikethrough = strikethrough;
        this.anchorH = anchorH;
        this.anchorV = anchorV;
        this.scale = scale;
    }

    public static void encode(ConsoleTextPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.busPos);
        buf.writeVarInt(msg.textId);
        buf.writeVarInt(msg.faceOrd);
        buf.writeFloat(msg.u);
        buf.writeFloat(msg.v);
        buf.writeUtf(msg.text, 128);
        buf.writeInt(msg.color);
        buf.writeBoolean(msg.bold);
        buf.writeBoolean(msg.italic);
        buf.writeBoolean(msg.underline);
        buf.writeBoolean(msg.strikethrough);
        buf.writeVarInt(msg.anchorH);
        buf.writeVarInt(msg.anchorV);
        buf.writeFloat(msg.scale);
    }

    public static ConsoleTextPacket decode(FriendlyByteBuf buf) {
        return new ConsoleTextPacket(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(),
                buf.readFloat(), buf.readFloat(), buf.readUtf(128), buf.readInt(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                buf.readVarInt(), buf.readVarInt(), buf.readFloat());
    }

    public static void handle(ConsoleTextPacket msg, Supplier<NetworkEvent.Context> ctx) {
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
            BlockPos origin = bus.getOrigin();
            if (origin == null) {
                return;
            }

            Direction[] faces = Direction.values();
            Direction face = faces[Math.max(0, Math.min(faces.length - 1, msg.faceOrd))];
            float u = msg.u;
            float v = msg.v;

            // 修改已有文字时以服务端保存的位置/面为准
            ConsoleBusBlockEntity.TextEntry existing = bus.findText(msg.textId);
            if (existing != null) {
                face = existing.face;
                u = existing.u;
                v = existing.v;
            }

            // 锚点夹在表面范围内（与 applyTextEdit 一致，便于距离校验）
            Direction busFacing = bus.getBusFacing();
            double halfW = ConsoleSurface.halfWidthPx(face, busFacing,
                    bus.getSizeX(), bus.getSizeY(), bus.getSizeZ());
            double halfH = ConsoleSurface.halfHeightPx(face, busFacing,
                    bus.getSizeX(), bus.getSizeY(), bus.getSizeZ());
            float cu = (float) Math.max(-halfW, Math.min(halfW, u));
            float cv = (float) Math.max(-halfH, Math.min(halfH, v));

            // 距离校验：防止远处伪造包
            Vec3 point = ConsoleSurface.toWorld(origin, bus.getSizeX(), bus.getSizeY(), bus.getSizeZ(),
                    face, busFacing, cu, cv, 0.0D);
            if (player.distanceToSqr(point) > 64.0D) {
                return;
            }

            int status = bus.applyTextEdit(msg.textId, face, cu, cv, msg.text, msg.color,
                    msg.bold, msg.italic, msg.underline, msg.strikethrough,
                    msg.anchorH, msg.anchorV, msg.scale);
            switch (status) {
                case ConsoleBusBlockEntity.TEXT_NOT_FORMED -> hint(player, "message.cc_rc.console.need_form");
                case ConsoleBusBlockEntity.TEXT_LOCKED -> hint(player, "message.cc_rc.console.text_locked");
                case ConsoleBusBlockEntity.TEXT_OUTSIDE -> hint(player, "message.cc_rc.console.text_outside");
                case ConsoleBusBlockEntity.TEXT_LIMIT -> hint(player, "message.cc_rc.console.text_limit");
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
