package com.cc_rc.block.Console_button;

import com.cc_rc.block.console_panel.ConsolePanelBlock;
import com.cc_rc.block.console_panel.ConsolePanelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 控制台按钮方块
 * 继承 ConsolePanelBlock 复用放置逻辑、碰撞箱和文字显示
 * 参考原版 ButtonBlock 实现按钮按下/弹起功能（石质按钮，20tick弹起）
 * POWERED=true 时模型+贴图切换为on，false时切换为off
 */
public class ConsoleButtonBlock extends ConsolePanelBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    protected static final int BUTTON_TICKS = 20; // 石质按钮弹起时间 20 tick (1秒)

    public ConsoleButtonBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE, POWERED);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // 手持染料右键 → 优先染色面板文字（不影响按钮按下功能）
        if (ConsolePanelBlock.handleDyeInteraction(level, pos, player, hand)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (state.getValue(POWERED)) {
            return InteractionResult.CONSUME;
        }
        this.press(state, level, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void press(BlockState state, Level level, BlockPos pos) {
        BlockState next = state.setValue(POWERED, true);
        level.setBlock(pos, next, 3);
        this.updateNeighbours(next, level, pos);
        level.scheduleTick(pos, this, BUTTON_TICKS);
        level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3F, 0.6F);
        if (level.getBlockEntity(pos) instanceof ConsolePanelBlockEntity be) {
            be.setChanged();
        }
    }

    private void release(BlockState state, Level level, BlockPos pos) {
        BlockState next = state.setValue(POWERED, false);
        level.setBlock(pos, next, 3);
        this.updateNeighbours(next, level, pos);
        level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 0.3F, 0.5F);
        if (level.getBlockEntity(pos) instanceof ConsolePanelBlockEntity be) {
            be.setChanged();
        }
    }

    private void updateNeighbours(BlockState state, Level level, BlockPos pos) {
        // LeverBlock字节码金标准：第二次刷新 pos.relative(getConnectedDirection(state).getOpposite())
        Direction dir = getConnectedDirection(state);
        level.updateNeighborsAt(pos, this);
        level.updateNeighborsAt(pos.relative(dir.getOpposite()), this);
    }

    /**
     * 原版 FaceAttachedHorizontalDirectionalBlock#getConnectedDirection 字节级真值表：
     * FLOOR   → UP
     * CEILING → DOWN
     * WALL    → FACING 本身
     */
    public static Direction getConnectedDirection(BlockState state) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> Direction.UP;
            case CEILING -> Direction.DOWN;
            case WALL -> state.getValue(FACING);
        };
    }

    // 该方块不进入随机 tick 池（isRandomlyTicking 保持默认 false），
    // 弹起只由 press() 中的 scheduleTick(pos, this, BUTTON_TICKS) 确定性触发，
    // 避免随机 tick 干扰 20 tick 的固定按压时长。
    @Override
    public void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, RandomSource random) {
        // 仅由计划 tick 触发（随机 tick 不会调用本方法）
        if (state.getValue(POWERED)) {
            this.release(state, level, pos);
        }
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // 原版ButtonBlock：弱信号全方向=15（侧邻红石线直接亮）
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) && getConnectedDirection(state) == direction ? 15 : 0;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!isMoving && !state.is(newState.getBlock())) {
            if (state.getValue(POWERED)) {
                this.updateNeighbours(state, level, pos);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
