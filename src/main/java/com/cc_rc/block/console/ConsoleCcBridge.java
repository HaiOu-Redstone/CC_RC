package com.cc_rc.block.console;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 控制柜总线的 CC 接口线程安全桥。
 *
 * CC 电脑在独立线程（WorkerThread）执行 Lua，**严禁直接访问 Level / 方块实体**；
 * 而连续多次外设调用如果都标注 mainThread，每次都要等待主线程下一 tick，
 * 会产生「N 次读写 ≈ N tick」的延迟。这里采用与红石继电器总线相同的思路：
 *
 *  - 主线程每 tick 调用 {@link #publish(Snapshot)} 发布一份只读快照
 *    （变量值、组件值、记录仪历史等），Lua 线程只读 volatile 引用，0 tick；
 *  - Lua 的写操作只进入本类的待处理队列（ReentrantLock 保护），返回后立即结束；
 *  - 主线程每 tick 调用 {@link #drain()} 一次性取出全部待处理操作并按顺序应用，
 *    因此同一 tick 内任意多次读写都只在 tick 末尾结算一次。
 *
 * 读己之写：写操作除入队外还记录到 overlay，读取时优先返回 overlay，
 * overlay 与待处理队列在同一把锁内随 {@link #drain()} 一起清空，
 * 保证「刚写入、马上读取」能看到新值，又不会永久遮蔽主线程后续的修改。
 */
public final class ConsoleCcBridge {

    /** 单 tick 待处理操作上限：防止异常 Lua 脚本无限堆积内存。 */
    public static final int MAX_PENDING_OPS = 2048;

    /** 操作种类。 */
    public static final int OP_VAR_FLOAT = 0;
    public static final int OP_VAR_BOOL = 1;
    public static final int OP_COMP_FLOAT = 2;
    public static final int OP_COMP_BOOL = 3;
    public static final int OP_PLOT_PUSH = 4;
    public static final int OP_PLOT_SET = 5;
    /** RGB 颜色控制器：直接设置灯色（key = 组件代号，number = 0xRRGGBB）。 */
    public static final int OP_RGB_COLOR = 6;

    /** 一条待主线程应用的写操作。 */
    public static final class Op {
        public final int kind;
        /** 变量名或组件代号（统一小写，应用时按名称/代号重新查找）。 */
        public final String key;
        /** 浮点值 / 布尔值（布尔用 0/1 表示）。 */
        public final double number;
        /** 记录仪历史下标（0 起；仅 OP_PLOT_SET 使用）。 */
        public final int index;

        private Op(int kind, String key, double number, int index) {
            this.kind = kind;
            this.key = key;
            this.number = number;
            this.index = index;
        }
    }

    /** 变量快照：名称、类型、槽位、绑定组件代号与当前值。 */
    public static final class VarInfo {
        public final int slot;
        public final String name;
        public final ConsoleBusBlockEntity.VarType type;
        /** 绑定的组件代号；空字符串表示未绑定设备。 */
        public final String compCode;
        public final float floatValue;
        public final boolean boolValue;

        VarInfo(int slot, String name, ConsoleBusBlockEntity.VarType type,
                String compCode, float floatValue, boolean boolValue) {
            this.slot = slot;
            this.name = name;
            this.type = type;
            this.compCode = compCode;
            this.floatValue = floatValue;
            this.boolValue = boolValue;
        }
    }

    /** 组件快照：代号、类型、当前值与记录仪历史（历史数组只换引用，不原地修改）。 */
    public static final class CompInfo {
        public final int id;
        public final String code;
        public final ConsoleCompType type;
        public final float value;
        public final boolean boolValue;
        public final long state;
        public final float[] history;
        public final int decimals;
        public final String unit;
        /** RGB 颜色控制器的灯色（0xRRGGBB；-1 = 原色 / 未着色）。 */
        public final int color;
        /** RGB 颜色控制器当前是否启用（启用 = 不使用原色）。 */
        public final boolean enabled;

        CompInfo(int id, String code, ConsoleCompType type, float value, boolean boolValue,
                 long state, float[] history, int decimals, String unit,
                 int color, boolean enabled) {
            this.id = id;
            this.code = code;
            this.type = type;
            this.value = value;
            this.boolValue = boolValue;
            this.state = state;
            this.history = history;
            this.decimals = decimals;
            this.unit = unit;
            this.color = color;
            this.enabled = enabled;
        }
    }

    /** 主线程发布给 Lua 线程的不可变快照。 */
    public static final class Snapshot {
        public static final Snapshot EMPTY = new Snapshot(0.0F,
                Map.of(), Map.of(), List.of(), List.of());

        public final float period;
        /** 小写变量名 → 变量信息。 */
        public final Map<String, VarInfo> vars;
        /** 小写组件代号 → 组件信息。 */
        public final Map<String, CompInfo> comps;
        /** 按槽位顺序排列的变量表（供 listVars）。 */
        public final List<VarInfo> varList;
        /** 按放置顺序排列的组件表（供 listComponents）。 */
        public final List<CompInfo> compList;

        Snapshot(float period, Map<String, VarInfo> vars, Map<String, CompInfo> comps,
                 List<VarInfo> varList, List<CompInfo> compList) {
            this.period = period;
            this.vars = vars;
            this.comps = comps;
            this.varList = varList;
            this.compList = compList;
        }
    }

    private final ReentrantLock lock = new ReentrantLock();
    /** 待主线程应用的操作（按 Lua 调用顺序）。 */
    private final ArrayDeque<Op> pending = new ArrayDeque<>();
    /** Lua 线程最近写入的覆盖值（读己之写，drain 时清空）。 */
    private final Map<String, Double> varFloatOverlay = new HashMap<>();
    private final Map<String, Boolean> varBoolOverlay = new HashMap<>();
    private final Map<String, Double> compFloatOverlay = new HashMap<>();
    private final Map<String, Boolean> compBoolOverlay = new HashMap<>();
    private final Map<String, float[]> historyOverlay = new HashMap<>();
    /** RGB 灯色覆盖值（读己之写，drain 时清空）。 */
    private final Map<String, Integer> rgbColorOverlay = new HashMap<>();

    /** 主线程最近发布的快照；Lua 线程只读这一个 volatile 引用。 */
    private volatile Snapshot snapshot = Snapshot.EMPTY;

    public Snapshot snapshot() {
        return this.snapshot;
    }

    /** 主线程：整体替换快照（不可变对象，volatile 写入）。 */
    public void publish(Snapshot snapshot) {
        this.snapshot = snapshot == null ? Snapshot.EMPTY : snapshot;
    }

    /** 主线程：取走本 tick 全部待处理操作，并清空覆盖值（与入队共用一把锁）。 */
    public List<Op> drain() {
        this.lock.lock();
        try {
            if (this.pending.isEmpty()) {
                return List.of();
            }
            List<Op> ops = new ArrayList<>(this.pending);
            this.pending.clear();
            this.varFloatOverlay.clear();
            this.varBoolOverlay.clear();
            this.compFloatOverlay.clear();
            this.compBoolOverlay.clear();
            this.historyOverlay.clear();
            this.rgbColorOverlay.clear();
            return ops;
        } finally {
            this.lock.unlock();
        }
    }

    // ---------------------------------------------------------------- 变量

    /** 按名称查找变量（忽略大小写），不存在返回 null。 */
    public VarInfo findVar(String name) {
        if (name == null) {
            return null;
        }
        return this.snapshot.vars.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * 按名称查找「组件的附带变量」（忽略大小写）。
     * 只返回已绑定组件的变量（compCode 非空），未绑定的普通变量返回 null，
     * 避免组件接口意外暴露全部变量。
     */
    private VarInfo findAttachedVar(String name) {
        VarInfo info = findVar(name);
        return info != null && !info.compCode.isEmpty() ? info : null;
    }

    /** 读取变量值（优先返回本 tick 内刚写入的值）；不存在返回 null。 */
    public Object getVar(String name) {
        VarInfo info = findVar(name);
        if (info == null) {
            return null;
        }
        String key = info.name.toLowerCase(Locale.ROOT);
        this.lock.lock();
        try {
            if (info.type == ConsoleBusBlockEntity.VarType.FLOAT) {
                Double overlay = this.varFloatOverlay.get(key);
                return overlay != null ? (Object) overlay : (Object) (double) info.floatValue;
            }
            Boolean overlay = this.varBoolOverlay.get(key);
            return overlay != null ? (Object) overlay : (Object) info.boolValue;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * 写入变量（只入队，不碰 Level）。
     *
     * @param value Boolean 或 Number；类型必须与变量声明一致
     * @return 是否成功入队（变量不存在 / 类型不符 / 队列已满返回 false）
     */
    public boolean setVar(String name, Object value) {
        VarInfo info = findVar(name);
        if (info == null) {
            return false;
        }
        String key = info.name.toLowerCase(Locale.ROOT);
        this.lock.lock();
        try {
            if (this.pending.size() >= MAX_PENDING_OPS) {
                return false;
            }
            if (info.type == ConsoleBusBlockEntity.VarType.FLOAT) {
                Double number = asNumber(value);
                if (number == null) {
                    return false;
                }
                this.pending.add(new Op(OP_VAR_FLOAT, key, number, 0));
                this.varFloatOverlay.put(key, number);
            } else {
                Boolean flag = asBool(value);
                if (flag == null) {
                    return false;
                }
                this.pending.add(new Op(OP_VAR_BOOL, key, flag ? 1.0D : 0.0D, 0));
                this.varBoolOverlay.put(key, flag);
            }
            return true;
        } finally {
            this.lock.unlock();
        }
    }

    // ---------------------------------------------------------------- 组件

    /** 按代号查找组件（忽略大小写），不存在返回 null。 */
    public CompInfo findComp(String code) {
        if (code == null) {
            return null;
        }
        return this.snapshot.comps.get(code.toLowerCase(Locale.ROOT));
    }

    /**
     * 读取组件当前值（浮点返回 Number，开关量返回 Boolean）；不存在返回 null。
     *
     * 支持「附带变量名」：当 code 不是组件代号时，回退为按变量名读取，
     * 便于用同一套接口访问同步指示器的 代号_s（转速）/ 代号_b（灯状态）等附加变量。
     * 仅放行已绑定组件的变量（快照 compCode 非空），普通未绑定变量仍需走 getVar。
     */
    public Object getCompValue(String code) {
        CompInfo info = findComp(code);
        if (info == null) {
            VarInfo attached = findAttachedVar(code);
            return attached == null ? null : getVar(attached.name);
        }
        String key = info.code.toLowerCase(Locale.ROOT);
        this.lock.lock();
        try {
            if (info.type.supportsFloatVariable()) {
                Double overlay = this.compFloatOverlay.get(key);
                return overlay != null ? (Object) overlay : (Object) (double) info.value;
            }
            if (info.type.supportsBoolVariable()) {
                Boolean overlay = this.compBoolOverlay.get(key);
                return overlay != null ? (Object) overlay : (Object) info.boolValue;
            }
            return null;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * 写入组件值（只入队）。
     * 圆盘记录仪按「写入一个采样点」处理；按钮面板只读，返回 false。
     *
     * 支持「附带变量名」：code 不是组件代号时回退为按变量名写入（同样只放行已绑定组件的变量）；
     * 灯状态这类只读输出变量会被下一 tick 的实际判断值覆盖，与 setVar 行为一致。
     *
     * @param value Boolean 或 Number；必须与组件类型匹配
     */
    public boolean setCompValue(String code, Object value) {
        CompInfo info = findComp(code);
        if (info == null) {
            VarInfo attached = findAttachedVar(code);
            return attached != null && setVar(attached.name, value);
        }
        if (!info.type.supportsVariableWrite()) {
            return false;
        }
        String key = info.code.toLowerCase(Locale.ROOT);
        this.lock.lock();
        try {
            if (this.pending.size() >= MAX_PENDING_OPS) {
                return false;
            }
            if (info.type.supportsFloatVariable()) {
                Double number = asNumber(value);
                if (number == null) {
                    return false;
                }
                if (info.type.supportsHistory()) {
                    // 记录仪：与变量上传一致，写入即移位记一笔（范围 0~100，与数字圆盘记录仪一致）
                    double clamped = clampPlotValue(number);
                    this.pending.add(new Op(OP_PLOT_PUSH, key, clamped, 0));
                    this.historyOverlay.put(key, pushHistory(historyOf(info, key), clamped));
                    this.compFloatOverlay.put(key, clamped);
                } else {
                    this.pending.add(new Op(OP_COMP_FLOAT, key, number, 0));
                    this.compFloatOverlay.put(key, number);
                }
            } else if (info.type.supportsBoolVariable()) {
                Boolean flag = asBool(value);
                if (flag == null) {
                    return false;
                }
                this.pending.add(new Op(OP_COMP_BOOL, key, flag ? 1.0D : 0.0D, 0));
                this.compBoolOverlay.put(key, flag);
            } else {
                return false;
            }
            return true;
        } finally {
            this.lock.unlock();
        }
    }

    // ------------------------------------------------------- RGB 颜色控制器

    /** 读取 RGB 控制器的灯色（0xRRGGBB）；不是 RGB 组件或当前为原色时返回 null。 */
    public Double getColor(String code) {
        CompInfo info = findComp(code);
        if (info == null || info.type != ConsoleCompType.RGB_COLOR) {
            return null;
        }
        String key = info.code.toLowerCase(Locale.ROOT);
        this.lock.lock();
        try {
            Integer overlay = this.rgbColorOverlay.get(key);
            int color = overlay != null ? overlay : info.color;
            return color == ConsoleBusBlockEntity.CompEntry.NO_COLOR ? null : (double) color;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * 直接设置 RGB 控制器的灯色（0xRRGGBB，与 CC 的 colors 表兼容），只入队。
     *
     * @return 是否是有效的 RGB 组件且成功入队
     */
    public boolean setColor(String code, double color) {
        CompInfo info = findComp(code);
        if (info == null || info.type != ConsoleCompType.RGB_COLOR || !Double.isFinite(color)) {
            return false;
        }
        int packed = ((int) Math.round(color)) & 0xFFFFFF;
        String key = info.code.toLowerCase(Locale.ROOT);
        this.lock.lock();
        try {
            if (this.pending.size() >= MAX_PENDING_OPS) {
                return false;
            }
            this.pending.add(new Op(OP_RGB_COLOR, key, packed, 0));
            this.rgbColorOverlay.put(key, packed);
            return true;
        } finally {
            this.lock.unlock();
        }
    }

    /** 读取 RGB 三个颜色分量；不是 RGB 组件或当前为原色时返回 null。 */
    public int[] getChannels(String code) {
        Double color = getColor(code);
        if (color == null) {
            return null;
        }
        int packed = color.intValue();
        return new int[]{(packed >> 16) & 0xFF, (packed >> 8) & 0xFF, packed & 0xFF};
    }

    /** 一次设置 RGB 三个分量（0~255 钳制）；返回是否成功入队。 */
    public boolean setChannels(String code, double r, double g, double b) {
        int ri = clampChannel(r);
        int gi = clampChannel(g);
        int bi = clampChannel(b);
        return setColor(code, (ri << 16) | (gi << 8) | bi);
    }

    /**
     * 单独设置一个颜色通道。
     *
     * @param channel "r" / "g" / "b"（也接受 red / green / blue，忽略大小写）
     * @return 通道名无效或组件不是 RGB 时返回 false
     */
    public boolean setChannel(String code, String channel, double value) {
        CompInfo info = findComp(code);
        if (info == null || info.type != ConsoleCompType.RGB_COLOR || !Double.isFinite(value)) {
            return false;
        }
        String name = channel == null ? "" : channel.trim().toLowerCase(Locale.ROOT);
        int shift = switch (name) {
            case "r", "red" -> 16;
            case "g", "green" -> 8;
            case "b", "blue" -> 0;
            default -> -1;
        };
        if (shift < 0) {
            return false;
        }
        // 以当前颜色为基准替换单个通道（当前为原色时从黑色开始组合）。
        // 「当前颜色」必须取 overlay（本 tick 内已写入的颜色），否则同一 tick 里
        // setChannel("r") 之后再 setChannel("g")，后者会用快照里的旧颜色当基准，
        // 把刚才那个 r 通道覆盖掉；两次 setColor 也已各自入队，最终以队尾那条为准，
        // 表现就是「连设两通道，前一通道丢失」。
        String key = info.code.toLowerCase(Locale.ROOT);
        int base;
        this.lock.lock();
        try {
            Integer overlay = this.rgbColorOverlay.get(key);
            int current = overlay != null ? overlay : info.color;
            base = current == ConsoleBusBlockEntity.CompEntry.NO_COLOR ? 0 : current;
        } finally {
            this.lock.unlock();
        }
        int packed = (base & ~(0xFF << shift)) | (clampChannel(value) << shift);
        return setColor(code, packed);
    }

    /** 读取 RGB 控制器是否启用（启用 = 显示设置颜色，停用 = 显示原色）；非 RGB 返回 null。 */
    public Boolean getEnabled(String code) {
        CompInfo info = findComp(code);
        if (info == null || info.type != ConsoleCompType.RGB_COLOR) {
            return null;
        }
        String key = info.code.toLowerCase(Locale.ROOT);
        this.lock.lock();
        try {
            Boolean overlay = this.compBoolOverlay.get(key);
            return overlay != null ? overlay : info.enabled;
        } finally {
            this.lock.unlock();
        }
    }

    /** 设置 RGB 控制器启用 / 停用，返回是否成功入队。 */
    public boolean setEnabled(String code, boolean enabled) {
        return setCompValue(code, enabled);
    }

    /** 颜色分量 0~255 取整并钳制。 */
    private static int clampChannel(double value) {
        if (!Double.isFinite(value)) {
            return 0;
        }
        return (int) Math.max(0, Math.min(255, Math.round(value)));
    }

    // ------------------------------------------------------------ 圆盘记录仪

    /** 写入一个历史采样点（最新在首位），返回钳制后的值；组件不是记录仪返回 null。 */
    public Double pushPoint(String code, double value) {
        CompInfo info = findComp(code);
        if (info == null || !info.type.supportsHistory()) {
            return null;
        }
        String key = info.code.toLowerCase(Locale.ROOT);
        double clamped = clampPlotValue(value);
        this.lock.lock();
        try {
            if (this.pending.size() >= MAX_PENDING_OPS) {
                return null;
            }
            this.pending.add(new Op(OP_PLOT_PUSH, key, clamped, 0));
            float[] next = pushHistory(historyOf(info, key), clamped);
            this.historyOverlay.put(key, next);
            this.compFloatOverlay.put(key, clamped);
            return clamped;
        } finally {
            this.lock.unlock();
        }
    }

    /** 手动设置记录仪某一点（Lua 索引 1~50，1 = 最新），返回是否成功。 */
    public boolean setPoint(String code, int index, double value) {
        CompInfo info = findComp(code);
        if (info == null || !info.type.supportsHistory()) {
            return false;
        }
        int internal = index - 1;
        if (internal < 0 || internal >= ConsoleBusBlockEntity.CompEntry.HISTORY_LEN) {
            return false;
        }
        String key = info.code.toLowerCase(Locale.ROOT);
        double clamped = clampPlotValue(value);
        this.lock.lock();
        try {
            if (this.pending.size() >= MAX_PENDING_OPS) {
                return false;
            }
            this.pending.add(new Op(OP_PLOT_SET, key, clamped, internal));
            float[] next = copyHistory(historyOf(info, key));
            next[internal] = (float) clamped;
            this.historyOverlay.put(key, next);
            // 主线程侧 withPoint() 对 index == 0（Lua 索引 1，即最新一点）会同步更新
            // 组件当前值，因此这里必须跟着写 overlay，否则 Lua 写完第 1 点再读
            // getComponentValue，拿到的还是上一 tick 的旧值，违反本类的「读己之写」契约
            if (internal == 0) {
                this.compFloatOverlay.put(key, clamped);
            }
            return true;
        } finally {
            this.lock.unlock();
        }
    }

    /** 读取记录仪某一点（Lua 索引 1~50）；越界或不是记录仪返回 null。 */
    public Double getPoint(String code, int index) {
        float[] history = getHistory(code);
        int internal = index - 1;
        if (history == null || internal < 0
                || internal >= ConsoleBusBlockEntity.CompEntry.HISTORY_LEN) {
            return null;
        }
        return (double) history[internal];
    }

    /** 读取整段历史（下标 0 = 最新）；不是记录仪返回 null。 */
    public float[] getHistory(String code) {
        CompInfo info = findComp(code);
        if (info == null || !info.type.supportsHistory()) {
            return null;
        }
        String key = info.code.toLowerCase(Locale.ROOT);
        this.lock.lock();
        try {
            float[] overlay = this.historyOverlay.get(key);
            if (overlay != null) {
                return overlay;
            }
        } finally {
            this.lock.unlock();
        }
        if (info.history == null) {
            return new float[ConsoleBusBlockEntity.CompEntry.HISTORY_LEN];
        }
        return info.history;
    }

    /** 取当前可用的历史数组（overlay 优先，否则快照，最后空数组）。 */
    private float[] historyOf(CompInfo info, String key) {
        float[] overlay = this.historyOverlay.get(key);
        if (overlay != null) {
            return overlay;
        }
        if (info.history != null) {
            return info.history;
        }
        return new float[ConsoleBusBlockEntity.CompEntry.HISTORY_LEN];
    }

    /** 生成「整体后移一位、新值写入首位」的新数组（不修改原数组）。 */
    private static float[] pushHistory(float[] base, double value) {
        float[] next = new float[ConsoleBusBlockEntity.CompEntry.HISTORY_LEN];
        System.arraycopy(base, 0, next, 1,
                Math.min(base.length, ConsoleBusBlockEntity.CompEntry.HISTORY_LEN - 1));
        next[0] = (float) value;
        return next;
    }

    /** 复制历史数组，长度统一为 HISTORY_LEN。 */
    private static float[] copyHistory(float[] base) {
        float[] next = new float[ConsoleBusBlockEntity.CompEntry.HISTORY_LEN];
        System.arraycopy(base, 0, next, 0,
                Math.min(base.length, ConsoleBusBlockEntity.CompEntry.HISTORY_LEN));
        return next;
    }

    /** 记录仪数值范围与数字圆盘记录仪一致：钳制到 0~100。 */
    private static double clampPlotValue(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(100.0D, value));
    }

    // -------------------------------------------------------------- 参数解析

    /** Number → Double（拒绝 NaN / Infinity）。 */
    private static Double asNumber(Object value) {
        if (!(value instanceof Number number)) {
            return null;
        }
        double d = number.doubleValue();
        return Double.isFinite(d) ? d : null;
    }

    /** Boolean 或 Number（0 = false，非 0 = true）→ Boolean。 */
    private static Boolean asBool(Object value) {
        if (value instanceof Boolean flag) {
            return flag;
        }
        if (value instanceof Number number) {
            return number.doubleValue() != 0.0D;
        }
        return null;
    }
}
