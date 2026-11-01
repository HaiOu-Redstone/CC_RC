package com.cc_rc.gui;

import com.cc_rc.ModMenuTypes;
import com.cc_rc.block.poster.PosterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

/**
 * 海报设置菜单（无物品槽位，仅作为打开 PosterScreen 的载体）。
 *
 * 打开流程（参考 edit_text 菜单）：
 *  - 服务端在 PosterBlock.use 中调用 {@link #open(ServerPlayer, PosterBlockEntity)}，
 *    把方块坐标与「当前图片 / 缩放 / dy / dz」写进同步数据；
 *  - 客户端由 IForgeMenuType 从同步数据读出这些值，PosterScreen 据此初始化缩略图选中项与输入框；
 *  - 「应用」时由 PosterScreen 发送 PosterConfigPacket（C2S）写回方块实体。
 */
public class PosterMenu extends AbstractContainerMenu {

    /** 目标方块坐标（服务端/客户端一致）。 */
    public final BlockPos pos;
    /** 当前图片索引 / 缩放倍率 / 垂直偏移 / 水平偏移（作为界面初始值）。 */
    public final int image;
    public final float scale;
    public final float dy;
    public final float dz;

    public PosterMenu(int windowId, BlockPos pos, int image, float scale, float dy, float dz) {
        super(ModMenuTypes.POSTER.get(), windowId);
        this.pos = pos;
        this.image = image;
        this.scale = scale;
        this.dy = dy;
        this.dz = dz;
    }

    /** 服务端打开：把方块实体的当前配置写进同步数据。 */
    public static void open(ServerPlayer player, PosterBlockEntity be) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (windowId, inventory, p) -> new PosterMenu(windowId, be.getBlockPos(),
                        be.getImage(), be.getScale(), be.getDy(), be.getDz()),
                Component.translatable("gui.cc_rc.poster.title")),
                buf -> {
                    buf.writeBlockPos(be.getBlockPos());
                    buf.writeVarInt(be.getImage());
                    buf.writeFloat(be.getScale());
                    buf.writeFloat(be.getDy());
                    buf.writeFloat(be.getDz());
                });
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
