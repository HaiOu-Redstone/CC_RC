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
 * 客户端→服务端：拖动控制台表面文字（只改锚点位置）。
 *
 * 多功能工具按住右键拖动文字时高频发送；服务端以自己保存的文字为准校验
 * 成型状态与距离，并把位置夹在表面范围内后写回。
 */
public class ConsoleTextMovePacket {

    private final BlockPos busPos;
    private final int textId;
    private final float u;
    private final float v;

    public ConsoleTextMovePacket(BlockPos busPos, int textId, float u, float v) {
        this.busPos = busPos;
        this.textId = textId;
        this.u = u;
        this.v = v;
    }

    public static void encode(ConsoleTextMovePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.busPos);
        buf.writeVarInt(msg.textId);
        buf.writeFloat(msg.u);
        buf.writeFloat(msg.v);
    }

    public static ConsoleTextMovePacket decode(FriendlyByteBuf buf) {
        return new ConsoleTextMovePacket(buf.readBlockPos(), buf.readVarInt(),
                buf.readFloat(), buf.readFloat());
    }

    public static void handle(ConsoleTextMovePacket msg, Supplier<NetworkEvent.Context> ctx) {
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
            ConsoleBusBlockEntity.TextEntry existing = bus.findText(msg.textId);
            if (existing == null) {
                return;
            }
            // 拒绝非有限数值：Math.max/min 对 NaN 恒返回 NaN，会让下面的距离校验
            // 因 NaN 比较恒 false 而被绕过（GUI 的 Float.parseFloat 接受 "NaN"）
            if (!Float.isFinite(msg.u) || !Float.isFinite(msg.v)) {
                return;
            }

            // 距离校验：以服务端保存的文字位置为准，防止远处伪造包
            Vec3 point = ConsoleSurface.toWorld(origin, bus.getSizeX(), bus.getSizeY(),
                    bus.getSizeZ(), existing.face, bus.getBusFacing(),
                    existing.u, existing.v, 0.0D);
            if (player.distanceToSqr(point) > 64.0D) {
                return;
            }

            int status = bus.applyTextMove(msg.textId, msg.u, msg.v);
            switch (status) {
                case ConsoleBusBlockEntity.TEXT_NOT_FORMED -> hint(player, "message.cc_rc.console.need_form");
                case ConsoleBusBlockEntity.TEXT_LOCKED -> hint(player, "message.cc_rc.console.text_locked");
                default -> {
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void hint(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
