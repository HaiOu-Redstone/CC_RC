package com.cc_rc.block.console;

import com.cc_rc.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 斜面控制台方块实体。
 *
 * 只保存「视觉 / 交互映射」所需的三条信息：
 *  - 所属总线（结构数据仍保存在总线里，这里不复制组件/文字）；
 *  - 被覆盖的宿主控制柜方块坐标；
 *  - 宿主被覆盖的那个面（竖直侧面，或顶面）。
 *
 * 成型时由总线扫描登记；斜面被拆除 / 结构失效时清空。
 */
public class ConsoleSlopeBlockEntity extends BlockEntity {

    /** 所属总线坐标（未登记时为 null）。 */
    private BlockPos busPos;
    /** 被覆盖的宿主控制柜方块坐标。 */
    private BlockPos hostPos;
    /** 宿主被覆盖的面。 */
    private Direction hostFace;

    public ConsoleSlopeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONSOLE_SLOPE_BE.get(), pos, state);
    }

    @Nullable
    public BlockPos getBusPos() {
        return this.busPos;
    }

    @Nullable
    public BlockPos getHostPos() {
        return this.hostPos;
    }

    @Nullable
    public Direction getHostFace() {
        return this.hostFace;
    }

    /** 由总线在成型 / 失效时调用：写入映射信息并同步客户端。 */
    public void setLink(@Nullable BlockPos busPos, @Nullable BlockPos hostPos,
                        @Nullable Direction hostFace) {
        if (java.util.Objects.equals(this.busPos, busPos)
                && java.util.Objects.equals(this.hostPos, hostPos)
                && this.hostFace == hostFace) {
            return;
        }
        this.busPos = busPos == null ? null : busPos.immutable();
        this.hostPos = hostPos == null ? null : hostPos.immutable();
        this.hostFace = hostFace;
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
        if (this.hostPos != null) {
            tag.putLong("Host", this.hostPos.asLong());
        }
        if (this.hostFace != null) {
            tag.putInt("HostFace", this.hostFace.get3DDataValue());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.busPos = tag.contains("Bus") ? BlockPos.of(tag.getLong("Bus")) : null;
        this.hostPos = tag.contains("Host") ? BlockPos.of(tag.getLong("Host")) : null;
        this.hostFace = tag.contains("HostFace")
                ? Direction.from3DDataValue(tag.getInt("HostFace")) : null;
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
