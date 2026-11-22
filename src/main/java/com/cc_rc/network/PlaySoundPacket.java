package com.cc_rc.network;

import com.cc_rc.client.PlaySoundClientHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端→客户端数据包：音频播放指令 /ccrc sound 通知目标玩家在客户端播放指定声音一次。
 *
 * 特点：
 *  - 服务端仅把数据包发送给指令指定的目标玩家（PacketDistributor.PLAYER），其他玩家不受影响；
 *  - 播放由客户端音效实例完成：无距离衰减（Attenuation.NONE）、无距离限制，任意距离完整可闻；
 *  - 音量由指令参数设置（默认 1.0），受收包玩家自己的主音量（MASTER）控制总响度。
 *
 * 服务端加载安全（PR #2 模式）：本包不直接引用任何客户端类，handle() 仅通过无参方法引用
 * PlaySoundClientHandler::dispatchPending 派发；方法引用惰性解析，专用服务器不会加载客户端类。
 */
public class PlaySoundPacket {
    private final ResourceLocation soundId;
    private final float volume;

    /**
     * 客户端派发暂存队列。
     *
     * 原先是单个 PENDING 槽位：同一 tick 收到多个 /ccrc sound 包时只有最后一个能活下来，
     * 其余被静默丢弃。指令明确支持 @a（全体玩家）与连续多条指令，丢包会让部分玩家
     * 听不到声音，所以改成队列按序全部消费。
     */
    private static final java.util.Queue<PlaySoundPacket> PENDING =
            new java.util.concurrent.ConcurrentLinkedQueue<>();

    public PlaySoundPacket(ResourceLocation soundId, float volume) {
        this.soundId = soundId;
        this.volume = volume;
    }

    public static void encode(PlaySoundPacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.soundId);
        buf.writeFloat(msg.volume);
    }

    public static PlaySoundPacket decode(FriendlyByteBuf buf) {
        return new PlaySoundPacket(buf.readResourceLocation(), buf.readFloat());
    }

    public static void handle(PlaySoundPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 仅客户端执行：先入队，再通过无参方法引用交给客户端处理器
            // （避免包类直接引用客户端类导致服务端崩溃）
            PENDING.add(msg);
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> PlaySoundClientHandler::dispatchPending);
        });
        ctx.get().setPacketHandled(true);
    }

    public ResourceLocation getSoundId() {
        return soundId;
    }

    public float getVolume() {
        return volume;
    }

    /** 客户端处理器取走暂存的包（按到达顺序全部取走；无包时返回空表）。 */
    public static java.util.List<PlaySoundPacket> consumePending() {
        java.util.List<PlaySoundPacket> out = new java.util.ArrayList<>();
        PlaySoundPacket p;
        while ((p = PENDING.poll()) != null) {
            out.add(p);
        }
        return out;
    }
}