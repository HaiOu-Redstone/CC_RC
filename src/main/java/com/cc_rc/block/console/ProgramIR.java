package com.cc_rc.block.console;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 控制柜程序 IR（图形化编辑器与编译器的中间表示）。
 *
 * 程序源有两条入口，最终都得到同样的表达式树：
 * <ul>
 *   <li>文本入口：界面输入原文，由 {@link ProgramExprParser} 做词法 / 语法解析；</li>
 *   <li>旧数据入口：存档与网络包里的 NBT / 二进制树，直接按节点还原。</li>
 * </ul>
 * 无论哪条入口，最终都只做类型检查、深度限制与引用校验，再交给 {@link ProgramVM} 编译执行。
 */
public final class ProgramIR {

    private ProgramIR() {
    }

    /**
     * 变量查询接口：IR 不直接持有总线数据，方便客户端与服务端共用同一套校验逻辑。
     */
    public interface Scope {
        /** 变量类型；槽位未定义返回 null。 */
        ConsoleBusBlockEntity.VarType typeOf(int slot);

        /** 变量名；槽位未定义返回带地址的占位文本。 */
        String nameOf(int slot);
    }

    /** 表达式节点类型。 */
    public enum Op {
        /** 浮点常量。 */
        FLOAT,
        /** 布尔常量。 */
        BOOL,
        /** 变量引用。 */
        VAR,
        /** 取负（一元 -）。 */
        NEG,
        /** 逻辑非（一元 !）。 */
        NOT,
        /** 布尔转浮点（显式拓宽）。 */
        CAST,
        ADD,
        SUB,
        MUL,
        DIV,
        /** 向下取整除法（//）。 */
        IDIV,
        /** 取余（%）。 */
        MOD,
        EQ,
        NE,
        LT,
        GT,
        LE,
        GE,
        /** 逻辑与（&&，布尔操作数）。 */
        AND,
        /** 逻辑或（||，布尔操作数）。 */
        OR,
        /** 向下取整（一元 #，等价 floor）。 */
        FLOOR,
        /** 取小数部分（一元 ~，等价 x - floor(x)）。 */
        FRAC,
        /** 取绝对值（一元 @，等价 abs）。 */
        ABS
    }

    /** 不可变表达式树节点。 */
    public static final class Expr {
        public final Op op;
        /** FLOAT 节点的值。 */
        public final float number;
        /** BOOL 节点的值。 */
        public final boolean flag;
        /** VAR 节点引用的变量槽位。 */
        public final int slot;
        public final Expr left;
        public final Expr right;

        private Expr(Op op, float number, boolean flag, int slot, Expr left, Expr right) {
            this.op = op;
            this.number = number;
            this.flag = flag;
            this.slot = slot;
            this.left = left;
            this.right = right;
        }

        public static Expr number(float value) {
            return new Expr(Op.FLOAT, value, false, 0, null, null);
        }

        public static Expr bool(boolean value) {
            return new Expr(Op.BOOL, 0.0F, value, 0, null, null);
        }

        public static Expr var(int slot) {
            return new Expr(Op.VAR, 0.0F, false, slot, null, null);
        }

        public static Expr unary(Op op, Expr inner) {
            return new Expr(op, 0.0F, false, 0, inner, null);
        }

        public static Expr binary(Op op, Expr a, Expr b) {
            return new Expr(op, 0.0F, false, 0, a, b);
        }

        /** 该节点是否为一元运算（只有一个子节点）。 */
        private boolean isUnary() {
            return this.op == Op.NEG || this.op == Op.NOT || this.op == Op.CAST
                    || this.op == Op.FLOOR || this.op == Op.FRAC || this.op == Op.ABS;
        }

        /** 该节点是否为二元运算。 */
        private boolean isBinary() {
            return !this.isUnary() && this.op != Op.FLOAT && this.op != Op.BOOL
                    && this.op != Op.VAR;
        }

        /**
         * 表达式结果类型；引用了未定义变量或运算类型非法时返回 null。
         * 算术运算符接受布尔操作数（按 false=0 / true=1 隐式拓宽）。
         */
        public ConsoleBusBlockEntity.VarType type(Scope scope) {
            switch (this.op) {
                case FLOAT:
                    return ConsoleBusBlockEntity.VarType.FLOAT;
                case BOOL:
                    return ConsoleBusBlockEntity.VarType.BOOL;
                case VAR:
                    return scope == null ? null : scope.typeOf(this.slot);
                case NEG:
                    return isNumeric(typeOf(this.left, scope))
                            ? ConsoleBusBlockEntity.VarType.FLOAT : null;
                case NOT:
                    return typeOf(this.left, scope) == ConsoleBusBlockEntity.VarType.BOOL
                            ? ConsoleBusBlockEntity.VarType.BOOL : null;
                case CAST:
                    return typeOf(this.left, scope) == ConsoleBusBlockEntity.VarType.BOOL
                            ? ConsoleBusBlockEntity.VarType.FLOAT : null;
                case FLOOR:
                case FRAC:
                case ABS:
                    // 取整 / 取小数 / 绝对值：数值（布尔可隐式拓宽）→ 浮点
                    return isNumeric(typeOf(this.left, scope))
                            ? ConsoleBusBlockEntity.VarType.FLOAT : null;
                case ADD:
                case SUB:
                case MUL:
                case DIV:
                case IDIV:
                case MOD:
                    return isNumeric(typeOf(this.left, scope))
                            && isNumeric(typeOf(this.right, scope))
                            ? ConsoleBusBlockEntity.VarType.FLOAT : null;
                case EQ:
                case NE:
                    // 布尔之间按布尔相等，其余按数值比较（布尔可隐式拓宽）
                    return typeOf(this.left, scope) != null && typeOf(this.right, scope) != null
                            ? ConsoleBusBlockEntity.VarType.BOOL : null;
                case LT:
                case GT:
                case LE:
                case GE:
                    return isNumeric(typeOf(this.left, scope))
                            && isNumeric(typeOf(this.right, scope))
                            ? ConsoleBusBlockEntity.VarType.BOOL : null;
                case AND:
                case OR:
                    // 逻辑与 / 或只接受布尔操作数
                    return typeOf(this.left, scope) == ConsoleBusBlockEntity.VarType.BOOL
                            && typeOf(this.right, scope) == ConsoleBusBlockEntity.VarType.BOOL
                            ? ConsoleBusBlockEntity.VarType.BOOL : null;
                default:
                    return null;
            }
        }

        private static ConsoleBusBlockEntity.VarType typeOf(Expr expr, Scope scope) {
            return expr == null ? null : expr.type(scope);
        }

        private static boolean isNumeric(ConsoleBusBlockEntity.VarType type) {
            return type == ConsoleBusBlockEntity.VarType.FLOAT
                    || type == ConsoleBusBlockEntity.VarType.BOOL;
        }

        /** 嵌套深度（叶子为 1），用于编译期的深度上限检查。 */
        public int depth() {
            if (this.isBinary()) {
                return 1 + Math.max(this.left == null ? 0 : this.left.depth(),
                        this.right == null ? 0 : this.right.depth());
            }
            if (this.isUnary()) {
                return 1 + (this.left == null ? 0 : this.left.depth());
            }
            return 1;
        }

        /** 运算符符号（界面显示用）；「无」返回空串。 */
        public static String symbol(Op op) {
            if (op == null) {
                return "";
            }
            switch (op) {
                case ADD:
                    return "+";
                case SUB:
                    return "-";
                case MUL:
                    return "*";
                case DIV:
                    return "/";
                case IDIV:
                    return "//";
                case MOD:
                    return "%";
                case EQ:
                    return "==";
                case NE:
                    return "!=";
                case LT:
                    return "<";
                case GT:
                    return ">";
                case LE:
                    return "<=";
                case GE:
                    return ">=";
                case AND:
                    return "&&";
                case OR:
                    return "||";
                case NEG:
                    return "-";
                case NOT:
                    return "!";
                case CAST:
                    return "float";
                default:
                    return "";
            }
        }

        /** 生成便于阅读的单行文本（嵌套二元运算自动加括号）。 */
        public String toText(Scope scope) {
            switch (this.op) {
                case FLOAT:
                    return formatNumber(this.number);
                case BOOL:
                    // 布尔常量统一显示为 1 / 0
                    return this.flag ? "1" : "0";
                case VAR:
                    return scope == null ? "#" + this.slot : scope.nameOf(this.slot);
                case NEG:
                    return "-" + wrap(this.left, scope);
                case NOT:
                    return "!" + wrap(this.left, scope);
                case CAST:
                    return "float(" + (this.left == null ? "" : this.left.toText(scope)) + ")";
                case FLOOR:
                    return "#" + wrap(this.left, scope);
                case FRAC:
                    return "~" + wrap(this.left, scope);
                case ABS:
                    return "@" + wrap(this.left, scope);
                default:
                    return wrap(this.left, scope) + " " + symbol(this.op) + " "
                            + wrap(this.right, scope);
            }
        }

        /** 子节点是二元运算时加括号，保证显示文本与树结构一致。 */
        private static String wrap(Expr expr, Scope scope) {
            if (expr == null) {
                return "";
            }
            String text = expr.toText(scope);
            return expr.isBinary() ? "(" + text + ")" : text;
        }

        /** 浮点显示：整数不带小数点，其余保留两位。 */
        public static String formatNumber(float value) {
            if (Math.abs(value - Math.round(value)) < 0.0001F) {
                return Long.toString(Math.round(value));
            }
            return String.format(java.util.Locale.ROOT, "%.2f", value);
        }

        /** 写入 NBT（递归）。 */
        public void save(CompoundTag tag) {
            tag.putString("Op", this.op.name());
            switch (this.op) {
                case FLOAT:
                    tag.putFloat("N", this.number);
                    break;
                case BOOL:
                    tag.putBoolean("B", this.flag);
                    break;
                case VAR:
                    tag.putInt("S", this.slot);
                    break;
                case NEG:
                case NOT:
                case CAST:
                case FLOOR:
                case FRAC:
                case ABS:
                    if (this.left != null) {
                        tag.put("L", saveOf(this.left));
                    }
                    break;
                default:
                    if (this.left != null) {
                        tag.put("L", saveOf(this.left));
                    }
                    if (this.right != null) {
                        tag.put("R", saveOf(this.right));
                    }
                    break;
            }
        }

        private static CompoundTag saveOf(Expr expr) {
            CompoundTag tag = new CompoundTag();
            expr.save(tag);
            return tag;
        }

        /** 从 NBT 读取；数据损坏时返回 null（该行会被丢弃）。 */
        public static Expr load(CompoundTag tag) {
            // 限制递归深度：异常存档里的超深树不应把读档线程的栈打爆
            return load(tag, Line.MAX_DEPTH);
        }

        /** 带剩余深度余量的读取；余量耗尽即判为损坏。 */
        private static Expr load(CompoundTag tag, int budget) {
            if (budget <= 0) {
                return null;
            }
            Op op;
            try {
                op = Op.valueOf(tag.getString("Op"));
            } catch (IllegalArgumentException ex) {
                return null;
            }
            switch (op) {
                case FLOAT:
                    return number(tag.getFloat("N"));
                case BOOL:
                    return bool(tag.getBoolean("B"));
                case VAR: {
                    int slot = tag.getInt("S");
                    if (slot <= 0 || slot >= ConsoleBusBlockEntity.VAR_SLOTS_MAX) {
                        return null;
                    }
                    return var(slot);
                }
                case NEG:
                case NOT:
                case CAST:
                case FLOOR:
                case FRAC:
                case ABS: {
                    Expr inner = tag.contains("L") ? load(tag.getCompound("L"), budget - 1) : null;
                    return inner == null ? null : unary(op, inner);
                }
                default: {
                    Expr a = tag.contains("L") ? load(tag.getCompound("L"), budget - 1) : null;
                    Expr b = tag.contains("R") ? load(tag.getCompound("R"), budget - 1) : null;
                    return a == null || b == null ? null : binary(op, a, b);
                }
            }
        }

        /** 写入网络包（递归）。 */
        public void write(FriendlyByteBuf buf) {
            buf.writeByte(this.op.ordinal());
            switch (this.op) {
                case FLOAT:
                    buf.writeFloat(this.number);
                    break;
                case BOOL:
                    buf.writeBoolean(this.flag);
                    break;
                case VAR:
                    buf.writeVarInt(this.slot);
                    break;
                case NEG:
                case NOT:
                case CAST:
                case FLOOR:
                case FRAC:
                case ABS:
                    this.left.write(buf);
                    break;
                default:
                    this.left.write(buf);
                    this.right.write(buf);
                    break;
            }
        }

        /** 从网络包读取；非法数据返回 null。 */
        public static Expr read(FriendlyByteBuf buf) {
            // 与 NBT 读取同样限制深度，避免恶意包构造超深树
            return read(buf, Line.MAX_DEPTH);
        }

        /** 带剩余深度余量的读取；余量耗尽即判为非法。 */
        private static Expr read(FriendlyByteBuf buf, int budget) {
            if (budget <= 0) {
                return null;
            }
            int ordinal = buf.readUnsignedByte();
            Op[] values = Op.values();
            if (ordinal >= values.length) {
                return null;
            }
            Op op = values[ordinal];
            switch (op) {
                case FLOAT:
                    return number(buf.readFloat());
                case BOOL:
                    return bool(buf.readBoolean());
                case VAR: {
                    int slot = buf.readVarInt();
                    return slot <= 0 || slot >= ConsoleBusBlockEntity.VAR_SLOTS_MAX
                            ? null : var(slot);
                }
                case NEG:
                case NOT:
                case CAST:
                case FLOOR:
                case FRAC:
                case ABS: {
                    Expr inner = read(buf, budget - 1);
                    return inner == null ? null : unary(op, inner);
                }
                default: {
                    Expr a = read(buf, budget - 1);
                    Expr b = read(buf, budget - 1);
                    return a == null || b == null ? null : binary(op, a, b);
                }
            }
        }
    }

    /** 行条件：真 / 布尔变量为真 / 布尔变量为假（!变量）。 */
    public enum Cond {
        ALWAYS,
        TRUE_VAR,
        FALSE_VAR
    }

    /** 一行程序：条件 + 目标变量 + 表达式 + 注释。 */
    public static final class Line {
        /** 表达式嵌套深度上限。 */
        public static final int MAX_DEPTH = 16;
        /** 注释最大长度（不参与编译，仅用于列表展示）。 */
        public static final int MAX_COMMENT = 80;
        /** 表达式原文最大长度（含换行；新版文本编辑器原样保存玩家写法）。 */
        public static final int MAX_SOURCE = 1024;

        public final boolean enabled;
        public final Cond cond;
        /** 条件变量槽位（ALWAYS 时无意义）。 */
        public final int condSlot;
        /** 赋值目标变量槽位。 */
        public final int targetSlot;
        public final Expr expr;
        /** 注释文本（无格式要求，编译 / 运行时不使用）。 */
        public final String comment;
        /**
         * 表达式原文（玩家书写的原始文本，可含换行）。
         * 为空表示没有保留原文（旧存档 / 旧编辑器），界面按 {@link Expr#toText} 回显。
         */
        public final String exprSource;
        /**
         * 条件表达式（新版可写任意布尔表达式）。
         * 为 null 表示沿用旧的三态条件 {@link #cond} / {@link #condSlot}（兼容旧存档）。
         */
        public final Expr condExpr;
        /**
         * 条件原文（玩家书写的原始文本）。
         * 为空时界面按 {@link #condExpr} 的 {@link Expr#toText} 或旧三态条件回显。
         */
        public final String condSource;

        public Line(boolean enabled, Cond cond, int condSlot, int targetSlot, Expr expr,
                    String comment, String exprSource, Expr condExpr, String condSource) {
            this.enabled = enabled;
            this.cond = cond == null ? Cond.ALWAYS : cond;
            this.condSlot = condSlot;
            this.targetSlot = targetSlot;
            this.expr = expr;
            this.comment = comment == null ? "" : comment;
            this.exprSource = exprSource == null ? "" : exprSource;
            this.condExpr = condExpr;
            this.condSource = condSource == null ? "" : condSource;
        }

        /** 兼容旧调用：条件只有三态，不保留任何原文。 */
        public Line(boolean enabled, Cond cond, int condSlot, int targetSlot, Expr expr,
                    String comment, String exprSource) {
            this(enabled, cond, condSlot, targetSlot, expr, comment, exprSource, null, "");
        }

        /** 兼容旧调用：不保留表达式原文。 */
        public Line(boolean enabled, Cond cond, int condSlot, int targetSlot, Expr expr,
                    String comment) {
            this(enabled, cond, condSlot, targetSlot, expr, comment, "", null, "");
        }

        /** 复制一份并替换启用状态（界面切换勾选用）。 */
        public Line withEnabled(boolean value) {
            return new Line(value, this.cond, this.condSlot, this.targetSlot, this.expr,
                    this.comment, this.exprSource, this.condExpr, this.condSource);
        }

        /** 校验错误类型（界面与服务端共用）。 */
        public enum Error {
            OK,
            BAD_TARGET,
            BAD_COND,
            BAD_EXPR,
            TYPE_MISMATCH,
            TOO_DEEP
        }

        /** 校验本行；slots 为当前配置的变量槽位总数。 */
        public Error validate(Scope scope, int slots) {
            if (this.targetSlot <= 0 || this.targetSlot >= slots) {
                return Error.BAD_TARGET;
            }
            ConsoleBusBlockEntity.VarType targetType = scope.typeOf(this.targetSlot);
            if (targetType == null) {
                return Error.BAD_TARGET;
            }
            if (this.condExpr != null) {
                // 新版条件表达式：必须是布尔类型，深度同样受限
                if (this.condExpr.depth() > MAX_DEPTH) {
                    return Error.TOO_DEEP;
                }
                if (this.condExpr.type(scope) != ConsoleBusBlockEntity.VarType.BOOL) {
                    return Error.BAD_COND;
                }
            } else if (this.cond != Cond.ALWAYS) {
                if (this.condSlot <= 0 || this.condSlot >= slots) {
                    return Error.BAD_COND;
                }
                if (scope.typeOf(this.condSlot) != ConsoleBusBlockEntity.VarType.BOOL) {
                    return Error.BAD_COND;
                }
            }
            if (this.expr == null) {
                return Error.BAD_EXPR;
            }
            if (this.expr.depth() > MAX_DEPTH) {
                return Error.TOO_DEEP;
            }
            ConsoleBusBlockEntity.VarType type = this.expr.type(scope);
            if (type == null) {
                return Error.BAD_EXPR;
            }
            // 比较结果只能赋给布尔变量，浮点也不能隐式降为布尔
            if (type != targetType) {
                return Error.TYPE_MISMATCH;
            }
            return Error.OK;
        }

        /** 生成便于阅读的单行文本，例如「[真] a = b + 3」。 */
        public String toText(Scope scope) {
            String condText;
            if (this.condExpr != null) {
                // 新版条件表达式：直接按树展开
                condText = this.condExpr.toText(scope);
            } else {
                switch (this.cond) {
                    case TRUE_VAR:
                        condText = scope.nameOf(this.condSlot);
                        break;
                    case FALSE_VAR:
                        condText = "!" + scope.nameOf(this.condSlot);
                        break;
                    default:
                        condText = "true";
                        break;
                }
            }
            String exprText = this.expr == null ? "?" : this.expr.toText(scope);
            return "[" + condText + "] " + scope.nameOf(this.targetSlot) + " = " + exprText;
        }

        /** 写入 NBT（行号由外层列表负责）。 */
        public void save(CompoundTag tag) {
            tag.putBoolean("On", this.enabled);
            tag.putString("Cond", this.cond.name());
            if (this.cond != Cond.ALWAYS) {
                tag.putInt("CondSlot", this.condSlot);
            }
            tag.putInt("Target", this.targetSlot);
            if (this.expr != null) {
                CompoundTag exprTag = new CompoundTag();
                this.expr.save(exprTag);
                tag.put("Expr", exprTag);
            }
            // 注释可为空，空注释不写入以兼容旧存档格式
            if (!this.comment.isEmpty()) {
                tag.putString("Comment", this.comment);
            }
            // 表达式原文：空表示旧数据，不写入
            if (!this.exprSource.isEmpty()) {
                tag.putString("Src", this.exprSource);
            }
            // 条件表达式与条件原文：旧数据没有，不写入
            if (this.condExpr != null) {
                CompoundTag condTag = new CompoundTag();
                this.condExpr.save(condTag);
                tag.put("CondExpr", condTag);
            }
            if (!this.condSource.isEmpty()) {
                tag.putString("CondSrc", this.condSource);
            }
        }

        /** 从 NBT 读取；数据损坏时返回 null。 */
        public static Line load(CompoundTag tag) {
            Cond cond;
            try {
                cond = Cond.valueOf(tag.getString("Cond"));
            } catch (IllegalArgumentException ex) {
                return null;
            }
            int condSlot = tag.contains("CondSlot") ? tag.getInt("CondSlot") : 0;
            int target = tag.getInt("Target");
            if (target <= 0 || target >= ConsoleBusBlockEntity.VAR_SLOTS_MAX) {
                return null;
            }
            Expr expr = tag.contains("Expr") ? Expr.load(tag.getCompound("Expr")) : null;
            if (expr == null) {
                return null;
            }
            String comment = tag.contains("Comment") ? tag.getString("Comment") : "";
            String source = tag.contains("Src") ? tag.getString("Src") : "";
            // 条件表达式 / 原文：旧存档没有，读取结果分别为 null 与空串
            Expr condExpr = tag.contains("CondExpr") ? Expr.load(tag.getCompound("CondExpr")) : null;
            String condSource = tag.contains("CondSrc") ? tag.getString("CondSrc") : "";
            return new Line(tag.getBoolean("On"), cond, condSlot, target, expr, comment, source,
                    condExpr, condSource);
        }

        /** 写入网络包。 */
        public void write(FriendlyByteBuf buf) {
            buf.writeBoolean(this.enabled);
            buf.writeByte(this.cond.ordinal());
            buf.writeVarInt(this.condSlot);
            buf.writeVarInt(this.targetSlot);
            this.expr.write(buf);
            buf.writeUtf(this.comment, MAX_COMMENT);
            buf.writeUtf(this.exprSource, MAX_SOURCE);
            // 条件表达式（可选）+ 条件原文
            buf.writeBoolean(this.condExpr != null);
            if (this.condExpr != null) {
                this.condExpr.write(buf);
            }
            buf.writeUtf(this.condSource, MAX_SOURCE);
        }

        /** 从网络包读取。 */
        public static Line read(FriendlyByteBuf buf) {
            boolean enabled = buf.readBoolean();
            int condOrd = buf.readUnsignedByte();
            Cond[] conds = Cond.values();
            Cond cond = condOrd < conds.length ? conds[condOrd] : Cond.ALWAYS;
            int condSlot = buf.readVarInt();
            int target = buf.readVarInt();
            Expr expr = Expr.read(buf);
            String comment = buf.readUtf(MAX_COMMENT);
            String source = buf.readUtf(MAX_SOURCE);
            Expr condExpr = buf.readBoolean() ? Expr.read(buf) : null;
            String condSource = buf.readUtf(MAX_SOURCE);
            return new Line(enabled, cond, condSlot, target, expr, comment, source,
                    condExpr, condSource);
        }
    }
}
