package com.cc_rc.block.console;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 控制台结构方块（多方块控制台的构成单元）。
 *
 * 本身是完整 1x1x1 立方体，无朝向状态；玩家把若干块拼成任意长方体后，
 * 由相邻的「控制台总线」校验并成型。结构数据全部存放在总线方块实体中，
 * 本方块只在方块实体里记录「属于哪个总线 + 当前结构状态」。
 *
 * 锁定规则：结构处于「完全成型」时禁止被挖掘（需先在总线解除锁定）。
 */
public class ConsoleBlock extends Block implements EntityBlock {

    /** 本方块所属的控制柜颜色：只有同色方块会互相连通（异色可相邻但不搭成一个结构）。 */
    public final ConsoleColor color;

    public ConsoleBlock(Properties properties) {
        this(properties, ConsoleColor.DEFAULT);
    }

    public ConsoleBlock(Properties properties, ConsoleColor color) {
        super(properties);
        this.color = color;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConsoleBlockEntity(pos, state);
    }

    /**
     * 空手右键：操作表面组件（开关 / 按钮 / 弧形仪表的加减按钮）。
     *
     * 手持物品时必须返回 PASS，否则会拦截编辑工具 / 多功能工具 / 组件物品的
     * useOn 逻辑（Forge 的调用顺序是 Block.use 先于 Item.useOn）。
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (ConsoleCompUse.tryUse(level, player, pos, hit)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /**
     * 破坏前拦截：完全成型（锁定）时禁止拆除，并提示玩家先解锁。
     * 返回 false 会阻止方块被移除（同时也就不会触发 onRemove 的失效处理）。
     */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
                                       boolean willHarvest, FluidState fluid) {
        if (!level.isClientSide && ConsoleStructureHelper.isLocked(level, pos)) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("message.cc_rc.console.locked"), true);
            }
            return false;
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    /** 方块即将被拆除（此时方块实体仍在）→ 通知总线解除成型，避免结构残留。 */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            ConsoleStructureHelper.invalidateFor(level, pos);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    /** 兜底：非玩家途径（活塞/指令等）移除方块时同样解除成型。 */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            ConsoleStructureHelper.invalidateFor(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
