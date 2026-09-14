package com.cc_rc.block.extended_relay;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.Nullable;

/**
 * 扩展红石继电器方块。
 *
 * 完整方块，可朝水平四向（x±/z±）放置，正面（front 贴图）朝向放置时玩家面向的方向。
 * 作为「扩展红石继电器总线」的远端红石端口：
 *  - 总线沿自己的朝向在任意距离找到本方块后，通过方块实体读写红石信号；
 *  - 本方块显示的是总线 setOutput/setAnalogOutput 写入的输出（isSignalSource，
 *    getSignal/getDirectSignal 转发方块实体的 outputs）；
 *  - 同时每 tick 刷新从世界读入的信号（inputs），供总线 getInput/getAnalogInput 读取。
 *
 * 红石行为（参考 CC RedstoneRelayBlock 与玻璃）：
 *  - canConnectRedstone=false：红石线不连接本方块、不将其作为导体；
 *  - isRedstoneConductor=false（Properties StatePredicate 设置，1.20.1 无 Block 层
 *    可覆写方法）：本方块视为透明非实心（同玻璃），红石线不从其上/旁穿透传导；
 *  - getSignal/getDirectSignal 返回真实定向输出（direction 反向语义，用
 *    direction.getOpposite() 换算本地方向），保证 setOutput 能向目标方向输出红石。
 */
public class ExtendedRelayBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public ExtendedRelayBlock(Properties properties) {
        // 1.20.1 中 isRedstoneConductor 定义在 BlockBehaviour.Properties（StatePredicate 形式），
        // Block 层已无该可覆写方法；false = 玻璃式非导体（红石线不将其当作导电方块传递信号）
        super(properties.isRedstoneConductor((state, level, pos) -> false));
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** 放置时正面朝玩家面向的水平方向（front 贴图朝玩家）。 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExtendedRelayBlockEntity(pos, state);
    }

    /**
     * 红石线可主动连接本方块（读取定向输出信号）；配合 Properties 的
     * isRedstoneConductor=false 仍保持"非导体"语义（红石线不从本方块穿透传导）。
     */
    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return true;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    /** 弱信号：定向输出该方向对应的 outputs 值（与 CC RedstoneRelayBlock 同构）。 */
    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof ExtendedRelayBlockEntity relay) {
            return relay.getRedstoneOutput(direction);
        }
        return 0;
    }

    /** 强信号：与弱信号一致（CC RedstoneRelayBlock 返回真实定向输出）。 */
    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    /** 每 tick 校验输入输出：应用总线写出的输出变化、刷新从世界读入的输入，并持续自调度。 */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 持续自调度：保证输入每 tick 刷新、输出待应用变化每 tick 合并应用
        level.scheduleTick(pos, this, 1);
        if (level.getBlockEntity(pos) instanceof ExtendedRelayBlockEntity relay) {
            // 无延迟改造：Lua 线程只写内部缓存，这里把变化合并应用到世界（每 tick 至多一次邻居通知）
            relay.applyOutputsToWorld(level, state);
            // 刷新从世界读入的输入（总线 getInput 依赖最新值）
            relay.refreshInputs(level, state);
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            // 放置后开始持续 tick（刷新输入）
            level.scheduleTick(pos, this, 1);
            // 通知邻居刷新（若有输出则生效）
            level.updateNeighborsAt(pos, this);
        }
    }

    /** 邻居变化：该方向输入可能已变，立即刷新（等下一 tick 也行，这里主动刷一次）。 */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                BlockPos neighborPos, boolean isMoving) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ExtendedRelayBlockEntity relay) {
            relay.refreshInputs(level, state);
        }
    }

    /** 方块被移除时刷新邻居（确保输出信号归零）。 */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!isMoving && !state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof ExtendedRelayBlockEntity relay
                    && relay.hasAnyOutput()) {
                level.updateNeighborsAt(pos, this);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}