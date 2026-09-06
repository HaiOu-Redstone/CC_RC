package com.cc_rc.block.Redstone_transmit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * 红石信号接收器（redstone_receiver）
 *
 * 功能：
 *   - 无方向完整方块（POWER 0~15 状态）
 *   - 会连接红石线（canConnectRedstone=true）
 *   - 向相邻方块发出当前信号强度的红石信号（getSignal 返回 POWER）
 *   - POWER > 0 显示 on 贴图，POWER 为 0 显示 off 贴图（由 blockstate 模型切换实现）
 *   - 自身不主动探测信号，由红石信号发射器沿面向方向扫描并直接写入 POWER
 */
public class RedstoneReceiverBlock extends Block {
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    public RedstoneReceiverBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER);
    }

    /** 可被红石线连接（红石线会延伸到此方块） */
    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return true;
    }

    /** 是红石信号源：向相邻方块输出 POWER 强度的信号（方向无关） */
    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWER);
    }
}