package com.cc_rc.block.digital_plotter;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.jetbrains.annotations.Nullable;

/**
 * 数字圆盘记录仪 CC: Tweaked 外设
 *
 * 使数字圆盘记录仪可被 CC 的调制解调器识别为外设，
 * 电脑可通过 Lua 读写长度50的整数列表（值0~100，越界自动钳制）。
 *
 * 用法（Lua）：
 *   local p = peripheral.find("digital_plotter")
 *   p.push(30)              -- 写入一个新值（自动删除最后一位并移位），越界钳制0~100，返回写入后的值
 *   local ok = p.setValue(3, 50)  -- 手动设置第3个位置的值，返回是否成功；索引越界返回 false
 *   local v = p.getValue(3)       -- 读取第3个位置的值；索引越界返回 nil
 *   local list = p.getList()      -- 读取整个列表（长度50，索引1~50）
 *
 * 注意：
 *   - 写入值只能为整型（Lua 传小数会报错）；超过100按100计，小于0按0计
 *   - 索引采用 Lua 惯例 1~50（对应内部 0~49）；超出列表范围不予写入或读取
 */
public class DigitalPlotterPeripheral implements IPeripheral {

    private final DigitalPlotterBlockEntity blockEntity;

    public DigitalPlotterPeripheral(DigitalPlotterBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public String getType() {
        return "digital_plotter";
    }

    /**
     * 写入一个新值：删除最后一位，其他值后移，新值写入首位。
     * 写入值只能为整型，超过100按100计，小于0按0计。
     *
     * @param value 要写入的整数
     * @return 写入后的值（钳制到 0~100）
     */
    @LuaFunction(mainThread = true)
    public final int push(int value) {
        blockEntity.push(value);
        return blockEntity.getValue(0);
    }

    /**
     * 手动设置列表中某个位置的值。
     * 写入值只能为整型，超过100按100计，小于0按0计。
     * 索引超出列表范围（1~50）时不予写入。
     *
     * @param index 位置索引（Lua 惯例从 1 开始，对应内部 0~49）
     * @param value 要设置的整数
     * @return 是否写入成功（索引越界返回 false）
     */
    @LuaFunction(mainThread = true)
    public final boolean setValue(int index, int value) {
        return blockEntity.setValue(index - 1, value);
    }

    /**
     * 读取列表中某个位置的值（0~100）。
     * 索引超出列表范围（1~50）时返回 nil（不予读取）。
     *
     * @param index 位置索引（Lua 惯例从 1 开始，对应内部 0~49）
     * @return 该位置的值；索引越界返回 nil
     */
    @LuaFunction
    @Nullable
    public final Integer getValue(int index) {
        return blockEntity.getValue(index - 1);
    }

    /**
     * 读取整个列表（长度50，索引1~50）。
     *
     * @return 包含50个整数的列表（Lua 表，索引1~50）
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

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof DigitalPlotterPeripheral o && blockEntity == o.blockEntity;
    }
}
