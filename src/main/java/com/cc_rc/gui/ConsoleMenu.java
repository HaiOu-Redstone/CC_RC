package com.cc_rc.gui;

import com.cc_rc.ModMenuTypes;
import com.cc_rc.block.console.ConsoleBusBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

/**
 * 控制台总线菜单（无物品槽位，仅作为打开 ConsoleScreen 的载体）。
 *
 * 只需要同步一个方块坐标：尺寸、格数、成型状态、错误原因都在客户端
 * 直接读取该坐标的 ConsoleBusBlockEntity（依靠 sendBlockUpdated 同步），
 * 因此结构变化后界面能实时刷新，不需要额外的 S2C 包。
 */
public class ConsoleMenu extends AbstractContainerMenu {

    /** 总线方块坐标（服务端/客户端一致）。 */
    public final BlockPos pos;

    public ConsoleMenu(int windowId, BlockPos pos) {
        super(ModMenuTypes.CONSOLE.get(), windowId);
        this.pos = pos;
    }

    /** 服务端打开：只写入总线坐标。 */
    public static void open(ServerPlayer player, ConsoleBusBlockEntity be) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (windowId, inventory, p) -> new ConsoleMenu(windowId, be.getBlockPos()),
                Component.translatable("gui.cc_rc.console.title")),
                buf -> buf.writeBlockPos(be.getBlockPos()));
    }

    @Override
    public boolean stillValid(Player player) {
        // 方块不销毁即可一直打开；按钮操作在网络包服务端再做距离与类型校验
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
