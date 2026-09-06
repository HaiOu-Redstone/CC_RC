package com.cc_rc.block.digital_knob;

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
 * 数字调节器方块实体
 *
 * 存储可显示整数（0~1000），默认0。
 * 另存储命名文字（text，白色，来自放置时物品的自定义名称，默认空不显示）。
 * 通过 NBT 持久化，支持网络同步。
 * 提供 adjustValue(int button) 按钮调整（-20,-1,+1,+20）及范围钳制。
 */
public class DigitalKnobBlockEntity extends BlockEntity {
    public static final int MIN_VALUE = 0;
    public static final int MAX_VALUE = 1000;
    public static final int DEFAULT_VALUE = 0;

    // 四个按钮对应的调整幅度：按钮0(-20)、1(-1)、2(+1)、3(+20)
    private static final int[] BUTTON_DELTAS = {-20, -1, +1, +20};

    private int value = DEFAULT_VALUE;
    private Component text = Component.empty();
    // 百分比模式默认开启：仅改变显示方式（value/10 + 百分号），实际存储值不变
    private boolean percentMode = true;

    public DigitalKnobBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIGITAL_KNOB_BE.get(), pos, state);
    }

    /** 读取当前值 */
    public int getValue() {
        return value;
    }

    /** 钳制范围后写入整数值 */
    public void setValue(int value) {
        this.value = Math.max(MIN_VALUE, Math.min(MAX_VALUE, value));
        sync();
    }

    /** 按钮调整：0~3 对应 -20,-1,+1,+20，钳制到 0~1000 */
    public void adjustValue(int button) {
        if (button >= 0 && button < BUTTON_DELTAS.length) {
            setValue(this.value + BUTTON_DELTAS[button]);
        }
    }

    /** 命名文字（白色，默认空不显示） */
    public void setText(Component text) {
        this.text = text == null ? Component.empty() : text;
        sync();
    }

    public Component getText() {
        return text;
    }

    /** 是否开启百分比模式 */
    public boolean isPercentMode() {
        return percentMode;
    }

    /** 设置百分比模式开关，返回新状态 */
    public boolean setPercentMode(boolean mode) {
        this.percentMode = mode;
        sync();
        return percentMode;
    }

    /** 切换百分比模式开关，返回新状态 */
    public boolean togglePercentMode() {
        return setPercentMode(!percentMode);
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
        tag.putInt("Value", value);
        tag.putBoolean("PercentMode", percentMode);
        if (!text.getString().isEmpty()) {
            tag.putString("Text", Component.Serializer.toJson(text));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        value = tag.contains("Value") ? tag.getInt("Value") : DEFAULT_VALUE;
        percentMode = tag.contains("PercentMode") ? tag.getBoolean("PercentMode") : true;
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