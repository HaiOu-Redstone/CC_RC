package com.cc_rc.block.digital_display;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 数码显示器 CC: Tweaked 外设
 *
 * 使数码显示器可被 CC 的调制解调器（wired modem）识别为外设，
 * 电脑可通过 Lua 修改方块上显示的橙色状态文字。
 *
 * 用法（Lua）：
 *   local p = peripheral.find("digital_display")
 *   p.setStatus("欢迎使用")
 */
public class DigitalDisplayPeripheral implements IPeripheral {

    private final DigitalDisplayBlockEntity blockEntity;

    public DigitalDisplayPeripheral(DigitalDisplayBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public String getType() {
        return "digital_display";
    }

    /**
     * 向数码显示器传入字符串，修改橙色状态文字内容。
     *
     * @param text 要显示的文字
     * @return 设置后的文字
     */
    @LuaFunction(mainThread = true)
    public final String setStatus(String text) {
        String value = text == null ? DigitalDisplayBlockEntity.DEFAULT_STATUS : text;
        blockEntity.setStatus(Component.literal(value));
        return value;
    }

    /**
     * 读取当前显示的橙色状态文字。
     *
     * @return 当前橙色文字内容
     */
    @LuaFunction(mainThread = true)
    public final String getStatus() {
        return blockEntity.getStatus().getString();
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof DigitalDisplayPeripheral o && blockEntity == o.blockEntity;
    }
}