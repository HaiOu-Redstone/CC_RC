package com.cc_rc.network;

import com.cc_rc.block.graffiti.GraffitiBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端→服务端数据包：涂鸦文字编辑界面（GraffitiScreen）提交
 * 文字内容、颜色、大小、水平/垂直偏移与字体格式。
 *
 * 服务端校验：目标方块实体必须是 GraffitiBlockEntity，且玩家距目标 8 格以内；
 * 校验通过后写入方块实体（越界值由 setConfig 统一夹取），并调用 sendBlockUpdated
 * 把新配置同步给附近客户端（客户端 GraffitiBER 立即按新参数重绘）。
 */
public class GraffitiConfigPacket {

    private final BlockPos pos;
    private final String text;
    private final int color;
    private final float scale;
    private final float dx;
    private final float dy;
    private final int flags;

    public GraffitiConfigPacket(BlockPos pos, String text, int color, float scale,
                                float dx, float dy, int flags) {
        this.pos = pos;
        this.text = text;
        this.color = color;
        this.scale = scale;
        this.dx = dx;
        this.dy = dy;
        this.flags = flags;
    }

    public static void encode(GraffitiConfigPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.text, GraffitiBlockEntity.MAX_TEXT);
        buf.writeVarInt(msg.color);
        buf.writeFloat(msg.scale);
        buf.writeFloat(msg.dx);
        buf.writeFloat(msg.dy);
        buf.writeByte(msg.flags);
    }

    public static GraffitiConfigPacket decode(FriendlyByteBuf buf) {
        return new GraffitiConfigPacket(buf.readBlockPos(),
                buf.readUtf(GraffitiBlockEntity.MAX_TEXT), buf.readVarInt(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readUnsignedByte());
    }

    public static void handle(GraffitiConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            ServerLevel level = player.serverLevel();

            // 服务端权威校验：方块实体类型 + 距离
            BlockEntity be = level.getBlockEntity(msg.pos);
            if (!(be instanceof GraffitiBlockEntity graffiti)) {
                return;
            }
            double maxDistSq = 8.0 * 8.0;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5,
                    msg.pos.getZ() + 0.5) > maxDistSq) {
                return;
            }

            graffiti.setConfig(msg.text, msg.color, msg.scale, msg.dx, msg.dy,
                    (msg.flags & 1) != 0, (msg.flags & 2) != 0,
                    (msg.flags & 4) != 0, (msg.flags & 8) != 0);
            // 同步给附近客户端（flags=3：更新方块实体数据 + 触发区块重渲染）
            level.sendBlockUpdated(msg.pos, graffiti.getBlockState(), graffiti.getBlockState(), 3);
        });
        ctx.get().setPacketHandled(true);
    }
}
