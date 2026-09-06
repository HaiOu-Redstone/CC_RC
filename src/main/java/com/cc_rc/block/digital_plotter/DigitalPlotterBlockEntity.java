package com.cc_rc.block.digital_plotter;

import com.cc_rc.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
 */
public class DigitalPlotterBlockEntity extends BlockEntity {
    public static final int DATA_LENGTH = 50;
    public static final int MIN_VALUE = 0;
    public static final int MAX_VALUE = 100;

    private int[] data = new int[DATA_LENGTH];
    private Component text = Component.empty();

    public DigitalPlotterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIGITAL_PLOTTER_BE.get(), pos, state);
    }

    /** 读取整个列表（供渲染器使用） */
    public int[] getData() {
        return data;
    }

    /** 写入一个新值：删除最后一位，其他值后移，新值写入首位（钳制0~100） */
    public void push(int value) {
        value = Math.max(MIN_VALUE, Math.min(MAX_VALUE, value));
        System.arraycopy(data, 0, data, 1, DATA_LENGTH - 1);
        data[0] = value;
        sync();
    }

    /** 设置指定位置的值（钳制0~100），索引越界不写入，返回是否成功 */
    public boolean setValue(int index, int value) {
        if (index < 0 || index >= DATA_LENGTH) {
            return false;
        }
        data[index] = Math.max(MIN_VALUE, Math.min(MAX_VALUE, value));
        sync();
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
        sync();
    }

    public Component getText() {
        return text;
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
