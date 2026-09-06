package com.cc_rc.block.Redstone_transmit;

import com.cc_rc.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * 红石信号发射器（redstone_sender）
 *
 * 功能：
 *   - 完整方块，可朝 x±/y±/z± 六个方向放置（FACING）
 *   - 会连接红石线（canConnectRedstone=true）
 *   - 受到外部红石信号更新时（neighborChanged），记录当前接收到的信号强度（POWER 0~15），
 *     并沿 FACING 方向 2~配置距离 遍历寻找红石信号接收器，将接收器信号强度设置为相同值
 *   - POWER > 0 显示 on 贴图，POWER 由有变 0 显示 off 贴图（由 blockstate 模型切换实现）
 */
public class RedstoneSenderBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    public RedstoneSenderBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWER);
    }

    /** 放置时朝向玩家视线反方向（面向玩家）；支持上下左右前后共六个方向 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    /** 可被红石线连接（红石线会延伸到此方块） */
    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return true;
    }

    /** 红石线/邻居更新时刷新自身信号并转发给接收器 */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            refreshSignal(state, level, pos);
        }
    }

    /** 放置时（且不是被同种方块替换）立即刷新一次信号 */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            refreshSignal(state, level, pos);
        }
    }

    /**
     * 读取发射器自身收到的信号强度；若与当前存储不同则更新自身 POWER，
     * 并沿 FACING 方向把信号强度转发给范围内所有接收器
     */
    private void refreshSignal(BlockState state, Level level, BlockPos pos) {
        int signal = level.getBestNeighborSignal(pos);
        int current = state.getValue(POWER);
        if (signal != current) {
            level.setBlock(pos, state.setValue(POWER, signal), 2);
            transmitToReceivers(level, pos, state.getValue(FACING), signal);
        }
    }

    /**
     * 沿 facing 方向从距离 2（不含相邻方块）到配置最大距离遍历，
     * 找到红石信号接收器则把其 POWER 设置为与自身相同
     */
    private void transmitToReceivers(Level level, BlockPos pos, Direction facing, int signal) {
        int range = Config.getRedstoneTransmitRange();
        for (int distance = 2; distance <= range; distance++) {
            BlockPos target = pos.relative(facing, distance);
            if (!level.getWorldBorder().isWithinBounds(target)) {
                break;
            }
            BlockState targetState = level.getBlockState(target);
            if (targetState.getBlock() instanceof RedstoneReceiverBlock) {
                int targetPower = targetState.getValue(RedstoneReceiverBlock.POWER);
                if (targetPower != signal) {
                    level.setBlock(target, targetState.setValue(RedstoneReceiverBlock.POWER, signal), 3);
                }
            }
        }
    }
}