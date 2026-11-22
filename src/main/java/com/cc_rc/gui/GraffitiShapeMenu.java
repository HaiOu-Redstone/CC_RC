package com.cc_rc.gui;

import com.cc_rc.ModMenuTypes;
import com.cc_rc.block.graffiti.GraffitiShapeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

/**
 * 涂鸦形状设置菜单（无物品槽位，仅作为打开 GraffitiShapeScreen 的载体）。
 *
 * 打开流程与涂鸦文字一致：
 *  - 服务端在 GraffitiShapeBlock.use（或画图工具放置后）调用
 *    {@link #open(ServerPlayer, GraffitiShapeBlockEntity)}，把坐标与当前配置写进同步数据；
 *  - 客户端由 IForgeMenuType 从同步数据读出，GraffitiShapeScreen 据此初始化按钮与输入框；
 *  - 「完成」时由 GraffitiShapeScreen 发送 GraffitiShapeConfigPacket（C2S）写回方块实体。
 */
public class GraffitiShapeMenu extends AbstractContainerMenu {

    /** 目标方块坐标（服务端/客户端一致）。 */
    public final BlockPos pos;
    /** 当前配置（作为界面初始值）。 */
    public final int type;
    public final String preset;
    public final int color;
    public final float size;
    public final float width;
    public final float rotation;
    public final float dx;
    public final float dy;

    public GraffitiShapeMenu(int windowId, BlockPos pos, int type, String preset, int color,
                             float size, float width, float rotation, float dx, float dy) {
        super(ModMenuTypes.GRAFFITI_SHAPE.get(), windowId);
        this.pos = pos;
        this.type = type;
        this.preset = preset;
        this.color = color;
        this.size = size;
        this.width = width;
        this.rotation = rotation;
        this.dx = dx;
        this.dy = dy;
    }

    /** 服务端打开：把方块实体的当前配置写进同步数据。 */
    public static void open(ServerPlayer player, GraffitiShapeBlockEntity be) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (windowId, inventory, p) -> new GraffitiShapeMenu(windowId, be.getBlockPos(),
                        be.getShapeType().ordinal(), be.getPreset(), be.getColor(),
                        be.getSize(), be.getWidth(), be.getRotation(), be.getDx(), be.getDy()),
                Component.translatable("block.cc_rc.graffiti_shape")),
                buf -> {
                    buf.writeBlockPos(be.getBlockPos());
                    buf.writeVarInt(be.getShapeType().ordinal());
                    buf.writeUtf(be.getPreset(), GraffitiShapeBlockEntity.MAX_PRESET_LENGTH);
                    buf.writeVarInt(be.getColor());
                    buf.writeFloat(be.getSize());
                    buf.writeFloat(be.getWidth());
                    buf.writeFloat(be.getRotation());
                    buf.writeFloat(be.getDx());
                    buf.writeFloat(be.getDy());
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
