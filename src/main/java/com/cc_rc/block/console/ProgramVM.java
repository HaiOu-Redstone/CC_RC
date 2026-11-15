package com.cc_rc.block.console;

import java.util.ArrayList;
import java.util.List;

/**
 * 控制柜程序字节码编译器与栈式虚拟机。
 *
 * 设计要点（与《控制柜程序系统实现方案》§7 一致）：
 *  - 指令与操作数打包进一个 int：低 8 位为操作码，高 24 位为操作数（槽位 / 跳转目标）；
 *  - 浮点常量占用两条 int：指令后紧跟 Float.floatToRawIntBits；
 *  - 跳转操作数使用「指令下标」，不是字节偏移；
 *  - 执行期只读写外部传入的影子数组，不碰组件、不做同步，保证一次执行零分配；
 *  - 运行期错误不中断程序：除零结果记 0、非有限结果置 0，并记录警告（每行去重）。
 */
public final class ProgramVM {

    private ProgramVM() {
    }

    /** 指令集版本（指令含义变化时递增，旧存档可据此丢弃字节码）。 */
    public static final int BYTECODE_VERSION = 2;

    /** 单次编译允许的最大指令条数（防止异常程序拖慢 tick）。 */
    public static final int MAX_CODE = 2048;

    // ---------------------------------------------------------------- 指令集（0x00 ~ 0x1E）

    public static final int OP_NOP = 0x00;
    /** 压入浮点常量：后跟一条原始位模式。 */
    public static final int OP_PUSH_F = 0x01;
    /** 压入布尔常量：操作数 0 / 1。 */
    public static final int OP_PUSH_B = 0x02;
    public static final int OP_LOAD_F = 0x03;
    public static final int OP_LOAD_B = 0x04;
    public static final int OP_STORE_F = 0x05;
    public static final int OP_STORE_B = 0x06;
    public static final int OP_ADD_F = 0x07;
    public static final int OP_SUB_F = 0x08;
    public static final int OP_MUL_F = 0x09;
    public static final int OP_DIV_F = 0x0A;
    /** 向下取整除法。 */
    public static final int OP_FLOORDIV_F = 0x0B;
    public static final int OP_MOD_F = 0x0C;
    public static final int OP_NEG_F = 0x0D;
    public static final int OP_NOT_B = 0x0E;
    public static final int OP_AND_B = 0x0F;
    public static final int OP_OR_B = 0x10;
    public static final int OP_LT_F = 0x11;
    public static final int OP_LE_F = 0x12;
    public static final int OP_GT_F = 0x13;
    public static final int OP_GE_F = 0x14;
    public static final int OP_EQ_F = 0x15;
    public static final int OP_NE_F = 0x16;
    public static final int OP_EQ_B = 0x17;
    public static final int OP_NE_B = 0x18;
    /** 布尔转浮点（false = 0.0，true = 1.0）。 */
    public static final int OP_B2F = 0x19;
    /** 栈顶布尔为 false 时跳转。 */
    public static final int OP_JMP_IF_FALSE = 0x1A;
    public static final int OP_HALT = 0x1B;
    /** 向下取整（一元，弹 1 压 1）。 */
    public static final int OP_FLOOR_F = 0x1C;
    /** 取小数部分 x - floor(x)（一元）。 */
    public static final int OP_FRAC_F = 0x1D;
    /** 取绝对值（一元）。 */
    public static final int OP_ABS_F = 0x1E;

    /** 浮点相等比较容差。 */
    public static final float EPSILON = 1.0E-5F;

    /** 运行期警告原因。 */
    public enum Reason {
        /** 除零，结果按 0 处理。 */
        DIV_ZERO,
        /** 结果非有限值（NaN / Infinity），已置 0。 */
        NON_FINITE,
        /** 变量绑定的组件已失效，使用冻结值继续计算。 */
        SOURCE_INVALID
    }

    /** 一条运行期警告：所属程序行号 + 原因。 */
    public static final class Warning {
        /** 程序行下标（0 ~ 63）。 */
        public final int line;
        public final Reason reason;

        public Warning(int line, Reason reason) {
            this.line = line;
            this.reason = reason;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Warning other)) {
                return false;
            }
            return this.line == other.line && this.reason == other.reason;
        }

        @Override
        public int hashCode() {
            return this.line * 31 + this.reason.ordinal();
        }
    }

    /** 编译产物：指令流 + 每条指令所属程序行。 */
    public static final class Compiled {
        public final int[] code;
        /** 与 code 等长：该指令来自哪一行程序（-1 表示辅助指令）。 */
        public final int[] lineOf;

        Compiled(int[] code, int[] lineOf) {
            this.code = code;
            this.lineOf = lineOf;
        }
    }

    // ---------------------------------------------------------------- 编译

    /**
     * 把程序行编译为字节码。
     *
     * @param lines 程序行数组（下标即行号，null 表示空行）
     * @param scope 变量查询视图，用于判断操作数类型（决定生成 LOAD_F 还是 LOAD_B）
     * @return 编译结果；没有任何可执行行或指令数超限时返回 null
     */
    public static Compiled compile(ProgramIR.Line[] lines, ProgramIR.Scope scope) {
        if (lines == null) {
            return null;
        }
        Builder builder = new Builder();
        for (int i = 0; i < lines.length; i++) {
            ProgramIR.Line line = lines[i];
            if (line == null || !line.enabled || line.expr == null) {
                continue;
            }
            builder.line = i;
            // 条件不是「真」时，条件为假直接跳到本行之后
            int jumpAt = -1;
            if (line.condExpr != null) {
                // 新版条件表达式：直接编译成布尔值，再按假跳转
                compileValue(builder, line.condExpr, scope);
                jumpAt = builder.emit(OP_JMP_IF_FALSE, 0);
            } else if (line.cond != ProgramIR.Cond.ALWAYS) {
                builder.emit(OP_LOAD_B, line.condSlot);
                if (line.cond == ProgramIR.Cond.FALSE_VAR) {
                    builder.emit(OP_NOT_B, 0);
                }
                jumpAt = builder.emit(OP_JMP_IF_FALSE, 0);
            }
            compileValue(builder, line.expr, scope);
            // 目标类型由校验保证与表达式类型一致，这里直接按目标定义生成 STORE
            ConsoleBusBlockEntity.VarType targetType = scope.typeOf(line.targetSlot);
            if (targetType == ConsoleBusBlockEntity.VarType.BOOL) {
                builder.emit(OP_STORE_B, line.targetSlot);
            } else {
                builder.emit(OP_STORE_F, line.targetSlot);
            }
            // 回填跳转目标：本行结束后的下一条指令
            if (jumpAt >= 0) {
                builder.patchOperand(jumpAt, builder.size());
            }
            if (builder.size() > MAX_CODE) {
                return null;
            }
        }
        if (builder.size() == 0) {
            return null;
        }
        builder.line = -1;
        builder.emit(OP_HALT, 0);
        return builder.build();
    }

    /** 编译一个值表达式（节点类型由 IR 保证合法）。 */
    private static void compileValue(Builder builder, ProgramIR.Expr expr, ProgramIR.Scope scope) {
        switch (expr.op) {
            case FLOAT -> {
                builder.emit(OP_PUSH_F, 0);
                builder.raw(Float.floatToRawIntBits(expr.number));
            }
            case BOOL -> builder.emit(OP_PUSH_B, expr.flag ? 1 : 0);
            case VAR -> {
                if (scope.typeOf(expr.slot) == ConsoleBusBlockEntity.VarType.BOOL) {
                    builder.emit(OP_LOAD_B, expr.slot);
                } else {
                    builder.emit(OP_LOAD_F, expr.slot);
                }
            }
            case NEG -> {
                compileAsFloat(builder, expr.left, scope);
                builder.emit(OP_NEG_F, 0);
            }
            case NOT -> {
                compileValue(builder, expr.left, scope);
                builder.emit(OP_NOT_B, 0);
            }
            case CAST -> {
                compileValue(builder, expr.left, scope);
                builder.emit(OP_B2F, 0);
            }
            // 取整 / 取小数 / 绝对值：先按浮点编译操作数，再发一元指令
            case FLOOR, FRAC, ABS -> {
                compileAsFloat(builder, expr.left, scope);
                builder.emit(switch (expr.op) {
                    case FLOOR -> OP_FLOOR_F;
                    case FRAC -> OP_FRAC_F;
                    default -> OP_ABS_F;
                }, 0);
            }
            case ADD, SUB, MUL, DIV, IDIV, MOD -> {
                compileAsFloat(builder, expr.left, scope);
                compileAsFloat(builder, expr.right, scope);
                builder.emit(switch (expr.op) {
                    case ADD -> OP_ADD_F;
                    case SUB -> OP_SUB_F;
                    case MUL -> OP_MUL_F;
                    case DIV -> OP_DIV_F;
                    case IDIV -> OP_FLOORDIV_F;
                    default -> OP_MOD_F;
                }, 0);
            }
            case LT, GT, LE, GE -> {
                compileAsFloat(builder, expr.left, scope);
                compileAsFloat(builder, expr.right, scope);
                builder.emit(switch (expr.op) {
                    case LT -> OP_LT_F;
                    case GT -> OP_GT_F;
                    case LE -> OP_LE_F;
                    default -> OP_GE_F;
                }, 0);
            }
            case EQ, NE -> {
                // 双方都是布尔时按布尔比较，否则按浮点比较（布尔可隐式拓宽）
                boolean bothBool = expr.left != null && expr.right != null
                        && expr.left.type(scope) == ConsoleBusBlockEntity.VarType.BOOL
                        && expr.right.type(scope) == ConsoleBusBlockEntity.VarType.BOOL;
                if (bothBool) {
                    compileValue(builder, expr.left, scope);
                    compileValue(builder, expr.right, scope);
                    builder.emit(expr.op == ProgramIR.Op.EQ ? OP_EQ_B : OP_NE_B, 0);
                } else {
                    compileAsFloat(builder, expr.left, scope);
                    compileAsFloat(builder, expr.right, scope);
                    builder.emit(expr.op == ProgramIR.Op.EQ ? OP_EQ_F : OP_NE_F, 0);
                }
            }
            case AND, OR -> {
                compileValue(builder, expr.left, scope);
                compileValue(builder, expr.right, scope);
                builder.emit(expr.op == ProgramIR.Op.AND ? OP_AND_B : OP_OR_B, 0);
            }
            default -> builder.emit(OP_PUSH_F, 0);
        }
    }

    /** 编译一个操作数并保证栈上是浮点（布尔自动补 B2F）。 */
    private static void compileAsFloat(Builder builder, ProgramIR.Expr expr, ProgramIR.Scope scope) {
        if (expr == null) {
            builder.emit(OP_PUSH_F, 0);
            builder.raw(Float.floatToRawIntBits(0.0F));
            return;
        }
        compileValue(builder, expr, scope);
        if (expr.type(scope) == ConsoleBusBlockEntity.VarType.BOOL) {
            builder.emit(OP_B2F, 0);
        }
    }

    /** 可增长的 int 指令缓冲区（编译期使用，执行期不再分配）。 */
    private static final class Builder {
        private int[] data = new int[64];
        private int[] lines = new int[64];
        private int size;
        private int line = -1;

        int size() {
            return this.size;
        }

        void raw(int value) {
            if (this.size == this.data.length) {
                int capacity = this.data.length * 2;
                this.data = java.util.Arrays.copyOf(this.data, capacity);
                this.lines = java.util.Arrays.copyOf(this.lines, capacity);
            }
            this.data[this.size] = value;
            this.lines[this.size] = this.line;
            this.size++;
        }

        /** 写入一条「操作码 + 操作数」指令，返回该指令的下标。 */
        int emit(int op, int operand) {
            int index = this.size;
            raw((op & 0xFF) | (operand << 8));
            return index;
        }

        /** 回填指令操作数（跳转目标）。 */
        void patchOperand(int index, int operand) {
            this.data[index] = (this.data[index] & 0xFF) | (operand << 8);
        }

        Compiled build() {
            return new Compiled(java.util.Arrays.copyOf(this.data, this.size),
                    java.util.Arrays.copyOf(this.lines, this.size));
        }
    }

    // ---------------------------------------------------------------- 执行

    /**
     * 执行编译产物；只读写传入的影子数组。
     *
     * @param compiled 编译结果
     * @param floats   浮点变量影子数组（下标即槽位）
     * @param bools    布尔变量影子数组（下标即槽位）
     * @return 本轮警告（同一行同一原因只保留一条，最多 {@link #MAX_WARNINGS} 条）
     */
    public static List<Warning> execute(Compiled compiled, float[] floats, boolean[] bools) {
        List<Warning> warnings = new ArrayList<>();
        if (compiled == null || floats == null || bools == null) {
            return warnings;
        }
        int[] code = compiled.code;
        // 表达式树深度上限 16，理论上栈深不超过 32；这里给足余量
        float[] stackF = new float[STACK_SIZE];
        boolean[] stackB = new boolean[STACK_SIZE];
        int sp = 0;
        int pc = 0;
        // 防御损坏的字节码：线性程序不允许回跳，仍限制总步数
        int steps = 0;
        int maxSteps = code.length * 4 + 64;
        while (pc >= 0 && pc < code.length && steps++ < maxSteps) {
            int instruction = code[pc];
            int op = instruction & 0xFF;
            int arg = instruction >>> 8;
            // 防御损坏的字节码：栈深不足以支撑本指令时立即结束本轮，
            // 否则 --sp 会变成负数并让读写落到错误的下标上
            if (sp < requiredStack(op)) {
                return warnings;
            }
            int at = pc++;
            switch (op) {
                case OP_PUSH_F -> {
                    if (pc >= code.length) {
                        return warnings;
                    }
                    pushF(stackF, sp++, Float.intBitsToFloat(code[pc++]));
                }
                case OP_PUSH_B -> {
                    pushB(stackB, sp++, arg != 0);
                }
                case OP_LOAD_F -> {
                    pushF(stackF, sp++, floats[clampSlot(arg, floats.length)]);
                }
                case OP_LOAD_B -> {
                    pushB(stackB, sp++, bools[clampSlot(arg, bools.length)]);
                }
                case OP_STORE_F -> {
                    int slot = clampSlot(arg, floats.length);
                    float value = popF(stackF, --sp);
                    // 写入前统一清洗非有限值，避免污染后续计算
                    if (!Float.isFinite(value)) {
                        value = 0.0F;
                        warn(warnings, compiled, at, Reason.NON_FINITE);
                    }
                    floats[slot] = value;
                }
                case OP_STORE_B -> {
                    int slot = clampSlot(arg, bools.length);
                    bools[slot] = popB(stackB, --sp);
                }
                case OP_ADD_F -> {
                    float b = popF(stackF, --sp);
                    float a = popF(stackF, --sp);
                    pushF(stackF, sp++, checkFinite(a + b, warnings, compiled, at));
                }
                case OP_SUB_F -> {
                    float b = popF(stackF, --sp);
                    float a = popF(stackF, --sp);
                    pushF(stackF, sp++, checkFinite(a - b, warnings, compiled, at));
                }
                case OP_MUL_F -> {
                    float b = popF(stackF, --sp);
                    float a = popF(stackF, --sp);
                    pushF(stackF, sp++, checkFinite(a * b, warnings, compiled, at));
                }
                case OP_DIV_F -> {
                    float b = popF(stackF, --sp);
                    float a = popF(stackF, --sp);
                    float value = 0.0F;
                    if (b == 0.0F) {
                        warn(warnings, compiled, at, Reason.DIV_ZERO);
                    } else {
                        value = checkFinite(a / b, warnings, compiled, at);
                    }
                    pushF(stackF, sp++, value);
                }
                case OP_FLOORDIV_F -> {
                    float b = popF(stackF, --sp);
                    float a = popF(stackF, --sp);
                    float value = 0.0F;
                    if (b == 0.0F) {
                        warn(warnings, compiled, at, Reason.DIV_ZERO);
                    } else {
                        value = checkFinite((float) Math.floor(a / b), warnings, compiled, at);
                    }
                    pushF(stackF, sp++, value);
                }
                case OP_MOD_F -> {
                    float b = popF(stackF, --sp);
                    float a = popF(stackF, --sp);
                    float value = 0.0F;
                    if (b == 0.0F) {
                        warn(warnings, compiled, at, Reason.DIV_ZERO);
                    } else {
                        // 余数符号跟随除数
                        value = checkFinite(a - (float) Math.floor(a / b) * b, warnings, compiled, at);
                    }
                    pushF(stackF, sp++, value);
                }
                case OP_NEG_F -> {
                    // 先弹出到局部变量再压栈：同一个表达式里不要同时使用 sp++ 与 --sp，
                    // 否则 Java 从左到右求值会让读栈用错下标（一元运算曾因此失效）
                    float a = popF(stackF, --sp);
                    pushF(stackF, sp++, -a);
                }
                case OP_NOT_B -> {
                    boolean a = popB(stackB, --sp);
                    pushB(stackB, sp++, !a);
                }
                case OP_FLOOR_F -> {
                    float a = popF(stackF, --sp);
                    pushF(stackF, sp++, (float) Math.floor(a));
                }
                case OP_FRAC_F -> {
                    float a = popF(stackF, --sp);
                    pushF(stackF, sp++, a - (float) Math.floor(a));
                }
                case OP_ABS_F -> {
                    float a = popF(stackF, --sp);
                    pushF(stackF, sp++, Math.abs(a));
                }
                case OP_AND_B -> {
                    boolean b = popB(stackB, --sp);
                    boolean a = popB(stackB, --sp);
                    pushB(stackB, sp++, a && b);
                }
                case OP_OR_B -> {
                    boolean b = popB(stackB, --sp);
                    boolean a = popB(stackB, --sp);
                    pushB(stackB, sp++, a || b);
                }
                case OP_LT_F -> {
                    float b = popF(stackF, --sp);
                    float a = popF(stackF, --sp);
                    pushB(stackB, sp++, a < b);
                }
                case OP_LE_F -> {
                    float b = popF(stackF, --sp);
                    float a = popF(stackF, --sp);
                    pushB(stackB, sp++, a <= b);
                }
                case OP_GT_F -> {
                    float b = popF(stackF, --sp);
                    float a = popF(stackF, --sp);
                    pushB(stackB, sp++, a > b);
                }
                case OP_GE_F -> {
                    float b = popF(stackF, --sp);
                    float a = popF(stackF, --sp);
                    pushB(stackB, sp++, a >= b);
                }
                case OP_EQ_F -> {
                    float b = popF(stackF, --sp);
                    float a = popF(stackF, --sp);
                    pushB(stackB, sp++, Math.abs(a - b) <= EPSILON);
                }
                case OP_NE_F -> {
                    float b = popF(stackF, --sp);
                    float a = popF(stackF, --sp);
                    pushB(stackB, sp++, Math.abs(a - b) > EPSILON);
                }
                case OP_EQ_B -> {
                    boolean b = popB(stackB, --sp);
                    boolean a = popB(stackB, --sp);
                    pushB(stackB, sp++, a == b);
                }
                case OP_NE_B -> {
                    boolean b = popB(stackB, --sp);
                    boolean a = popB(stackB, --sp);
                    pushB(stackB, sp++, a != b);
                }
                case OP_B2F -> {
                    boolean a = popB(stackB, --sp);
                    pushF(stackF, sp++, a ? 1.0F : 0.0F);
                }
                case OP_JMP_IF_FALSE -> {
                    if (!popB(stackB, --sp)) {
                        pc = arg;
                    }
                }
                case OP_HALT, OP_NOP -> {
                    return warnings;
                }
                default -> {
                    // 未知操作码视为程序结束，避免死循环
                    return warnings;
                }
            }
        }
        return warnings;
    }

    /** 警告条数上限（GUI 只显示最近几条，避免刷屏）。 */
    public static final int MAX_WARNINGS = 4;

    private static final int STACK_SIZE = 64;

    /**
     * 该操作码执行前最少需要的栈深（只用于防御损坏的字节码）。
     * 压栈 / 跳转 / 结束类指令需要 0，一元与写变量需要 1，二元运算需要 2。
     */
    private static int requiredStack(int op) {
        return switch (op) {
            case OP_ADD_F, OP_SUB_F, OP_MUL_F, OP_DIV_F, OP_FLOORDIV_F, OP_MOD_F,
                 OP_AND_B, OP_OR_B, OP_LT_F, OP_LE_F, OP_GT_F, OP_GE_F,
                 OP_EQ_F, OP_NE_F, OP_EQ_B, OP_NE_B -> 2;
            case OP_STORE_F, OP_STORE_B, OP_NEG_F, OP_NOT_B, OP_B2F,
                 OP_JMP_IF_FALSE, OP_FLOOR_F, OP_FRAC_F, OP_ABS_F -> 1;
            default -> 0;
        };
    }

    /** 浮点栈写入越界保护（异常字节码时不抛异常）。 */
    private static void pushF(float[] stack, int index, float value) {
        if (index >= 0 && index < stack.length) {
            stack[index] = value;
        }
    }

    private static void pushB(boolean[] stack, int index, boolean value) {
        if (index >= 0 && index < stack.length) {
            stack[index] = value;
        }
    }

    private static float popF(float[] stack, int index) {
        return index >= 0 && index < stack.length ? stack[index] : 0.0F;
    }

    private static boolean popB(boolean[] stack, int index) {
        return index >= 0 && index < stack.length && stack[index];
    }

    private static int clampSlot(int slot, int length) {
        return Math.max(0, Math.min(length - 1, slot));
    }

    /** 结果非有限时置 0 并记警告。 */
    private static float checkFinite(float value, List<Warning> warnings,
            Compiled compiled, int at) {
        if (Float.isFinite(value)) {
            return value;
        }
        warn(warnings, compiled, at, Reason.NON_FINITE);
        return 0.0F;
    }

    /** 记录一条警告（同一行同一原因去重，最多 MAX_WARNINGS 条）。 */
    private static void warn(List<Warning> warnings, Compiled compiled, int at, Reason reason) {
        if (warnings.size() >= MAX_WARNINGS) {
            return;
        }
        int line = at >= 0 && at < compiled.lineOf.length ? compiled.lineOf[at] : -1;
        Warning warning = new Warning(line, reason);
        if (!warnings.contains(warning)) {
            warnings.add(warning);
        }
    }
}
