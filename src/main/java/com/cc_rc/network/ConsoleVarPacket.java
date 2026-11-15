package com.cc_rc.network;

import com.cc_rc.block.console.ConsoleBusBlockEntity;
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
 * 客户端→服务端：控制柜变量表操作。
 *
 * 三种动作共用一条包：
 *  - {@link #ACTION_CREATE}：在指定空槽位创建变量（可绑定组件）；
 *  - {@link #ACTION_DELETE}：删除指定槽位的变量；
 *  - {@link #ACTION_SAVE_VALUES}：保存整张变量表的数值，并强制上传一次到绑定组件。
 *
 * 服务端以自己保存的总线数据为准，校验成型状态、距离与变量规则后再写入。
 */
public class ConsoleVarPacket {

    /** 创建变量。 */
    public static final int ACTION_CREATE = 0;
    /** 删除变量。 */
    public static final int ACTION_DELETE = 1;
    /** 保存变量数值。 */
    public static final int ACTION_SAVE_VALUES = 2;
    /** 保存运行间隔（0 号槽位，单位 tick）。 */
    public static final int ACTION_SAVE_PERIOD = 3;

    private final BlockPos busPos;
    private final int action;
    private final int slot;
    private final String name;
    private final int typeOrd;
    private final int compId;
    private final float[] floats;
    private final boolean[] bools;
    private final float period;

    private ConsoleVarPacket(BlockPos busPos, int action, int slot, String name,
                             int typeOrd, int compId, float[] floats, boolean[] bools,
                             float period) {
        this.busPos = busPos;
        this.action = action;
        this.slot = slot;
        this.name = name;
        this.typeOrd = typeOrd;
        this.compId = compId;
        this.floats = floats;
        this.bools = bools;
        this.period = period;
    }

    /** 创建变量（绑定设备时 name 必须与设备代号一致）。 */
    public static ConsoleVarPacket create(BlockPos busPos, int slot, String name,
                                          int typeOrd, int compId) {
        return new ConsoleVarPacket(busPos, ACTION_CREATE, slot, name, typeOrd, compId,
                null, null, 0.0F);
    }

    /** 删除变量。 */
    public static ConsoleVarPacket delete(BlockPos busPos, int slot) {
        return new ConsoleVarPacket(busPos, ACTION_DELETE, slot, null, 0, 0, null, null, 0.0F);
    }

    /** 保存整张变量表的数值（数组下标即地址，0 号槽位忽略）。 */
    public static ConsoleVarPacket saveValues(BlockPos busPos, float[] floats, boolean[] bools) {
        return new ConsoleVarPacket(busPos, ACTION_SAVE_VALUES, 0, null, 0, 0,
                floats, bools, 0.0F);
    }

    /** 保存运行间隔（0 号槽位）。 */
    public static ConsoleVarPacket savePeriod(BlockPos busPos, float period) {
        return new ConsoleVarPacket(busPos, ACTION_SAVE_PERIOD, 0, null, 0, 0,
                null, null, period);
    }

    public static void encode(ConsoleVarPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.busPos);
        buf.writeByte(msg.action);
        if (msg.action == ACTION_CREATE) {
            buf.writeVarInt(msg.slot);
            buf.writeUtf(msg.name == null ? "" : msg.name, 64);
            buf.writeVarInt(msg.typeOrd);
            buf.writeVarInt(msg.compId);
        } else if (msg.action == ACTION_DELETE) {
            buf.writeVarInt(msg.slot);
        } else if (msg.action == ACTION_SAVE_VALUES) {
            int floatCount = msg.floats == null ? 0 : msg.floats.length;
            buf.writeVarInt(floatCount);
            for (int i = 0; i < floatCount; i++) {
                buf.writeFloat(msg.floats[i]);
            }
            int boolCount = msg.bools == null ? 0 : msg.bools.length;
            buf.writeVarInt(boolCount);
            for (int i = 0; i < boolCount; i++) {
                buf.writeBoolean(msg.bools[i]);
            }
        } else if (msg.action == ACTION_SAVE_PERIOD) {
            buf.writeFloat(msg.period);
        }
    }

    public static ConsoleVarPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int action = buf.readByte();
        if (action == ACTION_CREATE) {
            return new ConsoleVarPacket(pos, action, buf.readVarInt(), buf.readUtf(64),
                    buf.readVarInt(), buf.readVarInt(), null, null, 0.0F);
        }
        if (action == ACTION_DELETE) {
            return new ConsoleVarPacket(pos, action, buf.readVarInt(), null, 0, 0,
                    null, null, 0.0F);
        }
        if (action == ACTION_SAVE_VALUES) {
            int floatCount = buf.readVarInt();
            float[] floats = new float[floatCount];
            for (int i = 0; i < floatCount; i++) {
                floats[i] = buf.readFloat();
            }
            int boolCount = buf.readVarInt();
            boolean[] bools = new boolean[boolCount];
            for (int i = 0; i < boolCount; i++) {
                bools[i] = buf.readBoolean();
            }
            return new ConsoleVarPacket(pos, action, 0, null, 0, 0, floats, bools, 0.0F);
        }
        if (action == ACTION_SAVE_PERIOD) {
            return new ConsoleVarPacket(pos, action, 0, null, 0, 0, null, null, buf.readFloat());
        }
        return new ConsoleVarPacket(pos, action, 0, null, 0, 0, null, null, 0.0F);
    }

    public static void handle(ConsoleVarPacket msg, Supplier<NetworkEvent.Context> ctx) {
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
            // 完全成型后变量表锁定（程序运行时不允许改数据）
            if (bus.getFormState() == ConsoleBusBlockEntity.State.FULL) {
                hint(player, "message.cc_rc.controller.var_locked");
                return;
            }

            if (msg.action == ACTION_CREATE) {
                int ord = Math.max(0, Math.min(
                        ConsoleBusBlockEntity.VarType.values().length - 1, msg.typeOrd));
                ConsoleBusBlockEntity.VarType type =
                        ConsoleBusBlockEntity.VarType.values()[ord];
                int result = bus.applyVarCreate(msg.slot, msg.name, type, msg.compId);
                if (result == ConsoleBusBlockEntity.VAR_OK) {
                    hint(player, "message.cc_rc.controller.var_created");
                } else {
                    hintResult(player, result);
                }
            } else if (msg.action == ACTION_DELETE) {
                int result = bus.applyVarDelete(msg.slot);
                if (result == ConsoleBusBlockEntity.VAR_OK) {
                    hint(player, "message.cc_rc.controller.var_deleted");
                } else {
                    hintResult(player, result);
                }
            } else if (msg.action == ACTION_SAVE_VALUES) {
                int result = bus.applyVarValuesSave(msg.floats, msg.bools);
                if (result == ConsoleBusBlockEntity.VAR_OK) {
                    hint(player, "message.cc_rc.controller.var_saved");
                } else {
                    hintResult(player, result);
                }
            } else if (msg.action == ACTION_SAVE_PERIOD) {
                int result = bus.applyProgramPeriod(msg.period);
                if (result == ConsoleBusBlockEntity.VAR_OK) {
                    hint(player, "message.cc_rc.controller.period_saved");
                } else {
                    hintResult(player, result);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void hint(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    /** 把变量操作的返回码翻译成提示文案。 */
    private static void hintResult(ServerPlayer player, int result) {
        String key;
        switch (result) {
            case ConsoleBusBlockEntity.VAR_BAD_SLOT -> key = "message.cc_rc.controller.var_bad_slot";
            case ConsoleBusBlockEntity.VAR_DUP_SLOT -> key = "message.cc_rc.controller.var_dup_slot";
            case ConsoleBusBlockEntity.VAR_BAD_NAME -> key = "message.cc_rc.controller.var_bad_name";
            case ConsoleBusBlockEntity.VAR_DUP_NAME -> key = "message.cc_rc.controller.var_dup_name";
            case ConsoleBusBlockEntity.VAR_COMP_NOT_FOUND -> key = "message.cc_rc.controller.var_comp_missing";
            case ConsoleBusBlockEntity.VAR_TYPE_MISMATCH -> key = "message.cc_rc.controller.var_type_mismatch";
            case ConsoleBusBlockEntity.VAR_NAME_IS_COMP -> key = "message.cc_rc.controller.var_name_used";
            case ConsoleBusBlockEntity.VAR_NOT_FOUND -> key = "message.cc_rc.controller.var_not_found";
            case ConsoleBusBlockEntity.VAR_IN_USE -> key = "message.cc_rc.controller.var_in_use";
            default -> key = "message.cc_rc.controller.var_invalid";
        }
        hint(player, key);
    }
}
