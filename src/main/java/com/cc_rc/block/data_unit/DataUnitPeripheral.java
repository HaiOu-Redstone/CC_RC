package com.cc_rc.block.data_unit;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.jetbrains.annotations.Nullable;

/**
 * 数据单元 CC: Tweaked 外设
 *
 * 使数据单元可被 CC 的调制解调器识别为外设，电脑可通过 Lua 读写：
 *   - 名称（字符串）
 *   - 整个数据列表（整数表，索引 1 起）
 *   - 列表某一位（索引 1 起）
 *
 * 用法（Lua）：
 *   local p = peripheral.find("data_unit")
 *   p.setName("server-01")          -- 设置名称
 *   local n = p.getName()            -- 读取名称
 *   p.setList({10, 20, 30})         -- 替换整个列表
 *   local list = p.getList()        -- 读取整个列表（Lua 表，索引 1 起）
 *   p.setValue(1, 99)               -- 设置第 1 位（索引==长度时追加）
 *   local v = p.getValue(2)         -- 读取第 2 位；越界返回 nil
 *
 * 所有方法均无 mainThread：直接读写方块实体的 volatile 字段（0 tick），
 * 广播由主线程 tick 节流合并执行（与数字调节器/圆盘记录仪一致）。
 */
public class DataUnitPeripheral implements IPeripheral {

    private final DataUnitBlockEntity blockEntity;

    public DataUnitPeripheral(DataUnitBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public String getType() {
        return "data_unit";
    }

    /** 读取名称（字符串）。 */
    @LuaFunction
    public final String getName() {
        return blockEntity.getName();
    }

    /** 设置名称（字符串）。 */
    @LuaFunction
    public final void setName(String name) {
        blockEntity.setName(name);
    }

    /**
     * 读取整个列表。
     *
     * @return Lua 表（索引 1 起，空列表为 {}）
     */
    @LuaFunction
    public final Object[] getList() {
        int[] data = blockEntity.getData();
        Integer[] list = new Integer[data.length];
        for (int i = 0; i < data.length; i++) {
            list[i] = data[i];
        }
        return list;
    }

    /**
     * 替换整个列表（长度上限 1024）。
     *
     * @param list Lua 表（索引 1 起的整数）
     * @return 是否成功（超长返回 false）
     */
    @LuaFunction
    public final boolean setList(int[] list) {
        return blockEntity.setList(list);
    }

    /**
     * 设置列表某一位（索引 1 起；索引 == 长度时追加，越界返回 false）。
     *
     * @param index 位置索引（Lua 惯例从 1 开始）
     * @param value 要写入的整数
     * @return 是否写入成功
     */
    @LuaFunction
    public final boolean setValue(int index, int value) {
        return blockEntity.setValue(index - 1, value);
    }

    /**
     * 读取列表某一位（索引 1 起）。
     *
     * @param index 位置索引（Lua 惯例从 1 开始）
     * @return 该位置的值；索引越界返回 nil
     */
    @LuaFunction
    @Nullable
    public final Integer getValue(int index) {
        return blockEntity.getValue(index - 1);
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof DataUnitPeripheral o && blockEntity == o.blockEntity;
    }
}
