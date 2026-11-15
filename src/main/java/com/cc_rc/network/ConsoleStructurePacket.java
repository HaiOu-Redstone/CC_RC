package com.cc_rc.network;

import com.cc_rc.block.console.ConsoleBusBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端→服务端数据包：控制台总线界面（ConsoleScreen）的成型 / 锁定 / 解锁操作。
 *
 * 服务端校验：目标方块实体必须是 ConsoleBusBlockEntity，且玩家距目标 8 格以内；
 * 校验通过后按 action 调用对应方法（成型/锁定内部还会再校验当前状态）。
 */
public class ConsoleStructurePacket {

    /** 检测并部分成型。 */
    public static final int ACTION_FORM = 0;
    /** 部分成型 → 完全成型（锁定）。 */
    public static final int ACTION_LOCK = 1;
    /** 完全成型 → 部分成型（解锁）。 */
    public static final int ACTION_UNLOCK = 2;

    private final BlockPos pos;
    private final int action;

    public ConsoleStructurePacket(BlockPos pos, int action) {
        this.pos = pos;
        this.action = action;
    }

    public static void encode(ConsoleStructurePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeVarInt(msg.action);
    }

    public static ConsoleStructurePacket decode(FriendlyByteBuf buf) {
        return new ConsoleStructurePacket(buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(ConsoleStructurePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            ServerLevel level = player.serverLevel();

            // 服务端权威校验：方块实体类型 + 距离
            BlockEntity be = level.getBlockEntity(msg.pos);
            if (!(be instanceof ConsoleBusBlockEntity bus)) {
                return;
            }
            double maxDistSq = 8.0 * 8.0;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5,
                    msg.pos.getZ() + 0.5) > maxDistSq) {
                return;
            }

            switch (msg.action) {
                case ACTION_FORM -> bus.formStructure();
                case ACTION_LOCK -> bus.lock();
                case ACTION_UNLOCK -> bus.unlock();
                default -> {
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
