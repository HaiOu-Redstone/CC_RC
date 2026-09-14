package com.cc_rc.block.extended_relay;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.ComputerSide;
import org.jetbrains.annotations.Nullable;

/**
 * 扩展红石继电器总线 CC: Tweaked 外设。
 *
 * 外设类型 "redstone_relay_bus"。通过有线调制解调器（wired modem）连接后，
 * Lua 可沿总线朝向在任意距离（紧贴=1，最大 16 可配置）对扩展红石继电器做
 * 数字/模拟红石信号的读写（类似 CC 电脑内置 redstone API 的远程版本）。
 *
 * 无延迟改造：全部方法**不标注 mainThread**——Lua 线程（CC WorkerThread）
 * 直接读写继电器线程安全状态（{@link RelayState}，ReentrantLock 保护，
 * 0 tick），**绝不访问 Minecraft 主线程的 Level/方块实体**：总线的
 * 「distance → 继电器状态」快照缓存由总线方块实体主线程每 tick 刷新，
 * Lua 线程只读 volatile 快照。输出变化由继电器主线程 tick 合并应用到世界。
 *
 * side 参数语义：**以目标继电器自身的朝向为基准的本地方向**
 * （"top"/"bottom"/"left"/"right"/"front"/"back"），与总线的朝向无关——
 * 用户可通过 isRelay(distance) 分别探测不同距离的继电器，再对各自侧边读写。
 *
 * 方法：
 *  - isRelay(distance)：检查 distance 处是否为扩展红石继电器
 *  - setOutput(distance, side, on)：布尔输出（15/0）
 *  - getOutput(distance, side)：布尔输出读取
 *  - setAnalogOutput(distance, side, 0-15)：模拟输出（校验范围）
 *  - getAnalogOutput(distance, side)：模拟输出读取
 *  - getInput(distance, side)：布尔输入读取
 *  - getAnalogInput(distance, side)：模拟输入读取
 */
public class ExtendedRelayBusPeripheral implements IPeripheral {

    /** 外设类型名（Lua 中 peripheral.find("redstone_relay_bus")） */
    private static final String TYPE = "redstone_relay_bus";

    private final ExtendedRelayBusBlockEntity bus;

    public ExtendedRelayBusPeripheral(ExtendedRelayBusBlockEntity bus) {
        this.bus = bus;
    }

    private static ComputerSide parseSide(String sideName) throws LuaException {
        ComputerSide side = ComputerSide.valueOfInsensitive(sideName);
        if (side == null) {
            throw new LuaException("Invalid side: " + sideName + " (expected top/bottom/left/right/front/back)");
        }
        return side;
    }

    private static int parseDistance(int distance) throws LuaException {
        if (distance < 1) throw new LuaException("Distance must be at least 1");
        return distance;
    }

    /** 按距离取继电器线程安全状态；未找到时抛出 Lua 异常（Lua 线程读缓存，不碰 Level）。 */
    private static RelayState requireRelay(ExtendedRelayBusBlockEntity bus, int distance) throws LuaException {
        RelayState relay = bus.getRelayState(parseDistance(distance));
        if (relay == null) {
            throw new LuaException("No extension relay at distance " + distance);
        }
        return relay;
    }

    /** 检查 distance 处是否为扩展红石继电器。 */
    @LuaFunction
    public final boolean isRelay(int distance) throws LuaException {
        return bus.getRelayState(parseDistance(distance)) != null;
    }

    /** 布尔输出：找到距离 distance 处的继电器，把 side 输出设为 on（15）或 off（0）。 */
    @LuaFunction
    public final void setOutput(int distance, String sideName, boolean on) throws LuaException {
        requireRelay(bus, distance).setOutput(parseSide(sideName), on ? 15 : 0);
    }

    /** 布尔输出读取：继电器 side 当前输出是否 > 0。 */
    @LuaFunction
    public final boolean getOutput(int distance, String sideName) throws LuaException {
        return requireRelay(bus, distance).getOutput(parseSide(sideName)) > 0;
    }

    /** 模拟输出：0~15 信号强度写入继电器 side。 */
    @LuaFunction
    public final void setAnalogOutput(int distance, String sideName, int value) throws LuaException {
        if (value < 0 || value > 15) throw new LuaException("Expected number in range 0-15");
        requireRelay(bus, distance).setOutput(parseSide(sideName), value);
    }

    /** 模拟输出读取：继电器 side 当前输出强度 0~15。 */
    @LuaFunction
    public final int getAnalogOutput(int distance, String sideName) throws LuaException {
        return requireRelay(bus, distance).getOutput(parseSide(sideName));
    }

    /** 布尔输入读取：继电器 side 是否从世界收到信号。 */
    @LuaFunction
    public final boolean getInput(int distance, String sideName) throws LuaException {
        return requireRelay(bus, distance).getInput(parseSide(sideName)) > 0;
    }

    /** 模拟输入读取：继电器 side 从世界读入的信号强度 0~15。 */
    @LuaFunction
    public final int getAnalogInput(int distance, String sideName) throws LuaException {
        return requireRelay(bus, distance).getInput(parseSide(sideName));
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof ExtendedRelayBusPeripheral o && bus == o.bus;
    }
}