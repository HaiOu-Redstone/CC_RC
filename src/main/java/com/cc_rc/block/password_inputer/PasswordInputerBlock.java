package com.cc_rc.block.password_inputer;

import com.cc_rc.block.console_panel.ConsolePanelBlock;
import com.cc_rc.item.PasswordCrackerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 密码输入器方块
 *
 * 继承 ConsolePanelBlock 复用放置逻辑、碰撞箱与文字显示（可贴墙/地板/天花板，14x14x3 面板形状）。
 * 状态：FACING + FACE（继承）+ POWERED（on/off，默认 off）。
 * 交互：手持破解器（PasswordCrackerItem）右键本方块 → 由 PasswordCrackManager 启动破解流程；
 *       破解完成后方块切为 on（POWERED=true），向贴墙/连接方向强充能输出 15 信号（类似按钮/刷卡机），
 *       1 秒（20 tick）后自动变回 off。
 */
public class PasswordInputerBlock extends ConsolePanelBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public PasswordInputerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        // 手持染料右键 → 优先染色面板文字（不影响密码破解功能）
        if (ConsolePanelBlock.handleDyeInteraction(level, pos, player, hand)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;

        // 只有手持破解器且方块处于关闭状态时才启动破解
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof PasswordCrackerItem)) return InteractionResult.PASS;
        if (state.getValue(POWERED)) return InteractionResult.CONSUME;

        if (player instanceof ServerPlayer serverPlayer) {
            PasswordCrackManager.start(serverPlayer, level, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 计划 tick：开启 1 秒（20 tick）后自动变回 off
        if (state.getValue(POWERED)) {
            BlockState next = state.setValue(POWERED, false);
            level.setBlock(pos, next, 3);
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 0.3F, 0.5F);
            updateNeighbours(next, level, pos);
        }
    }

    /** 破解成功时的开启入口（由 PasswordCrackManager 调用）：切 on + 更新邻居 + 排程 20 tick 后自动关。 */
    public void powerOn(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof PasswordInputerBlock) || state.getValue(POWERED)) return;
        BlockState next = state.setValue(POWERED, true);
        level.setBlock(pos, next, 3);
        level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3F, 0.6F);
        updateNeighbours(next, level, pos);
        level.scheduleTick(pos, this, 20);
    }

    private void updateNeighbours(BlockState state, Level level, BlockPos pos) {
        // LeverBlock 字节码金标准：第二次刷新 pos.relative(getConnectedDirection(state).getOpposite())
        Direction connected = getConnectedDirection(state);
        level.updateNeighborsAt(pos, this);
        level.updateNeighborsAt(pos.relative(connected.getOpposite()), this);
    }

    /**
     * 与 ConsoleButtonBlock 相同的 FACE 真值表：
     * FLOOR → UP；CEILING → DOWN；WALL → FACING 本身。
     */
    public static Direction getConnectedDirection(BlockState state) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> Direction.UP;
            case CEILING -> Direction.DOWN;
            case WALL -> state.getValue(FACING);
        };
    }

    // ---------- 红石输出（类似按钮/刷卡机：on 时全向弱信号 + 连接方向强充能） ----------
    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) && getConnectedDirection(state) == direction ? 15 : 0;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // 仅当方块被真正破坏/替换为其他方块时（POWERED 状态切换等属性变化不触发）：
        // 刷新邻居红石 + 中断正在破解本方块的玩家
        if (!isMoving && !state.is(newState.getBlock())) {
            if (state.getValue(POWERED)) {
                updateNeighbours(state, level, pos);
            }
            PasswordCrackManager.onBlockRemoved(level, pos);
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}