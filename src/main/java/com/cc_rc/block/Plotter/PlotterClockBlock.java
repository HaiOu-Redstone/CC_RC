package com.cc_rc.block.Plotter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/**
 * 圆盘记录仪时钟方块
 * 完整方块，六面相同贴图，不可交互
 * 当接收到脉冲上升沿（无信号→有信号）时，遍历上方 1~8 格，
 * 对每个模式为 1 的圆盘记录仪触发一次 doSample() 划线
 */
public class PlotterClockBlock extends Block implements EntityBlock {

    public PlotterClockBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlotterClockBlockEntity(pos, state);
    }

    /**
     * 本方块不主动输出红石信号（返回 false，getSignal 恒为 0）。
     * 被动接收逻辑见 getSignal 与 neighborChanged：靠邻居更新触发 BE 做上升沿检测。
     */
    @Override
    public boolean isSignalSource(BlockState state) {
        return false;
    }

    /**
     * 检测红石信号变化：邻居更新时让 BE 做上升沿检测
     */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PlotterClockBlockEntity cbe) {
            cbe.onNeighborChanged();
        }
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        return 0;
    }
}
