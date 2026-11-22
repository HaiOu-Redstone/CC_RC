package com.cc_rc.network;

import com.cc_rc.block.graffiti.GraffitiShapeBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端→服务端数据包：涂鸦形状设置界面（GraffitiShapeScreen）提交
 * 类型 / 预设 / 颜色 / 大小 / 线宽 / 旋转 / 偏移。
 *
 * 服务端校验：目标方块实体必须是 GraffitiShapeBlockEntity，且玩家距目标 8 格以内；
 * 校验通过后写入方块实体（越界值由 setConfig 统一夹取），并调用 sendBlockUpdated
 * 把新配置同步给附近客户端（客户端 GraffitiShapeBER 立即按新参数重绘）。
 */
public class GraffitiShapeConfigPacket {

    private final BlockPos pos;
    private final int type;
    private final String preset;
    private final int color;
    private final float size;
    private final float width;
    private final float rotation;
    private final float dx;
    private final float dy;

    public GraffitiShapeConfigPacket(BlockPos pos, int type, String preset, int color,
                                     float size, float width, float rotation,
                                     float dx, float dy) {
        this.pos = pos;
        this.type = type;
        this.preset = preset;
        this.color = color;
        this.size = size;
        this.width = width;
        this.rotation = rotation;
        this.dx = dx;
        this.dy = dy;
    }

    public static void encode(GraffitiShapeConfigPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeVarInt(msg.type);
        buf.writeUtf(msg.preset, GraffitiShapeBlockEntity.MAX_PRESET_LENGTH);
        buf.writeVarInt(msg.color);
        buf.writeFloat(msg.size);
        buf.writeFloat(msg.width);
        buf.writeFloat(msg.rotation);
        buf.writeFloat(msg.dx);
        buf.writeFloat(msg.dy);
    }

    public static GraffitiShapeConfigPacket decode(FriendlyByteBuf buf) {
        return new GraffitiShapeConfigPacket(buf.readBlockPos(), buf.readVarInt(),
                buf.readUtf(GraffitiShapeBlockEntity.MAX_PRESET_LENGTH), buf.readVarInt(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readFloat());
    }

    public static void handle(GraffitiShapeConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            ServerLevel level = player.serverLevel();

            // 服务端权威校验：方块实体类型 + 距离
            BlockEntity be = level.getBlockEntity(msg.pos);
            if (!(be instanceof GraffitiShapeBlockEntity graffiti)) {
                return;
            }
            double maxDistSq = 8.0 * 8.0;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5,
                    msg.pos.getZ() + 0.5) > maxDistSq) {
                return;
            }

            graffiti.setConfig(msg.type, msg.preset, msg.color, msg.size, msg.width,
                    msg.rotation, msg.dx, msg.dy);
            // 同步给附近客户端（flags=3：更新方块实体数据 + 触发区块重渲染）
            level.sendBlockUpdated(msg.pos, graffiti.getBlockState(), graffiti.getBlockState(), 3);
        });
        ctx.get().setPacketHandled(true);
    }
}
