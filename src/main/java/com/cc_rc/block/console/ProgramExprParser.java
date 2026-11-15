package com.cc_rc.block.console;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 程序表达式文本解析器（Pratt / 优先级爬升）。
 *
 * 约定：
 * <ul>
 *   <li>空格、制表符、换行只作分隔符，不产生任何语义（多行书写等同单行）；</li>
 *   <li>每个 Token 记录 [start, end) 字符区间，供界面着色与错误标红；</li>
 *   <li>一元运算只用符号：{@code !} 逻辑非、{@code -} 取负、{@code #} 向下取整、
 *       {@code ~} 取小数部分、{@code @} 取绝对值；不提供 floor()/abs() 之类的函数写法，
 *       避免与变量名产生歧义；</li>
 *   <li>唯一保留的函数式写法是历史数据里的 {@code float(x)}（显式布尔转浮点），
 *       因为 {@link ProgramIR.Expr#toText} 仍然这样输出，需要能读回。</li>
 * </ul>
 *
 * 解析失败时返回 {@code expr == null} 与首个错误，调用方据此禁止提交并标红对应区间。
 */
public final class ProgramExprParser {

    /** 一元运算的绑定优先级（高于所有二元运算）。 */
    private static final int PREC_UNARY = 6;

    /** 错误码：表达式为空。 */
    public static final String ERR_EMPTY = "empty";
    /** 错误码：无法识别的字符。 */
    public static final String ERR_CHAR = "bad_char";
    /** 错误码：数字格式不正确或超出范围。 */
    public static final String ERR_NUMBER = "bad_number";
    /** 错误码：未定义的变量。 */
    public static final String ERR_NAME = "unknown_var";
    /** 错误码：此处不该出现该符号。 */
    public static final String ERR_TOKEN = "unexpected_token";
    /** 错误码：表达式不完整（缺少操作数）。 */
    public static final String ERR_END = "unexpected_end";
    /** 错误码：缺少右括号。 */
    public static final String ERR_RPAREN = "missing_rparen";
    /** 错误码：类型不匹配。 */
    public static final String ERR_TYPE = "type_mismatch";
    /** 错误码：嵌套过深。 */
    public static final String ERR_DEPTH = "too_deep";

    private ProgramExprParser() {
    }

    /** 变量查询：名称 → 槽位，并兼作 {@link ProgramIR.Expr#type} 的作用域。 */
    public interface Vars extends ProgramIR.Scope {
        /** 按名称查变量槽位（实现方忽略大小写）；未定义返回 -1。 */
        int findSlot(String name);
    }

    /** Token 类别（着色与错误定位用）。 */
    public enum TokenKind {
        /** 变量名。 */
        NAME,
        /** 数字常量。 */
        NUMBER,
        /** true / false。 */
        BOOL,
        /** 运算符（含一元前缀）与取负用的减号。 */
        OP,
        /** 左括号。 */
        LPAREN,
        /** 右括号。 */
        RPAREN,
        /** 输入结束（零长度，位置等于原文长度）。 */
        EOF
    }

    /** 词法单元；start / end 为原文中的字符区间（左闭右开）。 */
    public static final class Token {
        public final TokenKind kind;
        public final String text;
        public final int start;
        public final int end;
        /** OP 对应的运算符（其他类别为 null）。 */
        public final ProgramIR.Op op;
        /** NUMBER 的数值。 */
        public final float number;
        /** BOOL 的值。 */
        public final boolean flag;
        /** NAME 解析到的槽位；未定义或不是变量时为 -1。 */
        public final int slot;

        private Token(TokenKind kind, String text, int start, int end, ProgramIR.Op op,
                      float number, boolean flag, int slot) {
            this.kind = kind;
            this.text = text;
            this.start = start;
            this.end = end;
            this.op = op;
            this.number = number;
            this.flag = flag;
            this.slot = slot;
        }
    }

    /** 解析错误：错误码 + 原文区间 + 中文提示（界面可改用错误码做本地化）。 */
    public static final class Error {
        public final String code;
        public final int start;
        public final int end;
        public final String message;

        private Error(String code, int start, int end, String message) {
            this.code = code;
            this.start = start;
            this.end = end;
            this.message = message;
        }
    }

    /** 解析结果：成功时 expr 非空，失败时 error 非空；tokens 始终可用（供着色）。 */
    public static final class Result {
        public final ProgramIR.Expr expr;
        public final Error error;
        public final List<Token> tokens;

        private Result(ProgramIR.Expr expr, Error error, List<Token> tokens) {
            this.expr = expr;
            this.error = error;
            this.tokens = tokens;
        }

        public boolean ok() {
            return this.expr != null && this.error == null;
        }
    }

    /**
     * 解析一段表达式文本。
     *
     * @param source     原文（可含换行，换行没有语义）
     * @param vars       变量查询；为空时所有名称都视为未定义
     * @param targetType 目标变量类型，用于 0/1 的布尔判定与赋值类型校验；null 表示不做赋值校验
     */
    public static Result parse(String source, Vars vars, ConsoleBusBlockEntity.VarType targetType) {
        String src = source == null ? "" : source;
        Lexer lexer = new Lexer(src, vars);
        lexer.lex();
        List<Token> tokens = Collections.unmodifiableList(lexer.tokens);
        if (lexer.error != null) {
            return new Result(null, lexer.error, tokens);
        }
        if (src.trim().isEmpty()) {
            return new Result(null, new Error(ERR_EMPTY, 0, src.length(), "表达式为空"), tokens);
        }
        Parser parser = new Parser(tokens);
        ProgramIR.Expr expr = parser.parseExpression(1, targetType);
        if (parser.error == null) {
            Token extra = parser.peek();
            if (extra.kind != TokenKind.EOF) {
                parser.fail(ERR_TOKEN, extra, "这里不该出现「" + extra.text + "」");
            }
        }
        if (parser.error != null) {
            return new Result(null, parser.error, tokens);
        }
        // 整段表达式在原文中的区间（用于类型 / 深度这类整体性错误）
        int start = tokens.get(0).start;
        int end = parser.lastEnd();
        ConsoleBusBlockEntity.VarType actual = expr.type(vars);
        if (actual == null) {
            return new Result(null, new Error(ERR_TYPE, start, end, "表达式中有类型不匹配的运算"), tokens);
        }
        if (targetType != null && actual != targetType) {
            return new Result(null, new Error(ERR_TYPE, start, end,
                    "表达式结果是" + typeName(actual) + "，目标是" + typeName(targetType)), tokens);
        }
        if (expr.depth() > ProgramIR.Line.MAX_DEPTH) {
            return new Result(null, new Error(ERR_DEPTH, start, end, depthMessage()), tokens);
        }
        return new Result(expr, null, tokens);
    }

    /** 运算符优先级（数字越大越优先，0 表示不是二元运算符）。 */
    private static int precedenceOf(ProgramIR.Op op) {
        if (op == null) {
            return 0;
        }
        return switch (op) {
            case OR -> 1;
            case AND -> 2;
            case EQ, NE, LT, GT, LE, GE -> 3;
            case ADD, SUB -> 4;
            case MUL, DIV, IDIV, MOD -> 5;
            default -> 0;
        };
    }

    /** 二元运算两侧操作数期望的类型（决定 0/1 是否按布尔常量解释）。 */
    private static ConsoleBusBlockEntity.VarType operandExpectOf(ProgramIR.Op op) {
        return op == ProgramIR.Op.AND || op == ProgramIR.Op.OR
                ? ConsoleBusBlockEntity.VarType.BOOL
                : ConsoleBusBlockEntity.VarType.FLOAT;
    }

    /** 该运算符能否出现在操作数位置（前缀一元）。 */
    private static boolean isUnaryOp(ProgramIR.Op op) {
        return op == ProgramIR.Op.SUB || op == ProgramIR.Op.NOT
                || op == ProgramIR.Op.FLOOR || op == ProgramIR.Op.FRAC
                || op == ProgramIR.Op.ABS;
    }

    /** Token 运算符 → 一元运算节点运算符（减号在操作数位置是取负）。 */
    private static ProgramIR.Op asUnaryOp(ProgramIR.Op op) {
        return op == ProgramIR.Op.SUB ? ProgramIR.Op.NEG : op;
    }

    private static String typeName(ConsoleBusBlockEntity.VarType type) {
        return type == ConsoleBusBlockEntity.VarType.BOOL ? "布尔" : "数值";
    }

    private static String depthMessage() {
        return "表达式嵌套过深（上限 " + ProgramIR.Line.MAX_DEPTH + " 层）";
    }

    /** 词法分析：跳过空白（含换行），产出 Token 序列；非法字符记录首个错误。 */
    private static final class Lexer {
        private final String src;
        private final Vars vars;
        private final List<Token> tokens = new ArrayList<>();
        private int pos;
        private Error error;

        private Lexer(String src, Vars vars) {
            this.src = src;
            this.vars = vars;
        }

        private void lex() {
            int length = this.src.length();
            while (this.pos < length) {
                char c = this.src.charAt(this.pos);
                if (Character.isWhitespace(c)) {
                    this.pos++;
                    continue;
                }
                if (Character.isLetter(c) || c == '_') {
                    this.readName();
                    continue;
                }
                if (Character.isDigit(c) || (c == '.' && this.pos + 1 < length
                        && Character.isDigit(this.src.charAt(this.pos + 1)))) {
                    this.readNumber();
                    continue;
                }
                if (!this.readSymbol()) {
                    return;
                }
            }
            this.tokens.add(new Token(TokenKind.EOF, "", length, length, null, 0.0F, false, -1));
        }

        private void readName() {
            int start = this.pos;
            int length = this.src.length();
            while (this.pos < length && isNamePart(this.src.charAt(this.pos))) {
                this.pos++;
            }
            String text = this.src.substring(start, this.pos);
            if (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false")) {
                this.tokens.add(new Token(TokenKind.BOOL, text, start, this.pos, null, 0.0F,
                        text.equalsIgnoreCase("true"), -1));
                return;
            }
            int slot = this.vars == null ? -1 : this.vars.findSlot(text);
            this.tokens.add(new Token(TokenKind.NAME, text, start, this.pos, null, 0.0F,
                    false, slot));
        }

        private static boolean isNamePart(char c) {
            return Character.isLetterOrDigit(c) || c == '_';
        }

        private void readNumber() {
            int start = this.pos;
            int length = this.src.length();
            while (this.pos < length && Character.isDigit(this.src.charAt(this.pos))) {
                this.pos++;
            }
            if (this.pos < length && this.src.charAt(this.pos) == '.') {
                this.pos++;
                while (this.pos < length && Character.isDigit(this.src.charAt(this.pos))) {
                    this.pos++;
                }
            }
            // 科学计数法：只有确实跟了数字才吞掉 e / E，否则（如「2e」）回退
            if (this.pos < length
                    && (this.src.charAt(this.pos) == 'e' || this.src.charAt(this.pos) == 'E')) {
                int save = this.pos;
                this.pos++;
                if (this.pos < length && (this.src.charAt(this.pos) == '+'
                        || this.src.charAt(this.pos) == '-')) {
                    this.pos++;
                }
                if (this.pos < length && Character.isDigit(this.src.charAt(this.pos))) {
                    while (this.pos < length && Character.isDigit(this.src.charAt(this.pos))) {
                        this.pos++;
                    }
                } else {
                    this.pos = save;
                }
            }
            String text = this.src.substring(start, this.pos);
            float value;
            try {
                value = Float.parseFloat(text);
            } catch (NumberFormatException e) {
                this.error = new Error(ERR_NUMBER, start, this.pos, "数字格式不正确「" + text + "」");
                return;
            }
            if (!Float.isFinite(value)) {
                this.error = new Error(ERR_NUMBER, start, this.pos, "数字超出范围「" + text + "」");
                return;
            }
            this.tokens.add(new Token(TokenKind.NUMBER, text, start, this.pos, null, value,
                    false, -1));
        }

        /** 读取括号或运算符；非法字符记录错误并返回 false。 */
        private boolean readSymbol() {
            int start = this.pos;
            char c = this.src.charAt(start);
            if (c == '(' || c == ')') {
                this.tokens.add(new Token(c == '(' ? TokenKind.LPAREN : TokenKind.RPAREN,
                        String.valueOf(c), start, start + 1, null, 0.0F, false, -1));
                this.pos++;
                return true;
            }
            // 双字符运算符优先（// 是整除，不是注释）
            String pair = start + 1 < this.src.length() ? this.src.substring(start, start + 2) : "";
            ProgramIR.Op op = null;
            int width = 1;
            switch (pair) {
                case "&&" -> {
                    op = ProgramIR.Op.AND;
                    width = 2;
                }
                case "||" -> {
                    op = ProgramIR.Op.OR;
                    width = 2;
                }
                case "==" -> {
                    op = ProgramIR.Op.EQ;
                    width = 2;
                }
                case "!=" -> {
                    op = ProgramIR.Op.NE;
                    width = 2;
                }
                case "<=" -> {
                    op = ProgramIR.Op.LE;
                    width = 2;
                }
                case ">=" -> {
                    op = ProgramIR.Op.GE;
                    width = 2;
                }
                case "//" -> {
                    op = ProgramIR.Op.IDIV;
                    width = 2;
                }
                default -> op = symbolOf(c);
            }
            if (op == null) {
                // 单个 = 是常见笔误：单独提示要写 ==，其余字符按无法识别处理
                String message = c == '='
                        ? "相等比较请写「==」"
                        : "无法识别的字符「" + c + "」";
                this.error = new Error(ERR_CHAR, start, start + 1, message);
                return false;
            }
            this.pos += width;
            this.tokens.add(new Token(TokenKind.OP, this.src.substring(start, this.pos), start,
                    this.pos, op, 0.0F, false, -1));
            return true;
        }

        /** 单字符运算符；减号记为 SUB，在操作数位置再解释为取负。 */
        private static ProgramIR.Op symbolOf(char c) {
            return switch (c) {
                case '+' -> ProgramIR.Op.ADD;
                case '-' -> ProgramIR.Op.SUB;
                case '*' -> ProgramIR.Op.MUL;
                case '/' -> ProgramIR.Op.DIV;
                case '%' -> ProgramIR.Op.MOD;
                case '!' -> ProgramIR.Op.NOT;
                case '<' -> ProgramIR.Op.LT;
                case '>' -> ProgramIR.Op.GT;
                case '#' -> ProgramIR.Op.FLOOR;
                case '~' -> ProgramIR.Op.FRAC;
                case '@' -> ProgramIR.Op.ABS;
                default -> null;
            };
        }
    }

    /** 语法分析：维护 token 下标、括号/一元嵌套计数与首个错误。 */
    private static final class Parser {
        private final List<Token> tokens;
        private int index;
        private int nesting;
        private Error error;

        private Parser(List<Token> tokens) {
            this.tokens = tokens;
        }

        private Token peek() {
            return this.tokens.get(this.index);
        }

        private Token peekNext() {
            int next = Math.min(this.index + 1, this.tokens.size() - 1);
            return this.tokens.get(next);
        }

        private void advance() {
            if (this.index < this.tokens.size() - 1) {
                this.index++;
            }
        }

        private void fail(String code, Token token, String message) {
            if (this.error == null) {
                this.error = new Error(code, token.start, token.end, message);
            }
        }

        /** 最后一个已消费 Token 的结束位置。 */
        private int lastEnd() {
            return this.tokens.get(Math.max(0, this.index - 1)).end;
        }

        /**
         * 解析一个表达式片段。
         *
         * @param minPrec 只接受优先级不低于该值的二元运算符
         * @param expect  期望类型（决定 0/1 按布尔常量还是浮点常量解释）
         */
        private ProgramIR.Expr parseExpression(int minPrec,
                                               ConsoleBusBlockEntity.VarType expect) {
            ProgramIR.Expr left = this.parseUnary(expect);
            if (left == null) {
                return null;
            }
            while (true) {
                Token token = this.peek();
                if (token.kind != TokenKind.OP) {
                    break;
                }
                int prec = precedenceOf(token.op);
                if (prec == 0 || prec < minPrec) {
                    break;
                }
                this.advance();
                ProgramIR.Expr right = this.parseExpression(prec + 1, operandExpectOf(token.op));
                if (right == null) {
                    return null;
                }
                left = ProgramIR.Expr.binary(token.op, left, right);
            }
            return left;
        }

        /** 操作数位置：先处理一元前缀（可连续叠用），再落到原子。 */
        private ProgramIR.Expr parseUnary(ConsoleBusBlockEntity.VarType expect) {
            Token token = this.peek();
            if (token.kind == TokenKind.OP && isUnaryOp(token.op)) {
                if (++this.nesting > ProgramIR.Line.MAX_DEPTH) {
                    this.fail(ERR_DEPTH, token, depthMessage());
                    return null;
                }
                this.advance();
                ConsoleBusBlockEntity.VarType operandExpect =
                        token.op == ProgramIR.Op.NOT
                                ? ConsoleBusBlockEntity.VarType.BOOL
                                : ConsoleBusBlockEntity.VarType.FLOAT;
                // 一元只吃比自己更高优先级的部分，因此「-a * b」是「(-a) * b」
                ProgramIR.Expr inner = this.parseExpression(PREC_UNARY, operandExpect);
                this.nesting--;
                if (inner == null) {
                    return null;
                }
                return ProgramIR.Expr.unary(asUnaryOp(token.op), inner);
            }
            return this.parsePrimary(expect);
        }

        private ProgramIR.Expr parsePrimary(ConsoleBusBlockEntity.VarType expect) {
            Token token = this.peek();
            switch (token.kind) {
                case NUMBER -> {
                    this.advance();
                    // 布尔位置上 0 / 1 按布尔常量处理（与旧行编辑器一致）
                    if (expect == ConsoleBusBlockEntity.VarType.BOOL
                            && (token.number == 0.0F || token.number == 1.0F)) {
                        return ProgramIR.Expr.bool(token.number == 1.0F);
                    }
                    return ProgramIR.Expr.number(token.number);
                }
                case BOOL -> {
                    this.advance();
                    return ProgramIR.Expr.bool(token.flag);
                }
                case NAME -> {
                    return this.parseName();
                }
                case LPAREN -> {
                    if (++this.nesting > ProgramIR.Line.MAX_DEPTH) {
                        this.fail(ERR_DEPTH, token, depthMessage());
                        return null;
                    }
                    this.advance();
                    ProgramIR.Expr inner = this.parseExpression(1, expect);
                    this.nesting--;
                    if (inner == null) {
                        return null;
                    }
                    Token close = this.peek();
                    if (close.kind != TokenKind.RPAREN) {
                        this.fail(ERR_RPAREN, close, "缺少右括号");
                        return null;
                    }
                    this.advance();
                    return inner;
                }
                case EOF -> {
                    this.fail(ERR_END, token, "表达式不完整，缺少操作数");
                    return null;
                }
                default -> {
                    this.fail(ERR_TOKEN, token, "这里不该出现「" + token.text + "」");
                    return null;
                }
            }
        }

        /** 变量名，或历史数据里的 float(...) 显式拓宽。 */
        private ProgramIR.Expr parseName() {
            Token token = this.peek();
            // 名为 float 的变量优先按变量解释；只有 float 未定义时才当作历史写法 float(x)
            if (token.slot <= 0 && token.text.equalsIgnoreCase("float")
                    && this.peekNext().kind == TokenKind.LPAREN) {
                this.advance();
                // 与括号同样计入嵌套深度，防止 float(float(float(...))) 绕过限制
                if (++this.nesting > ProgramIR.Line.MAX_DEPTH) {
                    this.fail(ERR_DEPTH, token, depthMessage());
                    return null;
                }
                this.advance();
                ProgramIR.Expr inner =
                        this.parseExpression(1, ConsoleBusBlockEntity.VarType.BOOL);
                this.nesting--;
                if (inner == null) {
                    return null;
                }
                Token close = this.peek();
                if (close.kind != TokenKind.RPAREN) {
                    this.fail(ERR_RPAREN, close, "缺少右括号");
                    return null;
                }
                this.advance();
                return ProgramIR.Expr.unary(ProgramIR.Op.CAST, inner);
            }
            if (token.slot <= 0) {
                this.fail(ERR_NAME, token, "未定义的变量「" + token.text + "」");
                return null;
            }
            this.advance();
            return ProgramIR.Expr.var(token.slot);
        }
    }
}
