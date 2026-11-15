package com.cc_rc.block.console;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * 控制柜配置文件的 JSON 数据结构（客户端保存 / 服务端加载共用）。
 *
 * 设计目标：文件可以直接用记事本打开阅读和手工修改，因此：
 *  - 字段名使用简短的小写英文，不含内部 id / 槽位号；
 *  - 变量引用一律写「变量名」，组件绑定一律写「组件代号」，
 *    加载时再按名称重新分配内部槽位与组件 id；
 *  - 不保存运行期状态（state / value / 历史采样），只保存可复现的结构信息。
 *
 * 顶层字段：
 *  - format / version：格式标识与版本，用于拒绝无关文件；
 *  - size / facing：多方块结构尺寸与总线朝向（加载时必须完全一致）；
 *  - components：面板组件；
 *  - texts：表面文字；
 *  - variables：变量表；
 *  - program：辅助计算机程序。
 */
public final class ConsoleConfigJson {

    /** 文件格式标识（写入 format 字段）。 */
    public static final String FORMAT = "cc_rc_console_config";
    /** 当前格式版本。 */
    public static final int VERSION = 1;

    /** 缩进输出，方便记事本查看；默认不输出 null 字段。 */
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConsoleConfigJson() {
    }

    /** 配置文件根对象。 */
    public static final class Root {
        /** 默认 null：文件里缺少 format 字段时视为非法，避免误加载任意 JSON。 */
        public String format;
        public int version = VERSION;
        public Size size;
        public String facing;
        public List<Component> components = new ArrayList<>();
        public List<Text> texts = new ArrayList<>();
        public List<Variable> variables = new ArrayList<>();
        public Program program;
    }

    /** 结构尺寸（格）。 */
    public static final class Size {
        public int x;
        public int y;
        public int z;

        public Size() {
        }

        public Size(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /** 面板组件。坐标 (u, v) 以所在表面中心为原点，单位为结构像素。 */
    public static final class Component {
        /** 组件类型资源名（见 ConsoleCompType.id，如 gauge、numitron）。 */
        public String type;
        /** 所在表面外法线方向（north / south / east / west / up / down）。 */
        public String face;
        public float u;
        public float v;
        /** 组件代号（变量按该名称绑定）。 */
        public String code;
        /** 第二名称（仅圆盘记录仪：推送触发变量按该名称绑定）。 */
        public String code2;
        /** 底板颜色 0xRRGGBB；-1 = 保持原样。 */
        public int color = CompColor.NONE;
        /** 附带文字（null = 无）。 */
        public TextContent text;
        /** 数字显示屏：保留小数位。 */
        public int decimals = ConsoleBusBlockEntity.CompEntry.DEFAULT_DECIMALS;
        /** 数字显示屏：显示单位。 */
        public String unit = "";
        /** 按钮：弹起时间（tick，-1 = 跟随配置，0 = 按住直到松手）。 */
        public int holdTicks = ConsoleBusBlockEntity.CompEntry.DEFAULT_HOLD_TICKS;
        /** 组件缩放（1.0 = 模型默认大小）。 */
        public float scale = ConsoleBusBlockEntity.CompEntry.DEFAULT_SCALE;
        /** RGB 颜色控制器：连接的组件代号（用「;」分隔；空 = 不连接）。 */
        public String links = "";
        /** RGB 颜色控制器：R / G / B 三个通道变量名（空 = 不启用）。 */
        public String rgbR = "";
        public String rgbG = "";
        public String rgbB = "";
        /** RGB 颜色控制器：是否显示原色（= 停用控制）。 */
        public boolean showOriginal;
    }

    /** 文字内容与样式（组件附带文字使用）。 */
    public static final class TextContent {
        public String content = "";
        public int color = -1;
        public boolean bold;
        public boolean italic;
        public boolean underline;
        public boolean strikethrough;
    }

    /** 结构表面文字（带锚点与缩放）。 */
    public static final class Text {
        public String face;
        public float u;
        public float v;
        public String content = "";
        public int color = -1;
        public boolean bold;
        public boolean italic;
        public boolean underline;
        public boolean strikethrough;
        /** 水平锚点：0 左 / 1 中 / 2 右。 */
        public int anchorH = 1;
        /** 垂直锚点：0 上 / 1 中 / 2 下。 */
        public int anchorV = 1;
        /** 缩放倍率（默认 0.25）。 */
        public float scale = 0.25F;
    }

    /** 变量表中的一个变量。 */
    public static final class Variable {
        public String name;
        /** FLOAT / BOOL。 */
        public String type;
        /** 是否为圆盘记录仪的推送触发变量。 */
        public boolean trigger;
        /** 绑定组件代号（触发变量写圆盘记录仪的第二名称）；null = 未绑定。 */
        public String component;
        /** 浮点变量值（BOOL 变量不写该字段）。 */
        public Float value;
        /** 布尔变量值（FLOAT 变量不写该字段）。 */
        public Boolean bool;
    }

    /** 辅助计算机程序。 */
    public static final class Program {
        /** 运行间隔（tick，0 = 停止）。 */
        public float period;
        public List<Line> lines = new ArrayList<>();
    }

    /** 一行程序。 */
    public static final class Line {
        /** 行号（0 ~ 63，用于保持空行位置）。 */
        public int row;
        public boolean enabled = true;
        /** ALWAYS / TRUE_VAR / FALSE_VAR（仅在 condition_expr 省略时使用）。 */
        public String cond = "ALWAYS";
        /** 条件变量名（cond 不为 ALWAYS 时有效）。 */
        public String condition;
        /** 新版条件表达式（布尔）；存在时优先于 cond / condition。 */
        public Expr condition_expr;
        /** 条件原文（可省略；为空时按 condition_expr 回显）。 */
        public String condition_source = "";
        /** 赋值目标变量名。 */
        public String target;
        public Expr expr;
        public String comment = "";
        /** 表达式原文（可含换行；为空时按 expr 树回显）。 */
        public String source = "";
    }

    /** 表达式树节点（结构与 ProgramIR.Expr 一一对应，变量用名称引用）。 */
    public static final class Expr {
        /** FLOAT / BOOL / VAR / NEG / NOT / CAST / ADD ... （同 ProgramIR.Op）。 */
        public String op;
        /** FLOAT 节点的值。 */
        public Float number;
        /** BOOL 节点的值。 */
        public Boolean flag;
        /** VAR 节点引用的变量名。 */
        public String var;
        public Expr left;
        public Expr right;
    }

    /** 「保持原样」的颜色常量，与 CompEntry.NO_COLOR 保持一致。 */
    public static final class CompColor {
        public static final int NONE = -1;

        private CompColor() {
        }
    }
}
