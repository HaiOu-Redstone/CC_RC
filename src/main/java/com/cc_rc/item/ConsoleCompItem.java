package com.cc_rc.item;

import com.cc_rc.ModBlocks;
import com.cc_rc.block.console.ConsoleBusBlockEntity;
import com.cc_rc.block.console.ConsoleCompType;
import com.cc_rc.block.console.ConsoleStructureHelper;
import com.cc_rc.block.console.ConsoleSurface;
import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * 控制台表面组件物品（仪表 / 数字显示屏 / 指示灯 / 按钮面板 / 发信终端）。
 *
 * 使用方式：
 *  - 控制台处于「部分成型」时，手持组件物品右键控制台表面 → 在该位置放置组件
 *    （自动生成默认代号，如 GAUGE_1）；
 *  - 右键已经放置的组件 → 打开组件设置界面（代号 / 位置 / 删除）。
 *
 * 组件放置后由空手右键操作（开关切换 / 按钮按下 / 弧形仪表的加减按钮），
 * 编辑工具右键仍用于编辑附带文字。
 */
public class ConsoleCompItem extends Item {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 本物品对应的组件类型。 */
    private final ConsoleCompType type;

    /** 悬停描述颜色（青色粗体，与编辑工具一致）。 */
    private static final int DESC_COLOR = 0x55FFFF;

    public ConsoleCompItem(ConsoleCompType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockPos clickedPos = context.getClickedPos();
        // 只对控制台结构方块生效
        if (!com.cc_rc.block.console.ConsoleStructureHelper.isConsole(
                level.getBlockState(clickedPos))) {
            return InteractionResult.PASS;
        }
        // 服务端执行实际逻辑（点击位置两端一致，无需额外网络包）
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            handleUse(serverPlayer, level, clickedPos, context);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 服务端：添加组件或打开已有组件的设置界面。 */
    private void handleUse(ServerPlayer player, Level level, BlockPos clickedPos,
                           UseOnContext context) {
        BlockPos busPos = ConsoleStructureHelper.findBus(level, clickedPos);
        if (busPos == null || !(level.getBlockEntity(busPos) instanceof ConsoleBusBlockEntity bus)) {
            hint(player, "message.cc_rc.console.need_form");
            return;
        }
        ConsoleBusBlockEntity.State state = bus.getFormState();
        if (state == ConsoleBusBlockEntity.State.UNFORMED) {
            hint(player, "message.cc_rc.console.need_form");
            return;
        }
        if (state == ConsoleBusBlockEntity.State.FULL) {
            hint(player, "message.cc_rc.console.comp_locked");
            return;
        }

        // 点击点 → 表面像素坐标
        Direction face = context.getClickedFace();
        Direction busFacing = bus.getBusFacing();
        double u = ConsoleSurface.pixelU(bus.getOrigin(), bus.getSizeX(), bus.getSizeY(),
                bus.getSizeZ(), face, busFacing, context.getClickLocation());
        double v = ConsoleSurface.pixelV(bus.getOrigin(), bus.getSizeX(), bus.getSizeY(),
                bus.getSizeZ(), face, busFacing, context.getClickLocation());
        if (!ConsoleSurface.contains(face, busFacing,
                bus.getSizeX(), bus.getSizeY(), bus.getSizeZ(), u, v)) {
            hint(player, "message.cc_rc.console.comp_outside");
            return;
        }

        // 命中已有组件 → 打开设置界面（而不是重复放置）
        ConsoleBusBlockEntity.CompEntry hit = bus.findCompAt(face, (float) u, (float) v);
        if (hit != null) {
            openCompMenu(player, bus, hit);
            return;
        }

        int status = bus.applyCompAdd(this.type, face, (float) u, (float) v);
        switch (status) {
            case ConsoleBusBlockEntity.COMP_OK -> hint(player, "message.cc_rc.console.comp_added");
            case ConsoleBusBlockEntity.COMP_NOT_FORMED -> hint(player, "message.cc_rc.console.need_form");
            case ConsoleBusBlockEntity.COMP_LOCKED -> hint(player, "message.cc_rc.console.comp_locked");
            case ConsoleBusBlockEntity.COMP_OUTSIDE -> hint(player, "message.cc_rc.console.comp_outside");
            case ConsoleBusBlockEntity.COMP_LIMIT -> hint(player, "message.cc_rc.console.comp_limit");
            case ConsoleBusBlockEntity.COMP_DUP_POS -> hint(player, "message.cc_rc.console.comp_dup_pos");
            default -> {
            }
        }
    }

    /** 打开组件设置界面（服务端调用）。 */
    public static void openCompMenu(ServerPlayer player, ConsoleBusBlockEntity bus,
                                    ConsoleBusBlockEntity.CompEntry entry) {
        BlockPos busPos = bus.getBlockPos().immutable();
        int faceOrd = entry.face.get3DDataValue();
        Direction busFacing = bus.getBusFacing();
        float halfW = (float) ConsoleSurface.halfWidthPx(entry.face, busFacing,
                bus.getSizeX(), bus.getSizeY(), bus.getSizeZ());
        float halfH = (float) ConsoleSurface.halfHeightPx(entry.face, busFacing,
                bus.getSizeX(), bus.getSizeY(), bus.getSizeZ());

        try {
            NetworkHooks.openScreen(player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("组件设置");
                }

                @Override
                public com.cc_rc.gui.ConsoleCompMenu createMenu(int windowId, Inventory inventory,
                                                                Player p) {
                    return new com.cc_rc.gui.ConsoleCompMenu(windowId, busPos, entry.id,
                            faceOrd, entry.type, entry.u, entry.v, entry.code, entry.code2,
                            entry.color,
                            entry.decimals, entry.unit, entry.holdTicks, entry.scale,
                            halfW, halfH,
                            entry.links, entry.rgbR, entry.rgbG, entry.rgbB,
                            entry.showOriginal, entry.speedVar, entry.lampVar,
                            entry.muteVar, entry.coverTicks, entry.keyMode);
                }
            }, buf -> com.cc_rc.gui.ConsoleCompMenu.write(buf,
                    new com.cc_rc.gui.ConsoleCompMenu(-1, busPos, entry.id, faceOrd,
                            entry.type, entry.u, entry.v, entry.code, entry.code2, entry.color,
                            entry.decimals, entry.unit, entry.holdTicks, entry.scale,
                            halfW, halfH,
                            entry.links, entry.rgbR, entry.rgbG, entry.rgbB,
                            entry.showOriginal, entry.speedVar, entry.lampVar,
                            entry.muteVar, entry.coverTicks, entry.keyMode)));
        } catch (Throwable t) {
            LOGGER.error("组件设置界面打开失败：", t);
            player.sendSystemMessage(Component.literal(
                    "组件设置界面打开失败。若刚进行过模组重编译，请重启游戏后再试。"));
        }
    }

    private static void hint(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        tooltipComponents.add(Component.translatable("item.cc_rc.desc_console_comp")
                .withStyle(Style.EMPTY.withBold(true).withColor(DESC_COLOR)));
    }
}
