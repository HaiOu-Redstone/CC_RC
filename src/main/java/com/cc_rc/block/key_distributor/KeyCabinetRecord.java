package com.cc_rc.block.key_distributor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

/**
 * 钥匙柜记录：坐标 + 朝向。
 * 由多功能工具在钥匙柜上右键记录，再录入到钥匙分发控制器（KeyDistributorBlockEntity）。
 * 提供 NBT 序列化/反序列化（列表整体存取）。
 */
public record KeyCabinetRecord(BlockPos pos, Direction facing) {

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("X", pos.getX());
        tag.putInt("Y", pos.getY());
        tag.putInt("Z", pos.getZ());
        tag.putString("Facing", facing.getName());
        return tag;
    }

    public static KeyCabinetRecord load(CompoundTag tag) {
        BlockPos pos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
        Direction facing = Direction.byName(tag.getString("Facing"));
        if (facing == null) facing = Direction.NORTH;
        return new KeyCabinetRecord(pos, facing);
    }

    /** 序列化整个记录列表 */
    public static CompoundTag saveList(List<KeyCabinetRecord> records) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Count", records.size());
        for (int i = 0; i < records.size(); i++) {
            tag.put("Rec" + i, records.get(i).save());
        }
        return tag;
    }

    /** 反序列化整个记录列表 */
    public static List<KeyCabinetRecord> loadList(CompoundTag tag) {
        List<KeyCabinetRecord> records = new ArrayList<>();
        int count = tag.getInt("Count");
        for (int i = 0; i < count; i++) {
            records.add(load(tag.getCompound("Rec" + i)));
        }
        return records;
    }
}