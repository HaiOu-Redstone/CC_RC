package com.cc_rc.network;

import com.cc_rc.block.poster.PosterBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端→服务端数据包：海报设置界面（PosterScreen）提交图片索引、缩放倍率与两个偏移量。
 *
 * 服务端校验：目标方块实体必须是 PosterBlockEntity，且玩家距目标 8 格以内；
 * 校验通过后写入方块实体（越界值由 setConfig 统一夹取），并调用 sendBlockUpdated
 * 把新配置同步给附近客户端（客户端 PosterBER 立即按新参数重绘）。
 */
public class PosterConfigPacket {

    private final BlockPos pos;
    private final int image;
    private final float scale;
    private final float dy;
    private final float dz;

    public PosterConfigPacket(BlockPos pos, int image, float scale, float dy, float dz) {
        this.pos = pos;
        this.image = image;
        this.scale = scale;
        this.dy = dy;
        this.dz = dz;
    }

    public static void encode(PosterConfigPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeVarInt(msg.image);
        buf.writeFloat(msg.scale);
        buf.writeFloat(msg.dy);
        buf.writeFloat(msg.dz);
    }

    public static PosterConfigPacket decode(FriendlyByteBuf buf) {
        return new PosterConfigPacket(buf.readBlockPos(), buf.readVarInt(),
                buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public static void handle(PosterConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            ServerLevel level = player.serverLevel();

            // 服务端权威校验：方块实体类型 + 距离
            BlockEntity be = level.getBlockEntity(msg.pos);
            if (!(be instanceof PosterBlockEntity poster)) {
                return;
            }
            double maxDistSq = 8.0 * 8.0;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5,
                    msg.pos.getZ() + 0.5) > maxDistSq) {
                return;
            }

            poster.setConfig(msg.image, msg.scale, msg.dy, msg.dz);
            // 同步给附近客户端（flags=3：更新方块实体数据 + 触发区块重渲染）
            level.sendBlockUpdated(msg.pos, poster.getBlockState(), poster.getBlockState(), 3);
        });
        ctx.get().setPacketHandled(true);
    }
}
