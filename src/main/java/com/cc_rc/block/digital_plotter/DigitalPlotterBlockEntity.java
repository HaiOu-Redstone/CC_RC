package com.cc_rc.block.digital_plotter;

import com.cc_rc.ModBlockEntities;
import com.cc_rc.block.ITextDisplay;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 数字圆盘记录仪方块实体
 *
 * 维护长度50的int列表（值0~100，越界自动钳制），无状态（不自动采样，无红石触发）。
 * 另存储命名文字（text，白色，来自放置时物品的自定义名称，默认空不显示）。
 * 通过 NBT 持久化，支持网络同步。
 *
 * 操作接口：
 *   - push(value)    ：写入一个新值，自动删除最后一位并移位（钳制0~100）
 *   - setValue(i, v) ：手动设置指定位置的值（钳制0~100），索引越界不写入，返回是否成功
 *   - getValue(i)    ：读取指定位置的值（0~100），索引越界返回 null
 *   - getData()      ：读取整个列表（供渲染器使用）
 *
 * 线程模型（CC 外设无延迟改造）：data 为 volatile 引用，写操作采用
 * 「写时复制」——新建数组副本并原子替换引用，**永不修改共享数组**，
 * 读方（Lua/渲染/存档）拿到的是不可变快照，无需加锁；写后只置 dirty，
 * 由主线程 tick 节流广播（每 tick 至多一次）。
 */
public class DigitalPlotterBlockEntity extends BlockEntity implements ITextDisplay {
    public static final int DATA_LENGTH = 50;
    public static final int MIN_VALUE = 0;
    public static final int MAX_VALUE = 100;

    /** 数据快照（volatile 引用：写时复制，读方拿到不可变快照） */
    private volatile int[] data = new int[DATA_LENGTH];
    private Component text = Component.empty();
    /** 待广播标记（volatile：Lua 线程置位，主线程 tick 消费并清除） */
    private volatile boolean dirty = false;

    public DigitalPlotterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIGITAL_PLOTTER_BE.get(), pos, state);
    }

    /** 读取整个列表（供渲染器使用；返回当前不可变快照） */
    public int[] getData() {
        return data;
    }

    /** 写入一个新值：删除最后一位，其他值后移，新值写入首位（钳制0~100，写时复制） */
    public int push(int value) {
        value = Math.max(MIN_VALUE, Math.min(MAX_VALUE, value));
        int[] copy = Arrays.copyOf(data, DATA_LENGTH);
        System.arraycopy(copy, 0, copy, 1, DATA_LENGTH - 1);
        copy[0] = value;
        data = copy;
        dirty = true;
        return value;
    }

    /** 设置指定位置的值（钳制0~100，写时复制），索引越界不写入，返回是否成功 */
    public boolean setValue(int index, int value) {
        if (index < 0 || index >= DATA_LENGTH) {
            return false;
        }
        int[] copy = Arrays.copyOf(data, DATA_LENGTH);
        copy[index] = Math.max(MIN_VALUE, Math.min(MAX_VALUE, value));
        data = copy;
        dirty = true;
        return true;
    }

    /** 读取指定位置的值（0~100），索引越界返回 null */
    @Nullable
    public Integer getValue(int index) {
        if (index < 0 || index >= DATA_LENGTH) {
            return null;
        }
        return data[index];
    }

    /** 命名文字（白色，默认空不显示） */
    public void setText(Component text) {
        this.text = text == null ? Component.empty() : text;
        dirty = true;
    }

    public Component getText() {
        return text;
    }

    /**
     * 主线程 tick：dirty 时合并广播——每 tick 至多一次「标记存档 + 客户端同步」。
     * 写序约束：数组引用替换在前、dirty=true 在后（volatile happens-before）。
     */
    public static void tick(Level level, BlockPos pos, BlockState state, DigitalPlotterBlockEntity be) {
        if (be.dirty) {
            be.dirty = false;
            be.setChanged();
            if (!level.isClientSide) {
                level.sendBlockUpdated(pos, state, state, 3);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putIntArray("PlotterData", data);
        if (!text.getString().isEmpty()) {
            tag.putString("Text", Component.Serializer.toJson(text));
        }
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
        if (tag.contains("Text")) {
            text = Component.Serializer.fromJson(tag.getString("Text"));
        } else {
            text = Component.empty();
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
        }
    }
}