package com.cc_rc.gui;

import com.cc_rc.ModMenuTypes;
import com.cc_rc.block.graffiti.GraffitiBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

/**
 * 涂鸦文字编辑菜单（无物品槽位，仅作为打开 GraffitiScreen 的载体）。
 *
 * 打开流程（参考海报设置菜单）：
 *  - 服务端在 GraffitiBlock.use（或编辑工具放置后）调用
 *    {@link #open(ServerPlayer, GraffitiBlockEntity)}，把坐标与当前配置写进同步数据；
 *  - 客户端由 IForgeMenuType 从同步数据读出，GraffitiScreen 据此初始化输入框与按钮；
 *  - 「完成」时由 GraffitiScreen 发送 GraffitiConfigPacket（C2S）写回方块实体。
 */
public class GraffitiMenu extends AbstractContainerMenu {

    /** 目标方块坐标（服务端/客户端一致）。 */
    public final BlockPos pos;
    /** 当前配置（作为界面初始值）。 */
    public final String text;
    public final int color;
    public final float scale;
    public final float dx;
    public final float dy;
    public final boolean bold;
    public final boolean italic;
    public final boolean underline;
    public final boolean strikethrough;

    public GraffitiMenu(int windowId, BlockPos pos, String text, int color, float scale,
                        float dx, float dy, boolean bold, boolean italic,
                        boolean underline, boolean strikethrough) {
        super(ModMenuTypes.GRAFFITI.get(), windowId);
        this.pos = pos;
        this.text = text;
        this.color = color;
        this.scale = scale;
        this.dx = dx;
        this.dy = dy;
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
        this.strikethrough = strikethrough;
    }

    /** 服务端打开：把方块实体的当前配置写进同步数据。 */
    public static void open(ServerPlayer player, GraffitiBlockEntity be) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (windowId, inventory, p) -> new GraffitiMenu(windowId, be.getBlockPos(),
                        be.getText(), be.getColor(), be.getScale(), be.getDx(), be.getDy(),
                        be.isBold(), be.isItalic(), be.isUnderline(), be.isStrikethrough()),
                Component.translatable("block.cc_rc.graffiti")),
                buf -> {
                    buf.writeBlockPos(be.getBlockPos());
                    buf.writeUtf(be.getText(), GraffitiBlockEntity.MAX_TEXT);
                    buf.writeVarInt(be.getColor());
                    buf.writeFloat(be.getScale());
                    buf.writeFloat(be.getDx());
                    buf.writeFloat(be.getDy());
                    // 字体格式打包到一个字节（低 4 位）
                    buf.writeByte(flags(be.isBold(), be.isItalic(),
                            be.isUnderline(), be.isStrikethrough()));
                });
    }

    /** 把四个字体格式打包为位标志：bit0 粗体、bit1 斜体、bit2 下划线、bit3 删除线。 */
    public static int flags(boolean bold, boolean italic, boolean underline, boolean strikethrough) {
        return (bold ? 1 : 0) | (italic ? 2 : 0) | (underline ? 4 : 0) | (strikethrough ? 8 : 0);
    }

    @Override
    public boolean stillValid(Player player) {
        // 方块不销毁即可一直打开；写入校验在网络包服务端处理（距离 + 方块类型）
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
