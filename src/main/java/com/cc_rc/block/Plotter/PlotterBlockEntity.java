package com.cc_rc.block.Plotter;

import com.cc_rc.Config;
import com.cc_rc.ModBlockEntities;
import com.cc_rc.block.console_panel.ConsolePanelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 圆盘记录仪方块实体
 * 维护长度24的int列表（值0-15），每N tick右移并读取红石信号写入首位
 * 模式1：被动触发（不自动运行）
 * 模式2~9：tick间隔从配置中读取（默认5/10/20/50/100/200/500/1000）
 */
public class PlotterBlockEntity extends ConsolePanelBlockEntity {
    private static final int DATA_LENGTH = 24;

    private int[] data = new int[DATA_LENGTH];
    private int mode = 5; // 1~9，默认5（间隔20tick，与旧版一致）
    private int tickCounter = Config.getPlotterTickInterval(5);

    public PlotterBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.PLOTTER_BE.get(), pos, state);
    }

    protected PlotterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public int[] getData() {
        return data;
    }

    public int getMode() {
        return mode;
    }

    /**
     * 设置模式（1~9），并重置计数器
     * 返回新的模式
     */
    public int setMode(int newMode) {
        if (newMode < 1) newMode = 1;
        if (newMode > 9) newMode = 9;
        this.mode = newMode;
        this.tickCounter = Config.getPlotterTickInterval(mode);
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return mode;
    }

    /**
     * 下一个模式：1->2->...->9->1
     * 返回新模式
     */
    public int nextMode() {
        int next = mode == 9 ? 1 : mode + 1;
        return setMode(next);
    }

    /**
     * 回到模式1
     * 返回新模式
     */
    public int resetMode() {
        return setMode(1);
    }

    /**
     * 每 1 tick 调用一次，负责按配置间隔更新数据
     */
    public void tick() {
        int interval = Config.getPlotterTickInterval(mode);
        if (interval <= 0) {
            // 被动触发模式：不自动运行
            return;
        }
        tickCounter--;
        if (tickCounter <= 0) {
            tickCounter = interval;
            doSample();
        }
    }

    /**
     * 主动采样接口（预留，供被动触发调用）
     */
    public void doSample() {
        // 删除列表最后一位，其他值后移
        System.arraycopy(data, 0, data, 1, DATA_LENGTH - 1);
        // 读取红石信号写入首位
        if (level != null) {
            data[0] = level.getBestNeighborSignal(worldPosition);
        } else {
            data[0] = 0;
        }
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putIntArray("PlotterData", data);
        tag.putInt("PlotterMode", mode);
        tag.putInt("TickCounter", tickCounter);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("PlotterData")) {
            int[] saved = tag.getIntArray("PlotterData");
            if (saved.length == DATA_LENGTH) {
                data = saved;
            }
        }
        if (tag.contains("PlotterMode")) {
            int m = tag.getInt("PlotterMode");
            mode = (m >= 1 && m <= 9) ? m : 1;
        }
        tickCounter = tag.contains("TickCounter") ? tag.getInt("TickCounter")
                : Config.getPlotterTickInterval(mode);
    }
}
