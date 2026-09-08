package com.cc_rc.item;

import com.cc_rc.Config;
import com.cc_rc.block.Plotter.PlotterBlock;
import com.cc_rc.block.Plotter.PlotterBlockEntity;
import com.cc_rc.block.digital_knob.DigitalKnobBlock;
import com.cc_rc.block.digital_knob.DigitalKnobBlockEntity;
import com.cc_rc.block.key_cabinet.KeyCabinetBlock;
import com.cc_rc.block.key_distributor.KeyCabinetRecord;
import com.cc_rc.block.key_distributor.KeyDistributorBlock;
import com.cc_rc.block.key_distributor.KeyDistributorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
 * 对着 KeyCabinet 右键记录其坐标与朝向到工具 NBT；再对 KeyDistributor 右键把记录录入控制器
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
     * 针对钥匙柜：右键记录其坐标与朝向到工具 NBT；
     * 针对钥匙分发控制器：右键把工具上记录的钥匙柜坐标与朝向录入控制器。
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockState state = level.getBlockState(pos);

        // 钥匙柜：记录其朝向与坐标到工具 NBT（供后续录入控制器）
        if (state.getBlock() instanceof KeyCabinetBlock) {
            // 客户端也返回 SUCCESS 消费交互，防止后续逻辑拦截
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            Direction facing = state.getValue(KeyCabinetBlock.FACING);
            CompoundTag tag = context.getItemInHand().getOrCreateTag();
            tag.putInt("KC_X", pos.getX());
            tag.putInt("KC_Y", pos.getY());
            tag.putInt("KC_Z", pos.getZ());
            tag.putString("KC_Facing", facing.getName());
            if (player instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.translatable("message.cc_rc.key_cabinet_recorded",
                        pos.getX(), pos.getY(), pos.getZ(), facing.getName()), true);
            }
            return InteractionResult.SUCCESS;
        }

        // 钥匙分发控制器：把工具上记录的钥匙柜坐标与朝向录入控制器
        if (state.getBlock() instanceof KeyDistributorBlock) {
            // 客户端也返回 SUCCESS 消费交互，防止后续逻辑拦截
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            CompoundTag tag = context.getItemInHand().getOrCreateTag();
            if (!tag.contains("KC_X")) {
                if (player instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.translatable("message.cc_rc.key_distributor_no_record"), true);
                }
                return InteractionResult.SUCCESS;
            }
            BlockPos cabinetPos = new BlockPos(tag.getInt("KC_X"), tag.getInt("KC_Y"), tag.getInt("KC_Z"));
            Direction cabinetFacing = Direction.byName(tag.getString("KC_Facing"));
            if (cabinetFacing == null) cabinetFacing = Direction.NORTH;
            if (level.getBlockEntity(pos) instanceof KeyDistributorBlockEntity be) {
                boolean added = be.addRecord(new KeyCabinetRecord(cabinetPos, cabinetFacing));
                if (player instanceof ServerPlayer sp) {
                    if (added) {
                        sp.sendSystemMessage(Component.translatable("message.cc_rc.key_distributor_added",
                                cabinetPos.getX(), cabinetPos.getY(), cabinetPos.getZ()), true);
                    } else {
                        sp.sendSystemMessage(Component.translatable("message.cc_rc.key_distributor_duplicate",
                                cabinetPos.getX(), cabinetPos.getY(), cabinetPos.getZ()), true);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }

        // 圆盘记录仪：切换采样模式
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