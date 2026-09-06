package com.cc_rc.block.digital_display;

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
 * 数码显示器方块实体
 *
 * 存储两行待显示文字：
 *   - 第一行（text）：命名文字，白色，默认空（未命名时不显示）
 *   - 第二行（status）：固定文字，橙色（0xf06020），默认 "----"，
 *       预留修改接口 setStatus()，供其他系统写入需要显示的状态文字
 * 通过 NBT 持久化，支持网络同步
 */
public class DigitalDisplayBlockEntity extends BlockEntity {
    public static final String DEFAULT_STATUS = "----";

    private Component text = Component.empty();
    private Component status = Component.literal(DEFAULT_STATUS);

    public DigitalDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIGITAL_DISPLAY_BE.get(), pos, state);
    }

    /** 第一行：命名文字（白色） */
    public void setText(Component text) {
        this.text = text == null ? Component.empty() : text;
        sync();
    }

    public Component getText() {
        return text;
    }

    /** 第二行：固定文字（橙色），预留修改接口 */
    public void setStatus(Component status) {
        this.status = status == null ? Component.literal(DEFAULT_STATUS) : status;
        sync();
    }

    public Component getStatus() {
        return status;
    }

    public void resetStatus() {
        setStatus(Component.literal(DEFAULT_STATUS));
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