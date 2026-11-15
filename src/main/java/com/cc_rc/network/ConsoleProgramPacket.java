package com.cc_rc.network;

import com.cc_rc.block.console.ConsoleBusBlockEntity;
import com.cc_rc.block.console.ProgramIR;
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
 * 客户端→服务端：控制柜程序表操作。
 *
 * 目前只有「保存整张程序表」一种动作：界面每次增删 / 修改一行都会发送完整快照，
 * 服务端以自己保存的变量表为准做全量校验，任何一行出错都拒绝保存并给出原因。
 */
public class ConsoleProgramPacket {

    /** 保存整张程序表（数组下标即行号，null 表示空行）。 */
    public static final int ACTION_SAVE_LINES = 0;

    private final BlockPos busPos;
    private final int action;
    private final ProgramIR.Line[] lines;

    private ConsoleProgramPacket(BlockPos busPos, int action, ProgramIR.Line[] lines) {
        this.busPos = busPos;
        this.action = action;
        this.lines = lines;
    }

    /** 保存整张程序表。 */
    public static ConsoleProgramPacket saveLines(BlockPos busPos, ProgramIR.Line[] lines) {
        return new ConsoleProgramPacket(busPos, ACTION_SAVE_LINES, lines);
    }

    public static void encode(ConsoleProgramPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.busPos);
        buf.writeByte(msg.action);
        if (msg.action == ACTION_SAVE_LINES) {
            int count = msg.lines == null ? 0 : msg.lines.length;
            buf.writeVarInt(count);
            for (int i = 0; i < count; i++) {
                ProgramIR.Line line = msg.lines[i];
                buf.writeBoolean(line != null);
                if (line != null) {
                    line.write(buf);
                }
            }
        }
    }

    public static ConsoleProgramPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int action = buf.readByte();
        if (action == ACTION_SAVE_LINES) {
            int count = buf.readVarInt();
            // 防御异常包：负行数直接视为 0，否则数组长度会变成负数
            if (count < 0) {
                count = 0;
            }
            // 防御异常包：行数超过上限直接丢弃多余部分
            ProgramIR.Line[] lines =
                    new ProgramIR.Line[Math.min(count, ConsoleBusBlockEntity.MAX_PROGRAM_LINES)];
            for (int i = 0; i < count; i++) {
                boolean present = buf.readBoolean();
                ProgramIR.Line line = present ? ProgramIR.Line.read(buf) : null;
                if (i < lines.length) {
                    lines[i] = line;
                }
            }
            return new ConsoleProgramPacket(pos, action, lines);
        }
        return new ConsoleProgramPacket(pos, action, null);
    }

    public static void handle(ConsoleProgramPacket msg, Supplier<NetworkEvent.Context> ctx) {
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
            // 距离校验：防止远程操作别人的控制柜
            if (player.distanceToSqr(Vec3.atCenterOf(msg.busPos)) > 64.0D) {
                return;
            }
            if (bus.getFormState() == ConsoleBusBlockEntity.State.FULL) {
                hint(player, "message.cc_rc.controller.prog_locked");
                return;
            }
            if (msg.action == ACTION_SAVE_LINES) {
                int result = bus.applyProgramSave(msg.lines);
                hintResult(player, result);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void hint(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    /** 把程序保存的返回码翻译成提示文案。 */
    private static void hintResult(ServerPlayer player, int result) {
        String key;
        switch (result) {
            case ConsoleBusBlockEntity.VAR_OK -> key = "message.cc_rc.controller.prog_saved";
            case ConsoleBusBlockEntity.PROG_LIMIT -> key = "message.cc_rc.controller.prog_limit";
            case ConsoleBusBlockEntity.PROG_BAD_TARGET ->
                    key = "message.cc_rc.controller.prog_bad_target";
            case ConsoleBusBlockEntity.PROG_BAD_COND -> key = "message.cc_rc.controller.prog_bad_cond";
            case ConsoleBusBlockEntity.PROG_BAD_EXPR -> key = "message.cc_rc.controller.prog_bad_expr";
            case ConsoleBusBlockEntity.PROG_TYPE_MISMATCH ->
                    key = "message.cc_rc.controller.prog_type";
            case ConsoleBusBlockEntity.PROG_TOO_DEEP -> key = "message.cc_rc.controller.prog_deep";
            default -> key = "message.cc_rc.controller.prog_invalid";
        }
        hint(player, key);
    }
}
