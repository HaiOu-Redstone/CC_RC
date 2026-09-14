package com.cc_rc.block.console_panel;

import com.cc_rc.ModBlockEntities;
import com.cc_rc.block.ITextDisplay;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 控制面板方块实体基类
 * 存储和管理方块上显示的文本，通过 NBT 持久化，支持网络同步
 * 支持 ticksRemaining 倒计时功能（安全按钮使用）
 */
public class ConsolePanelBlockEntity extends BlockEntity implements ITextDisplay {
    private Component text = Component.empty();
    private int ticksRemaining = 0;

    /** 用于 console_panel 自身 */
    public ConsolePanelBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.CONSOLE_PANEL_BE.get(), pos, state);
    }

    /** 用于子类传入自己的 BlockEntityType */
    protected ConsolePanelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setText(Component text) {
        this.text = text;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public Component getText() {
        return text;
    }

    public int getTicksRemaining() {
        return ticksRemaining;
    }

    public void setTicksRemaining(int ticks) {
        this.ticksRemaining = ticks;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!text.getString().isEmpty()) {
            tag.putString("Text", Component.Serializer.toJson(text));
        }
        if (ticksRemaining > 0) {
            tag.putInt("TicksRemaining", ticksRemaining);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Text")) {
            text = Component.Serializer.fromJson(tag.getString("Text"));
        }
        ticksRemaining = tag.contains("TicksRemaining") ? tag.getInt("TicksRemaining") : 0;
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
