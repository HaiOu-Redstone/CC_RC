package com.cc_rc.network;

import com.cc_rc.block.audio_player.AudioPlayerBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 客户端→服务端数据包：音频播放器界面（AudioPlayerScreen）提交全部配置。
 *
 * 服务端校验：目标方块实体必须是 AudioPlayerBlockEntity、玩家距目标 8 格以内、
 * 音效 id 合法且已注册；通过后写入方块实体（越界值由 setConfig 统一夹取），
 * 并调用 sendBlockUpdated 把新配置同步给附近客户端（客户端调度器随即使生效）。
 */
public class AudioPlayerConfigPacket {

    private final BlockPos pos;
    private final String soundId;
    private final int mode;
    private final float range;
    private final int attenuation;
    private final float start;
    private final float stop;
    private final float interval;
    private final int volume;

    public AudioPlayerConfigPacket(BlockPos pos, String soundId, int mode, float range,
                                   int attenuation, float start, float stop, float interval,
                                   int volume) {
        this.pos = pos;
        this.soundId = soundId;
        this.mode = mode;
        this.range = range;
        this.attenuation = attenuation;
        this.start = start;
        this.stop = stop;
        this.interval = interval;
        this.volume = volume;
    }

    public static void encode(AudioPlayerConfigPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.soundId, AudioPlayerBlockEntity.MAX_SOUND_ID_LENGTH);
        buf.writeVarInt(msg.mode);
        buf.writeFloat(msg.range);
        buf.writeVarInt(msg.attenuation);
        buf.writeFloat(msg.start);
        buf.writeFloat(msg.stop);
        buf.writeFloat(msg.interval);
        buf.writeVarInt(msg.volume);
    }

    public static AudioPlayerConfigPacket decode(FriendlyByteBuf buf) {
        return new AudioPlayerConfigPacket(buf.readBlockPos(),
                buf.readUtf(AudioPlayerBlockEntity.MAX_SOUND_ID_LENGTH),
                buf.readVarInt(), buf.readFloat(), buf.readVarInt(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt());
    }

    public static void handle(AudioPlayerConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            ServerLevel level = player.serverLevel();

            // 服务端权威校验：方块实体类型 + 距离
            BlockEntity be = level.getBlockEntity(msg.pos);
            if (!(be instanceof AudioPlayerBlockEntity audio)) {
                return;
            }
            double maxDistSq = 8.0 * 8.0;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5,
                    msg.pos.getZ() + 0.5) > maxDistSq) {
                return;
            }

            // 音效 id 规范化 + 注册表校验（只允许播放客户端也存在的已注册音效）
            String soundId = AudioPlayerBlockEntity.normalizeSoundId(msg.soundId);
            if (!soundId.isEmpty()) {
                ResourceLocation id = ResourceLocation.tryParse(soundId);
                if (id == null || !ForgeRegistries.SOUND_EVENTS.containsKey(id)) {
                    return;
                }
            }

            audio.setConfig(soundId, msg.mode, msg.range, msg.attenuation,
                    msg.start, msg.stop, msg.interval, msg.volume);
            // 同步给附近客户端（flags=3：更新方块实体数据 + 触发区块重渲染）
            level.sendBlockUpdated(msg.pos, audio.getBlockState(), audio.getBlockState(), 3);
        });
        ctx.get().setPacketHandled(true);
    }
}
