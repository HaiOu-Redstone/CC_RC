package com.cc_rc.gui;

import com.cc_rc.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * 编辑工具文字编辑菜单（普通可显示名称的方块）。
 *
 * 无物品槽位（纯文本编辑界面），仅作为「打开 GUI」的载体：
 *  - 服务端创建时（openMenu 的 MenuProvider）传入方块坐标 + 当前文字；
 *  - 客户端创建时由 IForgeMenuType 从同步数据中读出坐标、当前文字与组件 id。
 * 屏幕（EditTextScreen）读取这些字段初始化输入框，提交时按 {@link #compId} 分流：
 * -1 走 EditTextPacket 写回方块文字，>= 0 走 ConsoleCompTextPacket 写回组件附带文字。
 *
 * 控制台结构表面文字使用独立菜单 {@link ConsoleTextMenu}。
 * 控制台组件的「附带文字」复用本菜单，用 {@link #compId} 区分提交目标。
 */
public class EditTextMenu extends AbstractContainerMenu {

    /** 目标方块坐标（服务端/客户端一致，来自打开时的同步数据） */
    public final BlockPos pos;

    /** 当前显示文字（作为输入框初始内容；客户端来自同步数据） */
    public final Component currentText;

    /** 目标组件 id：-1 = 普通方块文字；>= 0 = 控制台组件的附带文字。 */
    public final int compId;

    /** 服务端/客户端共用构造：接收坐标、当前文字与目标组件 id */
    public EditTextMenu(int windowId, BlockPos pos, Component currentText, int compId) {
        super(ModMenuTypes.EDIT_TEXT.get(), windowId);
        this.pos = pos;
        this.currentText = currentText == null ? Component.empty() : currentText;
        this.compId = compId;
    }

    @Override
    public boolean stillValid(Player player) {
        // 方块不销毁即可一直打开；实际编辑校验在网络包服务端处理（距离 + ITextDisplay）
        return true;
    }

    // 本菜单无物品槽位（纯文本编辑界面）：快速移动直接返回空栈
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
