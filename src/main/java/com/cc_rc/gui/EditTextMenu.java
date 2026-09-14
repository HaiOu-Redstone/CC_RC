package com.cc_rc.gui;

import com.cc_rc.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 编辑工具文字编辑菜单。
 *
 * 无物品槽位（纯文本编辑界面），仅作为「打开 GUI」的载体：
 *  - 服务端创建时（openMenu 的 MenuProvider）传入方块坐标 + 当前文字；
 *  - 客户端创建时由 IForgeMenuType 从同步数据中读出坐标与当前文字。
 * 屏幕（EditTextScreen）读取这两个字段初始化输入框，提交时通过
 * EditTextPacket（C2S）把新文字写回目标方块实体。
 */
public class EditTextMenu extends AbstractContainerMenu {

    /** 目标方块坐标（服务端/客户端一致，来自打开时的同步数据） */
    public final BlockPos pos;

    /** 当前显示文字（作为输入框初始内容；客户端来自同步数据） */
    public final Component currentText;

    /** 服务端/客户端共用构造：接收坐标与当前文字 */
    public EditTextMenu(int windowId, BlockPos pos, Component currentText) {
        super(ModMenuTypes.EDIT_TEXT.get(), windowId);
        this.pos = pos;
        this.currentText = currentText == null ? Component.empty() : currentText;
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