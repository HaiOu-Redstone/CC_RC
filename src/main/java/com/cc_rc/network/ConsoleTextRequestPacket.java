package com.cc_rc.network;

import com.cc_rc.block.console.ConsoleBusBlockEntity;
import com.cc_rc.block.console.ConsoleSurface;
import com.cc_rc.item.EditToolItem;
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
 * 客户端→服务端：请求编辑控制台结构表面的文字。
 *
 * 客户端用编辑工具右键控制台表面时先做命中检测（需要字体宽度，只有客户端有 Font），
 * 判断点击点是否落在某条已有文字覆盖的区域内：
 *  - 命中 → textId 为那条文字的 id，服务端打开界面时带出原文；
 *  - 未命中 → textId = -1，服务端按 face/u/v 新建一条文字。
 *
 * 服务端负责权威校验（成型状态、点击点是否在表面内、距离），
 * 通过后打开复用的文字编辑界面（EditTextMenu，target = 控制台表面）。
 */
public class ConsoleTextRequestPacket {

    private final BlockPos busPos;
    private final int textId;
    private final int faceOrd;
    private final float u;
    private final float v;

    public ConsoleTextRequestPacket(BlockPos busPos, int textId, int faceOrd, float u, float v) {
        this.busPos = busPos;
        this.textId = textId;
        this.faceOrd = faceOrd;
        this.u = u;
        this.v = v;
    }

    public static void encode(ConsoleTextRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.busPos);
        buf.writeVarInt(msg.textId);
        buf.writeVarInt(msg.faceOrd);
        buf.writeFloat(msg.u);
        buf.writeFloat(msg.v);
    }

    public static ConsoleTextRequestPacket decode(FriendlyByteBuf buf) {
        return new ConsoleTextRequestPacket(buf.readBlockPos(), buf.readVarInt(),
                buf.readVarInt(), buf.readFloat(), buf.readFloat());
    }

    public static void handle(ConsoleTextRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
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

            // 只有「部分成型」状态可以编辑表面文字
            ConsoleBusBlockEntity.State state = bus.getFormState();
            if (state == ConsoleBusBlockEntity.State.UNFORMED) {
                hint(player, "message.cc_rc.console.need_form");
                return;
            }
            if (state == ConsoleBusBlockEntity.State.FULL) {
                hint(player, "message.cc_rc.console.text_locked");
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

            // 命中已有文字时以服务端保存的位置/内容为准（客户端数据可能过期）
            ConsoleBusBlockEntity.TextEntry existing = bus.findText(msg.textId);
            int textId = -1;
            if (existing != null) {
                textId = existing.id;
                face = existing.face;
                u = existing.u;
                v = existing.v;
            }

            Direction busFacing = bus.getBusFacing();
            if (!ConsoleSurface.contains(face, busFacing, bus.getSizeX(), bus.getSizeY(), bus.getSizeZ(), u, v)) {
                hint(player, "message.cc_rc.console.text_outside");
                return;
            }

            // 距离校验：以点击点在结构表面上的实际世界坐标为准
            Vec3 point = ConsoleSurface.toWorld(origin, bus.getSizeX(), bus.getSizeY(), bus.getSizeZ(),
                    face, busFacing, u, v, 0.0D);
            if (player.distanceToSqr(point) > 64.0D) {
                return;
            }

            // 打开控制台表面文字专用编辑界面（带出原样式/锚点/缩放）
            EditToolItem.openConsoleTextMenu(player, bus, existing, textId, face, u, v);
        });
        ctx.get().setPacketHandled(true);
    }

    /** 快捷提示（动作栏）。 */
    private static void hint(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
