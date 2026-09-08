package com.cc_rc.block.key_distributor;

import com.cc_rc.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 钥匙分发控制器方块实体
 * 存储已录入的钥匙柜记录列表（KeyCabinetRecord：坐标 + 朝向），通过 NBT 持久化。
 * 数据主要由服务端红石逻辑（KeyDistributorBlock）与 /ccrc keycabinet 指令读写。
 */
public class KeyDistributorBlockEntity extends BlockEntity {

    private final List<KeyCabinetRecord> records = new ArrayList<>();
    // 上一 tick 的红石信号强度（用于上升沿检测：脉冲只触发一次分发）
    private int lastSignal = 0;

    public KeyDistributorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KEY_DISTRIBUTOR_BE.get(), pos, state);
    }

    /**
     * 每 tick 心跳（由 KeyDistributorBlock.tick 调度驱动）：
     * 信号从 ≤5 跳变到 >5（上升沿）→ 只分发一次钥匙1/钥匙2；
     * 信号 ≤5 期间 → 持续校验记录，位置不再是钥匙柜则删除。
     */
    public void onTick() {
        if (level == null || level.isClientSide) return;
        if (!(getBlockState().getBlock() instanceof KeyDistributorBlock block)) return;

        int signal = level.getBestNeighborSignal(worldPosition);
        int last = this.lastSignal;
        this.lastSignal = signal;

        if (signal > KeyDistributorBlock.TRIGGER_STRENGTH && last <= KeyDistributorBlock.TRIGGER_STRENGTH) {
            block.distributeKeys(level, this);
        } else if (signal <= KeyDistributorBlock.TRIGGER_STRENGTH) {
            block.validateRecords(level, this);
        }
    }

    /** 添加记录（重复坐标不添加），返回是否新增 */
    public boolean addRecord(KeyCabinetRecord record) {
        for (KeyCabinetRecord r : records) {
            if (r.pos().equals(record.pos())) {
                return false;
            }
        }
        records.add(record);
        setChanged();
        return true;
    }

    /** 按坐标删除记录，返回是否删除成功 */
    public boolean removeRecord(BlockPos pos) {
        boolean removed = records.removeIf(r -> r.pos().equals(pos));
        if (removed) {
            setChanged();
        }
        return removed;
    }

    /** 当前记录数 */
    public int size() {
        return records.size();
    }

    /** 只读快照 */
    public List<KeyCabinetRecord> getRecords() {
        return List.copyOf(records);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Records", KeyCabinetRecord.saveList(records));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        records.clear();
        if (tag.contains("Records")) {
            records.addAll(KeyCabinetRecord.loadList(tag.getCompound("Records")));
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