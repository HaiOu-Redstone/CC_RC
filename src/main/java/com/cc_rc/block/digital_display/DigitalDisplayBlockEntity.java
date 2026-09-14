package com.cc_rc.block.digital_display;

import com.cc_rc.ModBlockEntities;
import com.cc_rc.block.ITextDisplay;
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
 * 数码显示器方块实体
 *
 * 存储两行待显示文字：
 *   - 第一行（text）：命名文字，白色，默认空（未命名时不显示）
 *   - 第二行（status）：固定文字，橙色（0xf06020），默认 "----"，
 *       预留修改接口 setStatus()，供其他系统写入需要显示的状态文字
 * 通过 NBT 持久化，支持网络同步
 *
 * 线程模型（CC 外设无延迟改造）：text/status 均为 volatile 引用，
 * Lua 线程可直读直写（0 tick）；写方法只置 dirty，由主线程 tick
 * 节流广播（每 tick 至多一次）。
 */
public class DigitalDisplayBlockEntity extends BlockEntity implements ITextDisplay {
    public static final String DEFAULT_STATUS = "----";

    private volatile Component text = Component.empty();
    private volatile Component status = Component.literal(DEFAULT_STATUS);
    /** 待广播标记（volatile：Lua 线程置位，主线程 tick 消费并清除） */
    private volatile boolean dirty = false;

    public DigitalDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIGITAL_DISPLAY_BE.get(), pos, state);
    }

    /** 第一行：命名文字（白色） */
    public void setText(Component text) {
        this.text = text == null ? Component.empty() : text;
        dirty = true;
    }

    public Component getText() {
        return text;
    }

    /** 第二行：固定文字（橙色），预留修改接口 */
    public void setStatus(Component status) {
        this.status = status == null ? Component.literal(DEFAULT_STATUS) : status;
        dirty = true;
    }

    public Component getStatus() {
        return status;
    }

    public void resetStatus() {
        setStatus(Component.literal(DEFAULT_STATUS));
    }

    /**
     * 主线程 tick：dirty 时合并广播——每 tick 至多一次「标记存档 + 客户端同步」。
     * 写序约束：字段写在前、dirty=true 在后（volatile happens-before）。
     */
    public static void tick(Level level, BlockPos pos, BlockState state, DigitalDisplayBlockEntity be) {
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
        if (!text.getString().isEmpty()) {
            tag.putString("Text", Component.Serializer.toJson(text));
        }
        if (!status.getString().isEmpty()) {
            tag.putString("Status", Component.Serializer.toJson(status));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Text")) {
            text = Component.Serializer.fromJson(tag.getString("Text"));
        } else {
            text = Component.empty();
        }
        if (tag.contains("Status")) {
            status = Component.Serializer.fromJson(tag.getString("Status"));
        } else {
            status = Component.literal(DEFAULT_STATUS);
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