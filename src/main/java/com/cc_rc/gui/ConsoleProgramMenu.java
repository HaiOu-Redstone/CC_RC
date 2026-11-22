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
 * 控制柜程序界面菜单（无物品槽位）。
 *
 * 只需要同步总线坐标：变量表、组件列表、成型状态都存放在总线方块实体里，
 * 客户端直接读取该坐标的 {@link ConsoleBusBlockEntity}（依赖 sendBlockUpdated 同步），
 * 因此变量增删与数值保存后界面会自动刷新，不需要额外的 S2C 包。
 */
public class ConsoleProgramMenu extends AbstractContainerMenu {

    /** 总线方块坐标（服务端 / 客户端一致）。 */
    public final BlockPos pos;

    public ConsoleProgramMenu(int windowId, BlockPos pos) {
        super(ModMenuTypes.CONSOLE_PROGRAM.get(), windowId);
        this.pos = pos;
    }

    /** 服务端打开：先把绑定变量的值刷新为组件当前值，再写入总线坐标。 */
    public static void open(ServerPlayer player, ConsoleBusBlockEntity be) {
        // 界面关闭期间玩家可能手动操作过开关 / 仪表，打开前同步一次，避免显示旧值
        be.refreshVarsFromComponents();
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (windowId, inventory, p) -> new ConsoleProgramMenu(windowId, be.getBlockPos()),
                Component.translatable("gui.cc_rc.program.title")),
                buf -> buf.writeBlockPos(be.getBlockPos()));
    }

    @Override
    public boolean stillValid(Player player) {
        // 变量操作在网络包服务端再做距离与成型状态校验；
        // 这里只需保证「太远就自动关闭界面」——原先恒 true 会让玩家走远后
        // 界面仍开着，而客户端读不到方块实体时会走空指针路径
        return player.distanceToSqr(
                this.pos.getX() + 0.5D, this.pos.getY() + 0.5D, this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
