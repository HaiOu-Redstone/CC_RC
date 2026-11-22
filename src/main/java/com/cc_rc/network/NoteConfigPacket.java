package com.cc_rc.network;

import com.cc_rc.entity.NoteEntity;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端→服务端数据包：便签编辑界面（NoteScreen）提交四行文字 / 颜色 / 字体格式。
 *
 * 服务端校验：按实体 id 找到的实体必须是 NoteEntity，且玩家距其 8 格以内；
 * 校验通过后写入实体同步数据，追踪该实体的客户端会收到 EntityData 更新并重绘。
 */
public class NoteConfigPacket {

    private final int entityId;
    private final CompoundTag data;

    public NoteConfigPacket(int entityId, CompoundTag data) {
        this.entityId = entityId;
        this.data = data;
    }

    public static void encode(NoteConfigPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeNbt(msg.data);
    }

    public static NoteConfigPacket decode(FriendlyByteBuf buf) {
        return new NoteConfigPacket(buf.readVarInt(), buf.readNbt());
    }

    public static void handle(NoteConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            ServerLevel level = player.serverLevel();
            Entity entity = level.getEntity(msg.entityId);
            if (!(entity instanceof NoteEntity note)) {
                return;
            }
            if (player.distanceToSqr(note) > 8.0D * 8.0D) {
                return;
            }
            note.setTextTag(msg.data);
        });
        ctx.get().setPacketHandled(true);
    }
}
