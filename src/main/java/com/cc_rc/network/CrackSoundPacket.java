package com.cc_rc.network;

import com.cc_rc.client.CrackSoundClientHandler;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端→客户端数据包：通知客户端开始/停止播放破解音效。
 * start=true → 在密码输入器位置循环播放 password_crack 音效；
 * start=false → 停止该位置的破解音效（破解中断/成功/方块破坏时发出）。
 *
 * 服务端加载安全：本包不直接引用任何客户端类——播放逻辑放在
 * com.cc_rc.client.CrackSoundClientHandler，handle() 仅通过无参方法引用派发
 * （方法引用惰性解析，专用服务器加载包类时不会触碰客户端类）。
 */
public class CrackSoundPacket {
    private final BlockPos pos;
    private final boolean start;

    /** 客户端派发暂存：handle() 先把数据包暂存，再由客户端处理器取走（仅在客户端主线程读写）。 */
    private static CrackSoundPacket PENDING;

    public CrackSoundPacket(BlockPos pos, boolean start) {
        this.pos = pos;
        this.start = start;
    }

    public static void encode(CrackSoundPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeBoolean(msg.start);
    }

    public static CrackSoundPacket decode(FriendlyByteBuf buf) {
        return new CrackSoundPacket(buf.readBlockPos(), buf.readBoolean());
    }

    public static void handle(CrackSoundPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 仅客户端执行：先暂存数据包，再通过无参方法引用交给客户端处理器（避免包类直接引用客户端类导致服务端崩溃）
            PENDING = msg;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> CrackSoundClientHandler::dispatchPending);
        });
        ctx.get().setPacketHandled(true);
    }

    public BlockPos getPos() {
        return pos;
    }

    public boolean isStart() {
        return start;
    }

    /** 客户端处理器取走暂存的包（一次性）。 */
    public static CrackSoundPacket consumePending() {
        CrackSoundPacket p = PENDING;
        PENDING = null;
        return p;
    }
}