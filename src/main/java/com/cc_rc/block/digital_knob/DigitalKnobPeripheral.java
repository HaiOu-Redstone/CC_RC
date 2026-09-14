package com.cc_rc.block.digital_knob;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.jetbrains.annotations.Nullable;

/**
 * 数字调节器 CC: Tweaked 外设
 *
 * 使数字调节器可被 CC 的调制解调器识别为外设，
 * 电脑可通过 Lua 读写整数值（0~1000，越界自动钳制）。
 *
 * 用法（Lua）：
 *   local p = peripheral.find("digital_knob")
 *   p.setValue(500)      -- 传入整数，越界自动钳制到 0~1000
 *   local v = p.getValue()  -- 读取整数 0~1000
 */
public class DigitalKnobPeripheral implements IPeripheral {

    private final DigitalKnobBlockEntity blockEntity;

    public DigitalKnobPeripheral(DigitalKnobBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public String getType() {
        return "digital_knob";
    }

    /**
     * 设置数值（整数），小于0设为0，大于1000设为1000。
     * 无 mainThread：直接写方块实体的 volatile 字段（0 tick），
     * 广播由主线程 tick 节流合并执行。
     *
     * @param value 传入的整数
     * @return 钳制后的整数
     */
    @LuaFunction
    public final int setValue(int value) {
        blockEntity.setValue(value);
        return blockEntity.getValue();
    }

    /**
     * 读取当前数值（整数0~1000）。
     *
     * @return 当前整数值
     */
    @LuaFunction
    public final int getValue() {
        return blockEntity.getValue();
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof DigitalKnobPeripheral o && blockEntity == o.blockEntity;
    }
}