package com.cc_rc.block.Safe_button;

import com.cc_rc.Config;
import com.cc_rc.block.console_panel.ConsolePanelBlock;
import com.cc_rc.block.console_panel.ConsolePanelBlockEntity;
import com.cc_rc.block.console_panel.TimerExpiredHandler;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerLevel;

/**
 * 安全按钮方块
 * 继承 ConsolePanelBlock 复用放置逻辑、碰撞箱和文字显示
 * 实现 TimerExpiredHandler：倒计时归零由本方块自身的 tick() 心跳链触发并回调 onTimerExpired()
 * 3个状态：
 *   状态1（初始）：无信号，潜行+右键→状态2（活板门开）
 *   状态2：无信号，右键→状态3（按钮按下），潜行+右键→状态1（活板门关）
 *         200tick无操作→自动变回状态1（活板门关）
 *   状态3：信号15，20tick后→状态2（按钮弹起），重新计时200tick
 */
public class SafeButtonBlock extends ConsolePanelBlock implements TimerExpiredHandler {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 1, 3);

    public SafeButtonBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(STAGE, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE, STAGE);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        int stage = state.getValue(STAGE);
        boolean sneaking = player.isShiftKeyDown();

        if (sneaking) {
            if (stage == 1) {
                transitionToStage2(state, level, pos);
            } else if (stage == 2) {
                transitionToStage1(state, level, pos);
            }
        } else {
            if (stage == 2) {
                transitionToStage3(state, level, pos);
            }
        }
        return InteractionResult.CONSUME;
    }

    private void transitionToStage1(BlockState state, Level level, BlockPos pos) {
        BlockState next = state.setValue(STAGE, 1);
        level.setBlock(pos, next, 3);
        level.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.3F, 0.7F);
        updateNeighbours(next, level, pos);
        if (level.getBlockEntity(pos) instanceof ConsolePanelBlockEntity be) {
            be.setTicksRemaining(0);
        }
    }

    private void transitionToStage2(BlockState state, Level level, BlockPos pos) {
        BlockState next = state.setValue(STAGE, 2);
        level.setBlock(pos, next, 3);
        level.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.3F, 0.7F);
        updateNeighbours(next, level, pos);
        setTimer(level, pos, Config.getTimeoutTicks());
    }

    private void transitionToStage3(BlockState state, Level level, BlockPos pos) {
        BlockState next = state.setValue(STAGE, 3);
        level.setBlock(pos, next, 3);
        level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3F, 0.6F);
        updateNeighbours(next, level, pos);
        setTimer(level, pos, Config.getSignalTicks());
    }

    /**
     * 设置倒计时并通过1-tick心跳调度实现
     * 每tick调用block.tick()，在tick中递减计数器
     */
    private void setTimer(Level level, BlockPos pos, int ticks) {
        if (level.getBlockEntity(pos) instanceof ConsolePanelBlockEntity be) {
            be.setTicksRemaining(ticks);
        }
        level.scheduleTick(pos, this, 1);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isClientSide) return;
        if (!(level.getBlockEntity(pos) instanceof ConsolePanelBlockEntity be)) return;

        int remaining = be.getTicksRemaining();
        if (remaining <= 0) return;

        remaining--;
        if (remaining > 0) {
            be.setTicksRemaining(remaining);
            level.scheduleTick(pos, this, 1);
        } else {
            be.setTicksRemaining(0);
            onTimerExpired(state, level, pos);
        }
    }

    @Override
    public void onTimerExpired(BlockState state, Level level, BlockPos pos) {
        int stage = state.getValue(STAGE);
        if (stage == 2) {
            transitionToStage1(state, level, pos);
        } else if (stage == 3) {
            BlockState next = state.setValue(STAGE, 2);
            level.setBlock(pos, next, 3);
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 0.3F, 0.5F);
            updateNeighbours(next, level, pos);
            setTimer(level, pos, Config.getTimeoutTicks());
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

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // 原版ButtonBlock：弱信号全方向=15（侧邻红石线直接亮）
        return state.getValue(STAGE) == 3 ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(STAGE) == 3 && getConnectedDirection(state) == direction ? 15 : 0;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!isMoving && !state.is(newState.getBlock())) {
            if (state.getValue(STAGE) == 3) {
                updateNeighbours(state, level, pos);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
