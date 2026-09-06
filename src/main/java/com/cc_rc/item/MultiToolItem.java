package com.cc_rc.item;

import com.cc_rc.Config;
import com.cc_rc.block.Plotter.PlotterBlock;
import com.cc_rc.block.Plotter.PlotterBlockEntity;
import com.cc_rc.block.digital_knob.DigitalKnobBlock;
import com.cc_rc.block.digital_knob.DigitalKnobBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 多功能工具物品
 * 不可堆叠，无耐久，无工具属性
 * 对着 Plotter 右键切换模式（1→2→…→9→1），Shift+右键重置为1
 * 对着 DigitalKnob 右键切换百分比模式开关，Shift+右键恢复默认（开启）
 * 数字调节器的处理放在 onItemUseFirst 中，优先于方块自身按钮交互执行（无论点击位置都切换模式）
 */
public class MultiToolItem extends Item {

    public MultiToolItem(Properties properties) {
        super(properties);
    }

    /**
     * 优先执行：针对数字调节器切换百分比模式开关（Shfit+右键恢复默认开启）。
     * 返回非 PASS 会拦截后续的 Block.use 与 Item.useOn。
     */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof DigitalKnobBlock)) {
            return InteractionResult.PASS;
        }

        // 客户端也返回 SUCCESS 消费交互，防止后续逻辑拦截
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (level.getBlockEntity(pos) instanceof DigitalKnobBlockEntity be) {
            boolean percentMode;
            if (player.isCrouching()) {
                percentMode = be.setPercentMode(true);
            } else {
                percentMode = be.togglePercentMode();
            }
            sendPercentModeMessage(player, percentMode);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * 针对圆盘记录仪切换采样模式：右键切换（1→2→…→9→1），Shift+右键重置为1。
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof PlotterBlock)) {
            return InteractionResult.PASS;
        }

        // 客户端也返回 SUCCESS 消费交互，防止后续逻辑拦截
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (level.getBlockEntity(pos) instanceof PlotterBlockEntity be) {
            int newMode;
            if (player.isCrouching()) {
                newMode = be.resetMode();
            } else {
                newMode = be.nextMode();
            }
            sendModeMessage(player, newMode);
        }
        return InteractionResult.SUCCESS;
    }

    private void sendPercentModeMessage(Player player, boolean percentMode) {
        if (player instanceof ServerPlayer sp) {
            Component msg = Component.translatable("message.cc_rc.digital_knob_percent",
                    Component.translatable(percentMode ? "message.cc_rc.on" : "message.cc_rc.off"));
            sp.sendSystemMessage(msg, true);
        }
    }

    private void sendModeMessage(Player player, int mode) {
        if (player instanceof ServerPlayer sp) {
            Component msg;
            int interval = Config.getPlotterTickInterval(mode);
            if (interval <= 0) {
                msg = Component.translatable("message.cc_rc.plotter_mode_passive");
            } else {
                msg = Component.translatable("message.cc_rc.plotter_mode", mode, interval);
            }
            sp.sendSystemMessage(msg, true);
        }
    }
}