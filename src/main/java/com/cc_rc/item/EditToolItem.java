package com.cc_rc.item;

import com.cc_rc.block.ITextDisplay;
import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * 编辑工具（edit_tool）——用于编辑「可显示名称的方块」表面文字的物品。
 *
 * 功能：
 *  - 主手持编辑工具**右键**任意 ITextDisplay 方块（控制面板类、数码显示器、
 *    数字调节器、数字圆盘记录仪等）→ 打开文字编辑 GUI（类铁砧单行输入框）；
 *  - **副手**持编辑工具放置可显示名称的方块时 → 放置完成后自动打开编辑界面。
 *
 * 文字提交走 EditTextPacket（C2S）：服务端校验目标方块是 ITextDisplay 且玩家在
 * 距离内后写入 setText（空文本 = 清除显示）。物品本身不持有任何方块状态。
 *
 * 副手放置自动打开通过 BlockEvent.EntityPlaceEvent 实现：放置事件触发时方块
 * 实体已创建，若放置者是玩家且**副手**持编辑工具、且新放置的方块是 ITextDisplay，
 * 则延迟 1 tick 打开编辑界面（确保名称已从物品写入 BE）。
 */
public class EditToolItem extends Item {

    // 本类日志器（与 CcRc 同款 LogUtils 风格）
    private static final Logger LOGGER = LogUtils.getLogger();

    // 悬停描述：青色粗体"用于编辑可显示名称的方块文字"
    private static final int DESC_COLOR = 0x55FFFF;

    public EditToolItem(Properties properties) {
        super(properties);
        // 注册副手放置自动打开的事件监听（本类作为订阅者）
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockState clickedState = level.getBlockState(context.getClickedPos());

        // 控制台结构方块 / 斜面控制台：在结构表面添加 / 编辑文字（位置由客户端命中检测决定）
        if (com.cc_rc.block.console.ConsoleStructureHelper.isConsole(clickedState)
                || clickedState.getBlock()
                        instanceof com.cc_rc.block.console.ConsoleSlopeBlock) {
            if (level.isClientSide) {
                // 只有客户端有字体宽度，可精确判断是否点在某条文字的覆盖区域内
                com.cc_rc.client.ConsoleTextClient.requestEdit(context);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 目标方块实体是可显示名称的方块（ITextDisplay）→ 打开文字编辑菜单
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        if (be instanceof ITextDisplay textDisplay) {
            // 仅服务端打开菜单；客户端返回成功避免额外逻辑
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                openEditMenu(serverPlayer, context.getClickedPos(), textDisplay.getText());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 涂鸦文字：右键已有涂鸦 → 打开编辑界面
        if (clickedState.getBlock() instanceof com.cc_rc.block.graffiti.GraffitiBlock) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                    && level.getBlockEntity(context.getClickedPos())
                            instanceof com.cc_rc.block.graffiti.GraffitiBlockEntity graffiti) {
                com.cc_rc.gui.GraffitiMenu.open(serverPlayer, graffiti);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 涂鸦文字：右键任意实心墙面 → 在墙外侧放置透明涂鸦方块并立即打开编辑界面
        Direction face = context.getClickedFace();
        if (!player.isShiftKeyDown() && face.getAxis() != Direction.Axis.Y
                && clickedState.isFaceSturdy(level, context.getClickedPos(), face)) {
            BlockPos target = context.getClickedPos().relative(face);
            if (level.getBlockState(target).canBeReplaced()) {
                if (!level.isClientSide) {
                    level.setBlock(target, com.cc_rc.ModBlocks.GRAFFITI.get().defaultBlockState()
                            .setValue(com.cc_rc.block.graffiti.GraffitiBlock.FACING, face), 3);
                    if (player instanceof ServerPlayer serverPlayer
                            && level.getBlockEntity(target)
                                    instanceof com.cc_rc.block.graffiti.GraffitiBlockEntity graffiti) {
                        com.cc_rc.gui.GraffitiMenu.open(serverPlayer, graffiti);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        return InteractionResult.PASS;
    }

    /** 打开编辑菜单：MenuProvider 携带坐标 + 当前文字（经同步数据传给客户端）。 */
    static void openEditMenu(ServerPlayer player, BlockPos pos, Component currentText) {
        try {
            NetworkHooks.openScreen(player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("item.cc_rc.edit_tool");
                }

                @Override
                public com.cc_rc.gui.EditTextMenu createMenu(int windowId, Inventory inventory, Player p) {
                    // 构造参数 = 方块坐标 + 当前文字（会被 IForgeMenuType 写进同步数据）
                    return new com.cc_rc.gui.EditTextMenu(windowId, pos, currentText, -1);
                }
            }, buf -> {
                // 同步数据顺序需与 MenuType 解码一致：方块坐标 + 当前文字 + 组件 id（-1 = 普通方块）
                buf.writeBlockPos(pos);
                buf.writeComponent(currentText);
                buf.writeVarInt(-1);
            });
        } catch (Throwable t) {
            logOpenFailure(player, t);
        }
    }

    /**
     * 打开控制台结构表面文字的专用编辑界面（服务端调用，参数来自 ConsoleTextRequestPacket）。
     *
     * @param bus   总线方块实体（结构数据源）
     * @param entry 已有文字（新建时为 null，使用默认样式/锚点/缩放）
     * @param textId 已有文字 id；-1 = 新建
     * @param face  文字所在的结构外表面
     * @param u     锚点相对该表面中心的横向像素偏移
     * @param v     锚点相对该表面中心的纵向像素偏移
     */
    public static void openConsoleTextMenu(ServerPlayer player,
                                           com.cc_rc.block.console.ConsoleBusBlockEntity bus,
                                           com.cc_rc.block.console.ConsoleBusBlockEntity.TextEntry entry,
                                           int textId, net.minecraft.core.Direction face,
                                           float u, float v) {
        BlockPos busPos = bus.getBlockPos().immutable();
        int faceOrd = face.get3DDataValue();
        int anchorH = entry == null ? 1 : entry.anchorH;
        int anchorV = entry == null ? 1 : entry.anchorV;
        float scale = entry == null
                ? com.cc_rc.Config.getConsoleTextDefaultScale()
                : entry.scale;
        Component currentText = entry == null ? Component.empty() : entry.toComponent();
        int color = entry == null ? -1 : entry.color;
        boolean bold = entry != null && entry.bold;
        boolean italic = entry != null && entry.italic;
        boolean underline = entry != null && entry.underline;
        boolean strike = entry != null && entry.strikethrough;

        // 表面半宽/半高（像素）：供客户端把锚点坐标夹在表面范围内
        net.minecraft.core.Direction busFacing = bus.getBusFacing();
        float halfW = (float) com.cc_rc.block.console.ConsoleSurface.halfWidthPx(face, busFacing,
                bus.getSizeX(), bus.getSizeY(), bus.getSizeZ());
        float halfH = (float) com.cc_rc.block.console.ConsoleSurface.halfHeightPx(face, busFacing,
                bus.getSizeX(), bus.getSizeY(), bus.getSizeZ());

        try {
            NetworkHooks.openScreen(player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("item.cc_rc.edit_tool");
                }

                @Override
                public com.cc_rc.gui.ConsoleTextMenu createMenu(int windowId, Inventory inventory, Player p) {
                    return new com.cc_rc.gui.ConsoleTextMenu(windowId, busPos, textId, faceOrd,
                            u, v, currentText, color, bold, italic, underline, strike,
                            anchorH, anchorV, scale, halfW, halfH);
                }
            }, buf -> com.cc_rc.gui.ConsoleTextMenu.write(buf, new com.cc_rc.gui.ConsoleTextMenu(
                    -1, busPos, textId, faceOrd, u, v, currentText, color, bold, italic,
                    underline, strike, anchorH, anchorV, scale, halfW, halfH)));
        } catch (Throwable t) {
            logOpenFailure(player, t);
        }
    }

    /**
     * 打开控制台组件「附带文字」的编辑界面（服务端调用，参数来自 ConsoleCompTextRequestPacket）。
     *
     * 附带文字位置相对组件固定、大小固定，没有位置 / 锚点 / 缩放选项，
     * 因此直接复用普通文字编辑界面，由菜单里的 compId 区分提交目标。
     *
     * @param bus   总线方块实体（结构数据源）
     * @param entry 目标组件（附带文字内容作为输入框初始值）
     */
    public static void openCompTextMenu(ServerPlayer player,
                                        com.cc_rc.block.console.ConsoleBusBlockEntity bus,
                                        com.cc_rc.block.console.ConsoleBusBlockEntity.CompEntry entry) {
        BlockPos busPos = bus.getBlockPos().immutable();
        int compId = entry.id;
        Component currentText = entry.text == null ? Component.empty() : entry.text.toComponent();
        try {
            NetworkHooks.openScreen(player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("item.cc_rc.edit_tool");
                }

                @Override
                public com.cc_rc.gui.EditTextMenu createMenu(int windowId, Inventory inventory, Player p) {
                    return new com.cc_rc.gui.EditTextMenu(windowId, busPos, currentText, compId);
                }
            }, buf -> {
                // 同步数据顺序需与 MenuType 解码一致：方块坐标 + 当前文字 + 组件 id
                buf.writeBlockPos(busPos);
                buf.writeComponent(currentText);
                buf.writeVarInt(compId);
            });
        } catch (Throwable t) {
            logOpenFailure(player, t);
        }
    }

    /** 打开界面失败的兜底处理（避免服务端 tick 循环被 Error 打断）。 */
    private static void logOpenFailure(ServerPlayer player, Throwable t) {
        // 防御：dev 环境若在游戏运行时并发执行 gradlew build，重写的 build/classes 可能使
        // 匿名类懒加载失败（NoClassDefFoundError）→ 直接崩溃整服。
        // 这里捕获并提示玩家重启游戏，避免服务端 tick 循环被 Error 打断。
        LOGGER.error("编辑工具菜单打开失败（匿名类加载异常，可能因游戏运行中并发重编译导致）：", t);
        player.sendSystemMessage(Component.literal(
                "编辑工具菜单打开失败（类加载异常）。若刚进行过模组重编译，请重启游戏后再试。"));
    }

    /**
     * 副手放置自动打开：玩家放置方块完成时（BE 已创建），若其副手持编辑工具
     * 且放置的方块是 ITextDisplay，则延迟 1 tick 打开编辑界面。
     * 延迟确保放置流程（含 setPlacedBy 写入自定义名称）完整结束。
     */
    @SubscribeEvent
    public void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() == null || event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // 仅副手持编辑工具时触发
        if (!(player.getOffhandItem().getItem() instanceof EditToolItem)) return;

        // 新放置的方块必须是可显示名称的方块（ITextDisplay）
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (!(be instanceof ITextDisplay display)) return;

        // 延迟 1 tick 再打开（放置/名称写入流程在事件返回后完成）
        BlockPos pos = event.getPos().immutable();
        player.server.tell(new net.minecraft.server.TickTask(player.server.getTickCount() + 1, () -> {
            BlockEntity latest = player.level().getBlockEntity(pos);
            if (latest instanceof ITextDisplay latestDisplay) {
                openEditMenu(player, pos, latestDisplay.getText());
            }
        }));
    }

    // 悬停描述
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        tooltipComponents.add(Component.translatable("item.cc_rc.desc_edit_tool")
                .withStyle(Style.EMPTY.withBold(true).withColor(DESC_COLOR)));
    }
}