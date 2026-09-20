package com.cc_rc.block.block_detector;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * 方块探测器 CC: Tweaked 外设
 *
 * 外设类型 "block_detector"。使方块探测器可被 CC 的调制解调器识别为外设，
 * 电脑可通过 Lua **只读**探测其面向（FACING 前方）的方块信息：
 *   - getFacing()           —— 探测方向字符串（north/south/west/east/up/down）
 *   - getBlockInfo()        —— 面向方块信息：坐标 x/y/z、注册名 id、模组来源 mod、
 *                             路径名 name、是否方块实体 isBlockEntity（未加载返回 nil）
 *   - getBlockEntityData()  —— 若目标为方块实体，返回其完整 NBT 数据（类似
 *                             /data get block，递归转换；不可修改），无方块实体返回 nil
 *
 * 所有方法均标注 **mainThread=true**：探测必须实时读取服务端世界
 * （Level/方块状态/方块实体/区块加载），而 Minecraft Level 非线程安全，
 * 只能由主线程访问——这与 CC 外设无延迟改造的约定一致
 * （「Lua 线程严禁访问 Level」）。因此每次调用消耗 1 个主线程 tick，
 * 属必然代价（参考未改造前的继电器总线）。
 */
public class BlockDetectorPeripheral implements IPeripheral {

    private static final String TYPE = "block_detector";

    private final BlockDetectorBlockEntity blockEntity;

    public BlockDetectorPeripheral(BlockDetectorBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /** 面向方块的坐标（FACING 前一格）。 */
    private BlockPos targetPos() {
        Level level = blockEntity.getLevel();
        BlockPos pos = blockEntity.getBlockPos();
        BlockState state = level == null ? null : level.getBlockState(pos);
        Direction facing = state != null && state.getValue(BlockDetectorBlock.FACING) != null
                ? state.getValue(BlockDetectorBlock.FACING) : Direction.UP;
        return pos.relative(facing);
    }

    /** 探测方向字符串（north/south/west/east/up/down）。 */
    @LuaFunction(mainThread = true)
    public final String getFacing() {
        BlockState state = blockEntity.getLevel().getBlockState(blockEntity.getBlockPos());
        return state.getValue(BlockDetectorBlock.FACING).getName();
    }

    /**
     * 面向方块信息（只读）。目标区块未加载时返回 nil。
     *
     * @return 表：{ x, y, z, id = "minecraft:stone", name = "stone", mod = "minecraft", isBlockEntity = false }
     */
    @LuaFunction(mainThread = true)
    @Nullable
    public final Map<String, Object> getBlockInfo() throws LuaException {
        Level level = getServiceLevel();
        BlockPos target = targetPos();
        if (!level.isLoaded(target)) {
            return null;
        }
        BlockState targetState = level.getBlockState(target);
        Block block = targetState.getBlock();
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);

        Map<String, Object> info = new HashMap<>();
        info.put("x", target.getX());
        info.put("y", target.getY());
        info.put("z", target.getZ());
        info.put("id", id == null ? "unknown" : id.toString());
        info.put("name", id == null ? "unknown" : id.getPath());
        info.put("mod", id == null ? "unknown" : id.getNamespace());
        info.put("isBlockEntity", level.getBlockEntity(target) != null);
        return info;
    }

    /**
     * 面向方块实体的完整数据（类似 /data get block，不可修改）。
     * 目标无方块实体、区块未加载或不存在时返回 nil。
     *
     * @return NBT 数据转换成的 Lua 表（含 id/坐标等基础字段）
     */
    @LuaFunction(mainThread = true)
    @Nullable
    public final Map<String, Object> getBlockEntityData() throws LuaException {
        Level level = getServiceLevel();
        BlockPos target = targetPos();
        if (!level.isLoaded(target)) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(target);
        if (be == null) {
            return null;
        }
        // saveWithId() 与 /data get block 返回内容一致（含 id、x/y/z 与全部字段）
        return (Map<String, Object>) nbtToObject(be.saveWithId());
    }

    /** 校验并返回服务端世界（无世界时抛出 Lua 异常）。 */
    private Level getServiceLevel() throws LuaException {
        Level level = blockEntity.getLevel();
        if (level == null) {
            throw new LuaException("No level available");
        }
        return level;
    }

    /** 递归把 NBT Tag 转换为 Lua 可表示的对象（Map/List/Number/String/Boolean）。 */
    public static Object nbtToObject(Tag tag) {
        switch (tag.getId()) {
            case Tag.TAG_COMPOUND -> {
                CompoundTag compound = (CompoundTag) tag;
                Map<String, Object> map = new HashMap<>();
                for (String key : compound.getAllKeys()) {
                    map.put(key, nbtToObject(compound.get(key)));
                }
                return map;
            }
            case Tag.TAG_LIST -> {
                ListTag list = (ListTag) tag;
                List<Object> out = new ArrayList<>(list.size());
                for (int i = 0; i < list.size(); i++) {
                    out.add(nbtToObject(list.get(i)));
                }
                return out;
            }
            case Tag.TAG_BYTE_ARRAY -> {
                byte[] arr = ((ByteArrayTag) tag).getAsByteArray();
                List<Object> out = new ArrayList<>(arr.length);
                for (byte b : arr) {
                    out.add((int) b);
                }
                return out;
            }
            case Tag.TAG_INT_ARRAY -> {
                int[] arr = ((IntArrayTag) tag).getAsIntArray();
                List<Object> out = new ArrayList<>(arr.length);
                for (int v : arr) {
                    out.add(v);
                }
                return out;
            }
            case Tag.TAG_LONG_ARRAY -> {
                long[] arr = ((LongArrayTag) tag).getAsLongArray();
                List<Object> out = new ArrayList<>(arr.length);
                for (long v : arr) {
                    out.add(v);
                }
                return out;
            }
            case Tag.TAG_STRING -> {
                return tag.getAsString();
            }
            case Tag.TAG_BYTE, Tag.TAG_SHORT, Tag.TAG_INT, Tag.TAG_LONG,
                 Tag.TAG_FLOAT, Tag.TAG_DOUBLE -> {
                return ((NumericTag) tag).getAsDouble();
            }
            default -> {
                return tag.toString();
            }
        }
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof BlockDetectorPeripheral o && blockEntity == o.blockEntity;
    }
}