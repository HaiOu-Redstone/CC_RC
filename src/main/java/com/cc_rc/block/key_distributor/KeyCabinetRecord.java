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

    /** 记录列表上限：存档里的 Count 完全不可信，必须有硬上限兜住内存。 */
    public static final int MAX_RECORDS = 256;

    /** 反序列化整个记录列表 */
    public static List<KeyCabinetRecord> loadList(CompoundTag tag) {
        List<KeyCabinetRecord> records = new ArrayList<>();
        // Count 来自存档，没有任何一致性校验：手工改档 / 区块损坏 / 其它模组误写
        // 都可能给出 Integer.MAX_VALUE，原样循环会把 Rec0..RecN 的空 Compound 全塞进
        // 列表直到 OOM（getCompound 缺失时返回空 tag，load 不会报错）
        int count = Math.min(tag.getInt("Count"), MAX_RECORDS);
        for (int i = 0; i < count; i++) {
            String key = "Rec" + i;
            if (!tag.contains(key)) {
                // 编号不连续说明数据已经损坏，后面的记录不再可信
                break;
            }
            records.add(load(tag.getCompound(key)));
        }
        return records;
    }
}