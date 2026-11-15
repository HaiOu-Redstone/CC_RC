package com.cc_rc.block.console;

import com.cc_rc.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 控制台结构方块实体。
 *
 * 只保存「所属总线坐标 + 结构状态」，用于：
 *  - 方块被拆时快速找到总线并解除成型；
 *  - 客户端 BER 在「部分成型」状态下绘制表面网格，辅助按像素摆放组件。
 *
 * 真正的结构数据（最小角/尺寸/组件）都在总线方块实体里。
 */
public class ConsoleBlockEntity extends BlockEntity {

    /** 所属总线坐标（未关联时为 null）。 */
    private BlockPos busPos;
    /** 结构状态：0 未成型 / 1 部分成型 / 2 完全成型。 */
    private int structureState;

    public ConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONSOLE_BLOCK_BE.get(), pos, state);
    }

    public BlockPos getBusPos() {
        return this.busPos;
    }

    public int getStructureState() {
        return this.structureState;
    }

    /** 由总线在成型/锁定/失效时调用，写入关联信息并同步客户端。 */
    public void setLink(BlockPos busPos, int structureState) {
        if (java.util.Objects.equals(this.busPos, busPos) && this.structureState == structureState) {
            return;
        }
        this.busPos = busPos == null ? null : busPos.immutable();
        this.structureState = structureState;
        this.setChanged();
        if (this.level != null) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.busPos != null) {
            tag.putLong("Bus", this.busPos.asLong());
        }
        tag.putInt("StructureState", this.structureState);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.busPos = tag.contains("Bus") ? BlockPos.of(tag.getLong("Bus")) : null;
        this.structureState = tag.getInt("StructureState");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
