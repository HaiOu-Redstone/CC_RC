package com.cc_rc.block.console;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * 控制柜总线 CC: Tweaked 外设（类型 "controller_bus"）。
 *
 * 电脑通过有线调制解调器接到控制柜总线后，可读写总线的**变量**与**组件值**，
 * 但不能修改程序与结构。所有方法都**不标注 mainThread**：
 * Lua 线程只读写 {@link ConsoleCcBridge} 中的快照与待处理队列（0 tick），
 * 主线程每 tick 批量应用，因此连续多次读写不会逐个消耗 tick。
 *
 * 执行顺序（同一 tick 内）：
 *   按钮弹起 → 辅助计算机程序 → CC 变量 / 组件出入 → 组件状态统一上传；
 * 程序与 CC 写同一变量时，CC 的写入在程序之后结算。
 *
 * 方法：
 *  - listVars()                     列出全部变量 {name, type, address, component}
 *  - getVar(name)                   读取变量（number / boolean / nil）
 *  - setVar(name, value)            写入变量（排队），返回是否成功
 *  - getPeriod()                    读取程序运行间隔（只读）
 *  - listComponents()               列出全部组件 {code, type, id, kind}
 *  - getComponent(code)             读取组件完整信息（value / state / decimals / unit）
 *  - getComponentValue(code)        读取组件当前值（code 也可写组件附带变量名，如 代号_s / 代号_b）
 *  - setComponentValue(code, value) 写入组件当前值（排队），返回是否成功（同样支持附带变量名）
 *  - pushPoint(code, value)         圆盘记录仪：写入一个采样点并整体移位（0~100），返回写入值
 *  - setPoint(code, index, value)   圆盘记录仪：设置第 index 点（Lua 索引 1~50，1 = 最新）
 *  - getPoint(code, index)          圆盘记录仪：读取第 index 点，越界返回 nil
 *  - getPoints(code)                圆盘记录仪：读取整段历史（长度 50，索引 1 = 最新）
 *  - getColor(code) / setColor(code, rgb)             RGB 控制器：直接读写 0xRRGGBB 灯色（兼容 CC colors 表）
 *  - getChannels(code) / setChannels(code, r, g, b)   RGB 控制器：读写三个颜色分量（0~255）
 *  - setChannel(code, channel, value)                 RGB 控制器：单独设置 r / g / b 通道
 *  - getEnabled(code) / setEnabled(code, on)          RGB 控制器：启用（显示设置颜色）/ 停用（显示原色）
 *
 * 组件代号 code 即玩家在组件设置界面填写的代号（不区分大小写）。
 * 按钮面板为只读按压状态，setComponentValue 会返回 false。
 */
public class ConsoleBusPeripheral implements IPeripheral {

    /** 外设类型名（Lua 中 peripheral.find("controller_bus")）。 */
    private static final String TYPE = "controller_bus";

    private final ConsoleBusBlockEntity bus;
    private final ConsoleCcBridge bridge;

    public ConsoleBusPeripheral(ConsoleBusBlockEntity bus) {
        this.bus = bus;
        this.bridge = bus.getCcBridge();
    }

    /** 列出全部变量：{name, type = "bool"/"float", address = 槽位, component = 绑定设备代号}。 */
    @LuaFunction
    public final Object[] listVars() {
        ConsoleCcBridge.Snapshot snapshot = this.bridge.snapshot();
        Object[] result = new Object[snapshot.varList.size()];
        for (int i = 0; i < result.length; i++) {
            ConsoleCcBridge.VarInfo info = snapshot.varList.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", info.name);
            row.put("type", info.type == ConsoleBusBlockEntity.VarType.BOOL ? "bool" : "float");
            row.put("address", (double) info.slot);
            if (!info.compCode.isEmpty()) {
                row.put("component", info.compCode);
            }
            result[i] = row;
        }
        return result;
    }

    /** 读取变量值；变量不存在返回 nil。 */
    @LuaFunction
    @Nullable
    public final Object getVar(String name) {
        return this.bridge.getVar(name);
    }

    /** 写入变量值（Boolean 或 Number，类型须与变量一致）；成功入队返回 true。 */
    @LuaFunction
    public final boolean setVar(String name, Object value) {
        return this.bridge.setVar(name, value);
    }

    /** 程序运行间隔（tick，0 = 停止）；只读。 */
    @LuaFunction
    public final double getPeriod() {
        return this.bridge.snapshot().period;
    }

    /** 列出全部组件：{code, type = 组件类型 id, id = 组件编号, kind = "bool"/"float"/"text"}。 */
    @LuaFunction
    public final Object[] listComponents() {
        ConsoleCcBridge.Snapshot snapshot = this.bridge.snapshot();
        Object[] result = new Object[snapshot.compList.size()];
        for (int i = 0; i < result.length; i++) {
            ConsoleCcBridge.CompInfo info = snapshot.compList.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", info.code);
            row.put("type", info.type.id);
            row.put("id", (double) info.id);
            row.put("kind", kindOf(info.type));
            result[i] = row;
        }
        return result;
    }

    /** 读取组件完整信息；不存在返回 nil。 */
    @LuaFunction
    @Nullable
    public final Map<String, Object> getComponent(String code) {
        ConsoleCcBridge.CompInfo info = this.bridge.findComp(code);
        if (info == null) {
            return null;
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("code", info.code);
        row.put("type", info.type.id);
        row.put("id", (double) info.id);
        row.put("kind", kindOf(info.type));
        Object value = this.bridge.getCompValue(info.code);
        if (value != null) {
            row.put("value", value);
        }
        if (info.type.supportsBoolVariable()) {
            row.put("state", (double) (info.boolValue ? 1 : 0));
        }
        if (info.type == ConsoleCompType.NUMITRON) {
            row.put("decimals", (double) info.decimals);
            row.put("unit", info.unit);
        }
        if (info.type == ConsoleCompType.RGB_COLOR) {
            // RGB 控制器：color 为 0xRRGGBB（原色时 -1），enabled = 是否显示设置颜色
            row.put("color", (double) info.color);
            row.put("enabled", info.enabled);
        }
        return row;
    }

    /**
     * 读取组件当前值（浮点组件返回 number，开关量返回 boolean）；不存在返回 nil。
     * code 也可以是组件附带变量名（如 代号_s / 代号_b），返回该变量当前值。
     */
    @LuaFunction
    @Nullable
    public final Object getComponentValue(String code) {
        return this.bridge.getCompValue(code);
    }

    /**
     * 写入组件当前值（排队）；记录仪按写入一个采样点处理，按钮面板只读返回 false。
     * code 也可以是组件附带变量名（如 代号_s），写入该变量（类型须匹配）。
     */
    @LuaFunction
    public final boolean setComponentValue(String code, Object value) {
        return this.bridge.setCompValue(code, value);
    }

    // ------------------------------------------------------ RGB 颜色控制器

    /** RGB 控制器：读取灯色（0xRRGGBB，可直接与 CC 的 colors 表比较）；原色或非 RGB 返回 nil。 */
    @LuaFunction
    @Nullable
    public final Double getColor(String code) {
        return this.bridge.getColor(code);
    }

    /** RGB 控制器：直接设置灯色（0xRRGGBB，可用 colors.combine 或 colors.rgb8）；成功返回 true。 */
    @LuaFunction
    public final boolean setColor(String code, double color) {
        return this.bridge.setColor(code, color);
    }

    /** RGB 控制器：读取三个颜色分量 {r = 0~255, g = 0~255, b = 0~255}；原色或非 RGB 返回 nil。 */
    @LuaFunction
    @Nullable
    public final Map<String, Object> getChannels(String code) {
        int[] channels = this.bridge.getChannels(code);
        if (channels == null) {
            return null;
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("r", (double) channels[0]);
        row.put("g", (double) channels[1]);
        row.put("b", (double) channels[2]);
        return row;
    }

    /** RGB 控制器：一次设置三个颜色分量（0~255 钳制）；成功返回 true。 */
    @LuaFunction
    public final boolean setChannels(String code, double r, double g, double b) {
        return this.bridge.setChannels(code, r, g, b);
    }

    /** RGB 控制器：单独设置一个通道（"r" / "g" / "b"，也接受 red / green / blue）；成功返回 true。 */
    @LuaFunction
    public final boolean setChannel(String code, String channel, double value) {
        return this.bridge.setChannel(code, channel, value);
    }

    /** RGB 控制器：读取是否启用（启用 = 显示设置颜色，停用 = 显示原色）；非 RGB 返回 nil。 */
    @LuaFunction
    @Nullable
    public final Boolean getEnabled(String code) {
        return this.bridge.getEnabled(code);
    }

    /** RGB 控制器：启用 / 停用；成功返回 true。 */
    @LuaFunction
    public final boolean setEnabled(String code, boolean enabled) {
        return this.bridge.setEnabled(code, enabled);
    }

    /** 圆盘记录仪：写入一个采样点（0~100 钳制，其余后移），返回写入值；非记录仪返回 nil。 */
    @LuaFunction
    @Nullable
    public final Double pushPoint(String code, double value) {
        return this.bridge.pushPoint(code, value);
    }

    /** 圆盘记录仪：设置第 index 点（Lua 索引 1~50，1 = 最新），返回是否成功。 */
    @LuaFunction
    public final boolean setPoint(String code, int index, double value) {
        return this.bridge.setPoint(code, index, value);
    }

    /** 圆盘记录仪：读取第 index 点；越界或非记录仪返回 nil。 */
    @LuaFunction
    @Nullable
    public final Double getPoint(String code, int index) {
        return this.bridge.getPoint(code, index);
    }

    /** 圆盘记录仪：读取整段历史（长度 50，索引 1 = 最新）；非记录仪返回 nil。 */
    @LuaFunction
    @Nullable
    public final Object[] getPoints(String code) {
        float[] history = this.bridge.getHistory(code);
        if (history == null) {
            return null;
        }
        Double[] result = new Double[ConsoleBusBlockEntity.CompEntry.HISTORY_LEN];
        for (int i = 0; i < result.length; i++) {
            float sample = i < history.length && Float.isFinite(history[i]) ? history[i] : 0.0F;
            result[i] = (double) sample;
        }
        return result;
    }

    /** 组件种类：布尔 / 浮点 / 文本（发信终端暂不开放读写）。 */
    private static String kindOf(ConsoleCompType type) {
        if (type.supportsBoolVariable()) {
            return "bool";
        }
        if (type.supportsFloatVariable()) {
            return "float";
        }
        return "text";
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof ConsoleBusPeripheral o && this.bus == o.bus;
    }
}
