package com.cc_rc.item;

import com.cc_rc.block.ITextDisplay;
import java.util.List;
import net.minecraft.core.BlockPos;
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

        // 目标方块实体必须是可显示名称的方块（ITextDisplay）
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        if (!(be instanceof ITextDisplay)) return InteractionResult.PASS;

        // 仅服务端打开菜单；客户端返回成功避免额外逻辑
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            openEditMenu(serverPlayer, context.getClickedPos(), ((ITextDisplay) be).getText());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 打开编辑菜单：MenuProvider 携带坐标 + 当前文字（经同步数据传给客户端）。 */
    static void openEditMenu(ServerPlayer player, BlockPos pos, Component currentText) {
        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("item.cc_rc.edit_tool");
            }

            @Override
            public com.cc_rc.gui.EditTextMenu createMenu(int windowId, Inventory inventory, Player p) {
                // 构造参数 = 方块坐标 + 当前文字（会被 IForgeMenuType 写进同步数据）
                return new com.cc_rc.gui.EditTextMenu(windowId, pos, currentText);
            }
        }, buf -> {
            buf.writeBlockPos(pos);
            buf.writeComponent(currentText);
        });
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