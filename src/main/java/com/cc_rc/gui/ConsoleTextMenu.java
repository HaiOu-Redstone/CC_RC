package com.cc_rc.gui;

import com.cc_rc.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * 控制台结构表面文字编辑菜单（专用 GUI，与普通方块的 EditTextMenu 分离）。
 *
 * 相较普通文字编辑界面，额外携带：
 *  - 锚点像素坐标 (u, v)：文字框对齐表面的位置；
 *  - 锚点组合 (anchorH, anchorV)：锚在文字框的左/中/右、上/中/下；
 *  - 缩放倍率 scale（默认值来自配置项 console_text.default_scale，默认 0.15）；
 *  - 表面半宽 / 半高（halfW / halfH，像素）：供客户端把坐标夹在表面范围内。
 *
 * 提交时通过 ConsoleTextPacket（C2S）回写总线方块实体。
 */
public class ConsoleTextMenu extends AbstractContainerMenu {

    /** 总线方块坐标 */
    public final BlockPos pos;
    /** 已有文字 id；-1 表示新建 */
    public final int textId;
    /** 表面外法线方向序号（Direction.get3DDataValue） */
    public final int faceOrd;
    /** 锚点像素坐标（相对表面中心） */
    public final float u;
    public final float v;
    /** 当前文字（新建时为空） */
    public final Component currentText;
    /** 当前颜色（-1 = 默认白） */
    public final int color;
    public final boolean bold;
    public final boolean italic;
    public final boolean underline;
    public final boolean strikethrough;
    /** 水平锚点：0 左 / 1 中 / 2 右 */
    public final int anchorH;
    /** 垂直锚点：0 上 / 1 中 / 2 下 */
    public final int anchorV;
    /** 缩放倍率 */
    public final float scale;
    /** 表面半宽 / 半高（像素），用于坐标夹取 */
    public final float halfW;
    public final float halfH;

    public ConsoleTextMenu(int windowId, BlockPos pos, int textId, int faceOrd, float u, float v,
                           Component currentText, int color, boolean bold, boolean italic,
                           boolean underline, boolean strikethrough,
                           int anchorH, int anchorV, float scale, float halfW, float halfH) {
        super(ModMenuTypes.CONSOLE_TEXT.get(), windowId);
        this.pos = pos;
        this.textId = textId;
        this.faceOrd = faceOrd;
        this.u = u;
        this.v = v;
        this.currentText = currentText == null ? Component.empty() : currentText;
        this.color = color;
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
        this.strikethrough = strikethrough;
        this.anchorH = anchorH;
        this.anchorV = anchorV;
        this.scale = scale;
        this.halfW = halfW;
        this.halfH = halfH;
    }

    /** 写入打开界面的同步数据（服务端调用），顺序必须与 {@link #read} 一致。 */
    public static void write(FriendlyByteBuf buf, ConsoleTextMenu menu) {
        buf.writeBlockPos(menu.pos);
        buf.writeVarInt(menu.textId);
        buf.writeVarInt(menu.faceOrd);
        buf.writeFloat(menu.u);
        buf.writeFloat(menu.v);
        buf.writeComponent(menu.currentText);
        buf.writeInt(menu.color);
        buf.writeBoolean(menu.bold);
        buf.writeBoolean(menu.italic);
        buf.writeBoolean(menu.underline);
        buf.writeBoolean(menu.strikethrough);
        buf.writeVarInt(menu.anchorH);
        buf.writeVarInt(menu.anchorV);
        buf.writeFloat(menu.scale);
        buf.writeFloat(menu.halfW);
        buf.writeFloat(menu.halfH);
    }

    /** 读取同步数据并构造菜单（客户端 IForgeMenuType 调用）。 */
    public static ConsoleTextMenu read(int windowId, FriendlyByteBuf buf) {
        return new ConsoleTextMenu(windowId, buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(),
                buf.readFloat(), buf.readFloat(), buf.readComponent(), buf.readInt(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    @Override
    public boolean stillValid(Player player) {
        // 实际编辑校验在网络包服务端处理（成型状态 + 距离）
        return true;
    }

    // 无物品槽位：快速移动直接返回空栈
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
