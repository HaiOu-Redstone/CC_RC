package com.cc_rc.network;

import com.cc_rc.ModSounds;
import com.cc_rc.client.LockCalloutSound;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端→客户端数据包：通知客户端在某只邪恶盖金实体位置播放「进攻D点」语音一次。
 * 与 RWR 循环音一样采用客户端实例播放（无距离衰减），解决服务端 playSound
 * 默认 16 格衰减导致玩家离实体较远时听不到的问题。
 */
public class EvilGajinLockPacket {
    private final int entityId;

    public EvilGajinLockPacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(EvilGajinLockPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
    }

    public static EvilGajinLockPacket decode(FriendlyByteBuf buf) {
        return new EvilGajinLockPacket(buf.readInt());
    }

    public static void handle(EvilGajinLockPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 仅客户端执行：在实体位置播放「进攻D点」一次（无衰减）
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (Minecraft.getInstance().level != null) {
                    Minecraft.getInstance().getSoundManager()
                            .play(new LockCalloutSound(ModSounds.EVIL_GAJIN_LOCK.get(), msg.entityId));
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}