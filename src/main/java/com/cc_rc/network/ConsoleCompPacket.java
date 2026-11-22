package com.cc_rc.network;

import com.cc_rc.block.console.ConsoleBusBlockEntity;
import com.cc_rc.block.console.ConsoleSurface;
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
 * 客户端→服务端：提交组件编辑（代号 / 位置 / 颜色 / 显示设置）或删除组件。
 *
 * delete = true 时忽略其余字段，直接删除 compId 对应的组件；
 * 否则写回全部可编辑字段（服务端会再次校验代号格式、重复与位置重叠）。
 */
public class ConsoleCompPacket {

    private final BlockPos busPos;
    private final int compId;
    private final String code;
    /** 第二名称（仅圆盘记录仪的推送触发名称；空 = 不设）。 */
    private final String code2;
    private final float u;
    private final float v;
    private final int color;
    /** 数字显示屏保留小数位（其它类型忽略）。 */
    private final int decimals;
    /** 数字显示屏单位（其它类型忽略）。 */
    private final String unit;
    /** 按钮弹起时间（tick，0 = 松手立刻弹起；其它类型忽略）。 */
    private final int holdTicks;
    /** 组件缩放（1.0 = 模型默认大小）。 */
    private final float scale;
    /** RGB 颜色控制器：连接的组件代号（用「;」分隔；其它类型忽略）。 */
    private final String links;
    /** RGB 颜色控制器：R / G / B 三个通道变量名（空 = 不启用；其它类型忽略）。 */
    private final String rgbR;
    private final String rgbG;
    private final String rgbB;
    /** RGB 颜色控制器：是否显示原色（= 停用；其它类型忽略）。 */
    private final boolean showOriginal;
    /** 同步指示器：转速变量名（空 = 自动使用 代号 + "_s"；其它类型忽略）。 */
    private final String speedVar;
    /** 同步指示器：灯亮状态变量名（空 = 自动使用 代号 + "_b"；其它类型忽略）。 */
    private final String lampVar;
    /** 警报器：静音状态变量名（空 = 自动使用 代号 + "_Q"；其它类型忽略）。 */
    private final String muteVar;
    /** 安全按钮：盖子自动关闭时间（tick，0 = 不自动关闭；其它类型忽略）。 */
    private final int coverTicks;
    /** 钥匙开关：钥匙模式（0 = 开闭，1 = 常开，2 = 常闭；其它类型忽略）。 */
    private final int keyMode;
    private final boolean delete;

    public ConsoleCompPacket(BlockPos busPos, int compId, String code, String code2,
                             float u, float v,
                             int color, int decimals, String unit, int holdTicks,
                             float scale, boolean delete) {
        this(busPos, compId, code, code2, u, v, color, decimals, unit, holdTicks, scale,
                "", "", "", "", false, delete);
    }

    /** 完整构造（带 RGB 颜色控制器的连接组件 / 三通道变量 / 显示原色开关）。 */
    public ConsoleCompPacket(BlockPos busPos, int compId, String code, String code2,
                             float u, float v,
                             int color, int decimals, String unit, int holdTicks,
                             float scale, String links, String rgbR, String rgbG,
                             String rgbB, boolean showOriginal, boolean delete) {
        this(busPos, compId, code, code2, u, v, color, decimals, unit, holdTicks, scale,
                links, rgbR, rgbG, rgbB, showOriginal, "", delete);
    }

    /** 完整构造（额外带同步指示器的转速变量名）。 */
    public ConsoleCompPacket(BlockPos busPos, int compId, String code, String code2,
                             float u, float v,
                             int color, int decimals, String unit, int holdTicks,
                             float scale, String links, String rgbR, String rgbG,
                             String rgbB, boolean showOriginal, String speedVar,
                             boolean delete) {
        this(busPos, compId, code, code2, u, v, color, decimals, unit, holdTicks, scale,
                links, rgbR, rgbG, rgbB, showOriginal, speedVar, "", delete);
    }

    /** 完整构造（额外带同步指示器的转速 / 灯亮状态变量名）。 */
    public ConsoleCompPacket(BlockPos busPos, int compId, String code, String code2,
                             float u, float v,
                             int color, int decimals, String unit, int holdTicks,
                             float scale, String links, String rgbR, String rgbG,
                             String rgbB, boolean showOriginal, String speedVar,
                             String lampVar, boolean delete) {
        this(busPos, compId, code, code2, u, v, color, decimals, unit, holdTicks, scale,
                links, rgbR, rgbG, rgbB, showOriginal, speedVar, lampVar,
                ConsoleBusBlockEntity.CompEntry.DEFAULT_COVER_TICKS, delete);
    }

    /** 完整构造（额外带安全按钮的盖子自动关闭时间）。 */
    public ConsoleCompPacket(BlockPos busPos, int compId, String code, String code2,
                             float u, float v,
                             int color, int decimals, String unit, int holdTicks,
                             float scale, String links, String rgbR, String rgbG,
                             String rgbB, boolean showOriginal, String speedVar,
                             String lampVar, int coverTicks, boolean delete) {
        this(busPos, compId, code, code2, u, v, color, decimals, unit, holdTicks, scale,
                links, rgbR, rgbG, rgbB, showOriginal, speedVar, lampVar, coverTicks,
                ConsoleBusBlockEntity.CompEntry.KEY_MODE_TOGGLE, delete);
    }

    /** 完整构造（额外带钥匙开关的钥匙模式）。 */
    public ConsoleCompPacket(BlockPos busPos, int compId, String code, String code2,
                             float u, float v,
                             int color, int decimals, String unit, int holdTicks,
                             float scale, String links, String rgbR, String rgbG,
                             String rgbB, boolean showOriginal, String speedVar,
                             String lampVar, int coverTicks, int keyMode, boolean delete) {
        this(busPos, compId, code, code2, u, v, color, decimals, unit, holdTicks, scale,
                links, rgbR, rgbG, rgbB, showOriginal, speedVar, lampVar, coverTicks,
                keyMode, "", delete);
    }

    /** 完整构造（额外带警报器的静音变量名）。 */
    public ConsoleCompPacket(BlockPos busPos, int compId, String code, String code2,
                             float u, float v,
                             int color, int decimals, String unit, int holdTicks,
                             float scale, String links, String rgbR, String rgbG,
                             String rgbB, boolean showOriginal, String speedVar,
                             String lampVar, int coverTicks, int keyMode,
                             String muteVar, boolean delete) {
        this.busPos = busPos;
        this.compId = compId;
        this.code = code == null ? "" : code;
        this.code2 = code2 == null ? "" : code2;
        this.u = u;
        this.v = v;
        this.color = color;
        this.decimals = decimals;
        this.unit = unit == null ? "" : unit;
        this.holdTicks = holdTicks;
        this.scale = scale;
        this.links = links == null ? "" : links;
        this.rgbR = rgbR == null ? "" : rgbR;
        this.rgbG = rgbG == null ? "" : rgbG;
        this.rgbB = rgbB == null ? "" : rgbB;
        this.showOriginal = showOriginal;
        this.speedVar = speedVar == null ? "" : speedVar;
        this.lampVar = lampVar == null ? "" : lampVar;
        this.muteVar = muteVar == null ? "" : muteVar;
        this.coverTicks = coverTicks;
        this.keyMode = keyMode;
        this.delete = delete;
    }

    public static void encode(ConsoleCompPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.busPos);
        buf.writeVarInt(msg.compId);
        buf.writeUtf(msg.code, 64);
        buf.writeUtf(msg.code2, 64);
        buf.writeFloat(msg.u);
        buf.writeFloat(msg.v);
        buf.writeInt(msg.color);
        buf.writeVarInt(msg.decimals);
        buf.writeUtf(msg.unit, 16);
        buf.writeVarInt(msg.holdTicks);
        buf.writeFloat(msg.scale);
        buf.writeUtf(msg.links, 1024);
        buf.writeUtf(msg.rgbR, 64);
        buf.writeUtf(msg.rgbG, 64);
        buf.writeUtf(msg.rgbB, 64);
        buf.writeBoolean(msg.showOriginal);
        buf.writeUtf(msg.speedVar, 64);
        buf.writeUtf(msg.lampVar, 64);
        buf.writeVarInt(msg.coverTicks);
        buf.writeVarInt(msg.keyMode);
        buf.writeUtf(msg.muteVar, 64);
        buf.writeBoolean(msg.delete);
    }

    public static ConsoleCompPacket decode(FriendlyByteBuf buf) {
        return new ConsoleCompPacket(buf.readBlockPos(), buf.readVarInt(), buf.readUtf(64),
                buf.readUtf(64), buf.readFloat(), buf.readFloat(), buf.readInt(),
                buf.readVarInt(), buf.readUtf(16), buf.readVarInt(), buf.readFloat(),
                buf.readUtf(1024), buf.readUtf(64), buf.readUtf(64), buf.readUtf(64),
                buf.readBoolean(), buf.readUtf(64), buf.readUtf(64),
                buf.readVarInt(), buf.readVarInt(), buf.readUtf(64), buf.readBoolean());
    }

    public static void handle(ConsoleCompPacket msg, Supplier<NetworkEvent.Context> ctx) {
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
            ConsoleBusBlockEntity.CompEntry existing = bus.findComp(msg.compId);
            if (existing == null) {
                return;
            }
            // 拒绝非有限数值：GUI 的 Float.parseFloat 接受 "NaN"/"Infinity"，
            // 而 Math.max/min 对 NaN 恒返回 NaN，会让下面的距离校验被 NaN 比较绕过，
            // 并把这个值一路写进方块实体（坐标/缩放全部失效）
            if (!Float.isFinite(msg.u) || !Float.isFinite(msg.v) || !Float.isFinite(msg.scale)) {
                return;
            }

            // 距离校验：以服务端保存的组件位置为准，防止远处伪造包
            Vec3 point = ConsoleSurface.toWorld(bus.getOrigin(), bus.getSizeX(), bus.getSizeY(),
                    bus.getSizeZ(), existing.face, bus.getBusFacing(),
                    existing.u, existing.v, 0.0D);
            if (player.distanceToSqr(point) > 64.0D) {
                return;
            }

            int status = msg.delete
                    ? bus.applyCompDelete(msg.compId)
                    : bus.applyCompEdit(msg.compId, msg.code, msg.code2, msg.u, msg.v, msg.color,
                            msg.decimals, msg.unit, msg.holdTicks, msg.scale,
                            msg.links, msg.rgbR, msg.rgbG, msg.rgbB, msg.showOriginal,
                            msg.speedVar, msg.lampVar, msg.muteVar,
                            msg.coverTicks, msg.keyMode);
            switch (status) {
                case ConsoleBusBlockEntity.COMP_NOT_FORMED -> hint(player, "message.cc_rc.console.need_form");
                case ConsoleBusBlockEntity.COMP_LOCKED -> hint(player, "message.cc_rc.console.comp_locked");
                case ConsoleBusBlockEntity.COMP_OUTSIDE -> hint(player, "message.cc_rc.console.comp_outside");
                case ConsoleBusBlockEntity.COMP_LIMIT -> hint(player, "message.cc_rc.console.comp_limit");
                case ConsoleBusBlockEntity.COMP_DUP_POS -> hint(player, "message.cc_rc.console.comp_dup_pos");
                case ConsoleBusBlockEntity.COMP_DUP_CODE -> hint(player, "message.cc_rc.console.comp_dup_code");
                case ConsoleBusBlockEntity.COMP_BAD_CODE -> hint(player, "message.cc_rc.console.comp_bad_code");
                default -> {
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void hint(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
