package com.cc_rc.gui;

import com.cc_rc.ModMenuTypes;
import com.cc_rc.block.console.ConsoleCompType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * 控制台表面组件编辑菜单。
 *
 * 携带数据：
 *  - 总线坐标与组件 id（服务端据此定位并回写）；
 *  - 组件类型、所在表面、位置 (u, v) 与代号；
 *  - 表面半宽 / 半高（像素），供客户端把位置夹在表面范围内。
 *
 * 当前组件只有模型与代号，没有功能逻辑。
 */
public class ConsoleCompMenu extends AbstractContainerMenu {

    /** 总线方块坐标 */
    public final BlockPos pos;
    /** 组件 id */
    public final int compId;
    /** 组件所在表面（Direction.get3DDataValue） */
    public final int faceOrd;
    /** 组件类型 */
    public final ConsoleCompType type;
    /** 组件位置（相对该表面中心的像素偏移） */
    public final float u;
    public final float v;
    /** 组件代号（英文字母 / 数字 / 下划线） */
    public final String code;
    /** 第二名称（仅圆盘记录仪：推送触发名称；空 = 不设） */
    public final String code2;
    /** 组件颜色（0xRRGGBB；-1 表示保持贴图原样） */
    public final int color;
    /** 数字显示屏：保留小数位（其它类型沿用 0，不参与显示） */
    public final int decimals;
    /** 数字显示屏：显示单位（空 = 不显示） */
    public final String unit;
    /** 按钮：弹起时间（tick，0 = 松手立刻弹起；-1 = 跟随配置文件） */
    public final int holdTicks;
    /** 组件缩放（1.0 = 模型默认大小） */
    public final float scale;
    /** 表面半宽 / 半高（像素） */
    public final float halfW;
    public final float halfH;
    /** RGB 颜色控制器：连接的组件代号（用「;」分隔）。 */
    public final String links;
    /** RGB 颜色控制器：R / G / B 三个通道变量名（空 = 不启用）。 */
    public final String rgbR;
    public final String rgbG;
    public final String rgbB;
    /** RGB 颜色控制器：是否显示原色（= 停用控制）。 */
    public final boolean showOriginal;
    /** 同步指示器：转速变量名（空 = 自动使用 代号 + "_s"）。 */
    public final String speedVar;
    /** 同步指示器：灯亮状态变量名（空 = 自动使用 代号 + "_b"）。 */
    public final String lampVar;
    /** 警报器：静音状态变量名（空 = 自动使用 代号 + "_Q"）。 */
    public final String muteVar;
    /** 安全按钮：盖子自动关闭时间（tick，0 = 不自动关闭）。 */
    public final int coverTicks;
    /** 钥匙开关：钥匙模式（0 = 开闭，1 = 常开，2 = 常闭）。 */
    public final int keyMode;

    public ConsoleCompMenu(int windowId, BlockPos pos, int compId, int faceOrd,
                           ConsoleCompType type, float u, float v, String code, String code2,
                           int color,
                           int decimals, String unit, int holdTicks, float scale,
                           float halfW, float halfH,
                           String links, String rgbR, String rgbG, String rgbB,
                           boolean showOriginal, String speedVar, String lampVar,
                           String muteVar, int coverTicks, int keyMode) {
        super(ModMenuTypes.CONSOLE_COMP.get(), windowId);
        this.pos = pos;
        this.compId = compId;
        this.faceOrd = faceOrd;
        this.type = type;
        this.u = u;
        this.v = v;
        this.code = code == null ? "" : code;
        this.code2 = code2 == null ? "" : code2;
        this.color = color;
        this.decimals = decimals;
        this.unit = unit == null ? "" : unit;
        this.holdTicks = holdTicks;
        this.scale = scale;
        this.halfW = halfW;
        this.halfH = halfH;
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
    }

    /** 写入打开界面的同步数据（服务端调用），顺序必须与 {@link #read} 一致。 */
    public static void write(FriendlyByteBuf buf, ConsoleCompMenu menu) {
        buf.writeBlockPos(menu.pos);
        buf.writeVarInt(menu.compId);
        buf.writeVarInt(menu.faceOrd);
        buf.writeVarInt(menu.type.ordinal());
        buf.writeFloat(menu.u);
        buf.writeFloat(menu.v);
        buf.writeUtf(menu.code, 64);
        buf.writeUtf(menu.code2, 64);
        buf.writeInt(menu.color);
        buf.writeVarInt(menu.decimals);
        buf.writeUtf(menu.unit, 16);
        buf.writeVarInt(menu.holdTicks);
        buf.writeFloat(menu.scale);
        buf.writeFloat(menu.halfW);
        buf.writeFloat(menu.halfH);
        buf.writeUtf(menu.links, 512);
        buf.writeUtf(menu.rgbR, 64);
        buf.writeUtf(menu.rgbG, 64);
        buf.writeUtf(menu.rgbB, 64);
        buf.writeBoolean(menu.showOriginal);
        buf.writeUtf(menu.speedVar, 64);
        buf.writeUtf(menu.lampVar, 64);
        buf.writeUtf(menu.muteVar, 64);
        buf.writeVarInt(menu.coverTicks);
        buf.writeVarInt(menu.keyMode);
    }

    /** 读取同步数据并构造菜单（客户端 IForgeMenuType 调用）。 */
    public static ConsoleCompMenu read(int windowId, FriendlyByteBuf buf) {
        return new ConsoleCompMenu(windowId, buf.readBlockPos(), buf.readVarInt(),
                buf.readVarInt(), ConsoleCompType.byOrdinal(buf.readVarInt()),
                buf.readFloat(), buf.readFloat(), buf.readUtf(64), buf.readUtf(64), buf.readInt(),
                buf.readVarInt(), buf.readUtf(16), buf.readVarInt(), buf.readFloat(),
                buf.readFloat(), buf.readFloat(),
                buf.readUtf(512), buf.readUtf(64), buf.readUtf(64), buf.readUtf(64),
                buf.readBoolean(), buf.readUtf(64), buf.readUtf(64), buf.readUtf(64),
                buf.readVarInt(), buf.readVarInt());
    }

    @Override
    public boolean stillValid(Player player) {
        // 实际校验在网络包服务端处理（成型状态 + 距离）；这里只负责「走远自动关闭」
        return player.distanceToSqr(
                this.pos.getX() + 0.5D, this.pos.getY() + 0.5D, this.pos.getZ() + 0.5D) <= 64.0D;
    }

    // 无物品槽位
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    /** 供界面显示的类型名。 */
    public Component typeName() {
        return Component.translatable(this.type.translationKey());
    }
}
