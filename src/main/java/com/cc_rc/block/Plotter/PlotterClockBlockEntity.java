package com.cc_rc.block.Plotter;

import com.cc_rc.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 圆盘记录仪时钟方块实体
 * 存储上一次的红石信号强度，用于检测脉冲（0→非0）上升沿
 * 上升沿触发时，向上扫描 1~8 格内的 模式1 圆盘记录仪并触发一次 doSample()
 */
public class PlotterClockBlockEntity extends BlockEntity {
    private int lastSignal = 0;

    public PlotterClockBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.PLOTTER_CLOCK_BE.get(), pos, state);
    }

    protected PlotterClockBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public int getLastSignal() {
        return lastSignal;
    }

    public void setLastSignal(int signal) {
        this.lastSignal = signal;
        setChanged();
    }

    /**
     * 信号邻居变化时调用。检测上升沿（无信号→有信号）并触发扫描。
     */
    public void onNeighborChanged() {
        if (level == null || level.isClientSide) return;
        int cur = level.getBestNeighborSignal(worldPosition);
        // 上升沿：上次信号为 0 且本次信号 > 0
        if (lastSignal == 0 && cur > 0) {
            triggerAbove();
        }
        setLastSignal(cur);
    }

    private void triggerAbove() {
        Level l = level;
        BlockPos base = worldPosition;
        // 遍历上方 1~8 格
        for (int dy = 1; dy <= 8; dy++) {
            BlockPos p = base.above(dy);
            BlockEntity be = l.getBlockEntity(p);
            if (be instanceof PlotterBlockEntity pbe && pbe.getMode() == 1) {
                pbe.doSample();
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("LastSignal", lastSignal);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        lastSignal = tag.contains("LastSignal") ? tag.getInt("LastSignal") : 0;
    }
}
