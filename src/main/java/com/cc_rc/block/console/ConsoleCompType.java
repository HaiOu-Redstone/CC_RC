package com.cc_rc.block.console;

import com.cc_rc.CcRc;
import com.cc_rc.ModItems;
import java.util.Locale;
import net.minecraft.world.item.Item;

/**
 * 控制台表面可放置的组件类型。
 *
 * 每种类型对应：
 *  - 一个注册物品（见 ModItems）；
 *  - 一份表面模型资源（assets/cc_rc/models/console_parts/&lt;id&gt;_surface.obj）；
 *  - 一张贴图（assets/cc_rc/textures/block/console_parts/&lt;id&gt;.png）。
 *
 * 尺寸取自模型顶点包围盒（单位：结构像素，1 格 = 16 像素），
 * 客户端命中检测与防止重叠都按这个矩形计算。
 */
public enum ConsoleCompType {
    /** 流量仪表（表盘 + 指针）：颜色作用于表盘底色，红色指针保持原色 */
    GAUGE("gauge", "meter", 5.0F, 5.0F, true, 1.2F),
    /** 数字显示屏：颜色暂存，后续用于显示文字颜色（不改模型外观） */
    NUMITRON("numitron", "display", 13.0F, 5.0F, false, 1.0F),
    /** 显示器（图表面板）：模型与数字显示屏一致，仅物品图标不同；显示功能预留 */
    GRAPH("graph", "plotter", 13.0F, 5.0F, false, 1.0F, "numitron"),
    /** 指示灯：颜色即亮灯颜色，熄灯色由亮灯色加深得到 */
    INDICATOR("indicator", "lamp", 7.0F, 4.0F, true, 1.0F),
    /** 按钮面板：颜色作用于中间的按键区（模型缩到原来的一半大小） */
    KEYPAD("keypad", "button", 5.0F, 5.0F, true, 0.5F),
    /** 发信终端：颜色暂存，后续用于显示文字颜色（不改模型外观） */
    TERMINAL("terminal", "terminal", 14.0F, 14.0F, false, 1.0F),
    /** 旋钮开关（默认「关」状态模型：箭头右上 + 红灯；右键切换开 / 关） */
    KNOB_SWITCH("knob_switch", "switch", 14.0F, 14.0F, false, 0.3F),
    /** 拉杆开关（默认「关」状态模型：杆向右倾 + 红灯；右键切换开 / 关） */
    LEVER_SWITCH("lever_switch", "lever", 14.0F, 14.0F, false, 0.3F),
    /** 立式条形仪表（指针取 50 位置；显示功能预留，暂不参与交互；整体放大到约 1.5 倍） */
    GAUGE_V("gauge_v", "gauge", 14.0F, 14.0F, false, 0.45F),
    /** 调节器仪表（弧形表盘 + 双按钮）：左（红）按钮 -1%、右（绿）按钮 +1%（Shift 为 10%），指针随数值转动；
     *  整体放大到约 1.5 倍 */
    GAUGE_ARC("gauge_arc", "knob", 14.0F, 14.0F, false, 0.45F),
    /**
     * RGB 颜色控制器：名称对应一个布尔变量（启用开关），另有 R / G / B 三个浮点变量。
     * 启用时按三个通道的数值给「灯面」上色，并把颜色同步到「连接的组件」；
     * 停用时自身显示原色，连接组件保持最后颜色。有附带文字时隐藏模型、改显示该文字。
     * 默认渲染缩放 0.25（面积为 0.5 时的 1/4），命中框随缩放一起缩小。
     */
    RGB_COLOR("rgb_color", "rgb", 14.0F, 14.0F, false, 0.25F),
    /**
     * 阀门式拉杆（lever_f）：逻辑与拉杆开关相同（右键切换开 / 关），
     * 开 / 关两套模型（把手角度已烘焙进模型），颜色作用于背板红色区域与把手红色部分。
     * 宽度按转换后的模型包围盒取 10.5 像素。
     */
    LEVER_F("lever_f", "lever_f", 10.5F, 14.0F, false, 0.3F),
    /**
     * 旋转按钮（button_x）：逻辑与按钮面板相同（按下后定时 / 松手弹起），
     * 给出的模型是「按下」状态，复位时 Rotate 分组绕面板法线顺时针旋转 90°；
     * 颜色作用于背板黄色区域。默认渲染缩放 0.15（在 0.3 基础上再缩小一半）。
     */
    BUTTON_X("button_x", "button_x", 14.0F, 14.0F, false, 0.15F),
    /**
     * 三状态旋钮开关（t_switch）：浮点变量，state 0/1/2 分别对应 绿 / 橙 / 红 三套模型
     * （三个档位，箭头分别指向左 / 中 / 右灯），右键按 0→1→2→0 循环切换。
     * 颜色作用于背板（o Panel 分组）。
     */
    T_SWITCH("t_switch", "t_switch", 14.0F, 14.0F, false, 0.3F),
    /**
     * 三状态拉杆（t_lever）：逻辑与 t_switch 相同，模型为控制杆（左倾 / 竖直 / 右倾），
     * 颜色作用于背板（o Panel 分组）。
     */
    T_LEVER("t_lever", "t_lever", 14.0F, 14.0F, false, 0.3F),
    /**
     * 双色指示灯（lamp_d）：布尔变量，亮 / 灭通过两套模型切换
     * （state 0 = 红亮绿灭，state 1 = 绿亮红灭），颜色作用于背板（o Panel 分组）。
     */
    LAMP_D("lamp_d", "lamp_d", 14.0F, 14.0F, false, 0.3F),
    /**
     * 扇形刻度仪表（gauge_f）：功能与流量仪表（GAUGE）相同，绑定浮点变量，
     * 指针 0 位于左下 30°、顺时针扫过 240° 到右下 30°（100）。
     * 颜色作用于表盘亮部，黑色指针与刻度保持原色（整图着色时跳过深色像素）。
     */
    GAUGE_F("gauge_f", "gauge_f", 14.0F, 14.0F, true, 0.45F),
    /**
     * 同步指示器（synchroscope）：自身浮点变量表示指针位置（-180~180，正上方 = 0），
     * 附加浮点变量 &lt;代号&gt;_s 表示转速（度/tick，正 = 顺时针、负 = 逆时针）。
     * 成型后指针按转速持续旋转并在 ±180 处环绕；转速绝对值小于配置阈值
     * 且角度在 ±15° 内时切换为「灯亮」模型（state 1），否则为「灯灭」模型（state 0）。
     * 染色沿用扇形仪表的整图重新着色：只染表盘亮部（中间背景），
     * 外框 / 刻度 / 指针均为深色，重新着色时会被跳过。
     */
    SYNCHROSCOPE("synchroscope", "synchroscope", 14.0F, 14.0F, true, 0.45F),
    /**
     * 安全按钮（button_s）：带玻璃罩的按钮，三套模型对应 合上 / 开盖弹起 / 开盖按下。
     * 空手 Shift+右键开 / 关盖；开盖状态下右键按下（弹起时间规则与按钮面板相同）；
     * 盖子可设置自动关闭时间（0 = 不自动关，默认 100 tick）。
     * 绑定布尔变量，表示按钮当前是否按下（与按钮面板一致）。
     * 颜色作用于按钮本体（模型里 o Button 分组标记为 usemtl tint），
     * 面板 / 金属框 / 玻璃罩保持原色。
     */
    BUTTON_S("button_s", "button_s", 14.0F, 14.0F, false, 0.3F),
    /**
     * 钥匙开关（switch_k）：逻辑与旋钮开关相同（右键切换开 / 关），但需要先插入钥匙。
     * 四套模型 = 有无钥匙 × 开 / 关（<code>switch_k[.1.2.3]_surface.obj</code>）。
     * 成型前 Shift+右键可取出一把「控制柜钥匙」（NBT 记录本组件代号）；
     * 成型后默认无钥匙、不能开关：用代号匹配的钥匙右键插入（消耗），空手右键开关，
     * 空手 Shift+右键拔钥匙（受「钥匙模式」限制：开闭 / 常开 / 常闭）。
     * 绑定布尔变量，只看开关状态，与是否插着钥匙无关。
     */
    SWITCH_K("switch_k", "switch_k", 14.0F, 14.0F, false, 0.3F),
    /**
     * 圆盘记录仪 L（graph_l）：独立模型（16×13 结构像素，比圆盘记录仪更高），
     * 右侧多一支「笔」。功能与 GRAPH 完全相同（浮点变量 + 历史采样 + 推送触发名称），
     * 笔只按最新采样值（历史序列 index 0）上下平移；曲线由渲染器绘制，
     * 组件颜色只作用于曲线，模型本体保持原贴图（不参与整图着色）。
     * 追加在枚举末尾，避免改变已有类型的序号（byOrdinal / 网络包按序号传输）。
     */
    GRAPH_L("graph_l", "plotter_l", 16.0F, 13.0F, false, 1.0F, "plotter_l"),
    /**
     * 警报器（alarm）：一块面板上从左到右为 方形灯 / 静音按钮 / 重置按钮。
     * 主变量为布尔「报警」量：false → true 时解除静音、灯闪烁并从组件位置发声；
     * 按静音（中）→ 静音变量 &lt;代号&gt;_Q = true、灯常亮、停止发声；
     * 按重置（右）→ 把自身变量写回 false、灯灭。
     * 六套模型 = 灯灭 / 灯亮 × 无按钮按下 / 静音按下 / 重置按下。
     */
    // 染色只作用于模型里标记 usemtl tint 的背板红色面板（顶点色 + 纯白采样块），
    // 不走整张贴图重新着色，故 recolorable 保持 false
    ALARM("alarm", "alarm", 16.0F, 16.0F, false, 0.45F),
    /**
     * 数字键盘（numpad）：外壳 + 4 位数字屏 + 10 个按键 + 2 个圆钮。
     * 绑定浮点变量，屏幕上方显示该变量的数值（组件颜色用于数字文字颜色）。
     * 空手右键打开输入界面（数字输入框 + 确认 / 放弃），确认后把值写入组件与变量。
     * 追加在枚举末尾，避免改变已有类型的序号（byOrdinal / 网络包按序号传输）。
     */
    NUMPAD("numpad", "numpad", 16.0F, 16.0F, false, 0.5F);

    /** 资源名后缀（全小写；决定表面模型 / 贴图 / 类型翻译键）。 */
    public final String id;
    /** 物品注册名后缀（全小写；同功能组件在不同系列中保持相同后缀）。 */
    public final String itemSuffix;
    /** 组件在表面上的宽度（像素，未缩放）。 */
    public final float widthPx;
    /** 组件在表面上的高度（像素，未缩放）。 */
    public final float heightPx;
    /**
     * 颜色是否通过「重新着色整张贴图」作用于模型外观。
     * false 表示该类型不走整图着色：显示屏 / 发信终端的颜色暂只记录（后续用于文字颜色）；
     * panel_props 系列（旋钮 / 拉杆 / 立式仪表 / 调节器仪表）改走「可染色面 + 顶点色」，
     * 只给底板与底座上色，见 ConsoleBusBER 的 tint 逻辑。
     */
    public final boolean recolorable;
    /** 模型默认缩放倍率（1.0 = 原始大小）；玩家可在组件设置里再乘以自己的缩放。 */
    public final float renderScale;
    /**
     * 模型与贴图共用的资源名（不含后缀），默认与 {@link #id} 相同。
     * 显示器（graph）复用数字显示屏（numitron）的模型与贴图，只换物品图标。
     */
    public final String modelId;

    ConsoleCompType(String id, String itemSuffix, float widthPx,
                    float heightPx, boolean recolorable, float renderScale) {
        this(id, itemSuffix, widthPx, heightPx, recolorable, renderScale, id);
    }

    ConsoleCompType(String id, String itemSuffix, float widthPx,
                    float heightPx, boolean recolorable, float renderScale, String modelId) {
        this.id = id;
        this.itemSuffix = itemSuffix;
        this.widthPx = widthPx;
        this.heightPx = heightPx;
        this.recolorable = recolorable;
        this.renderScale = renderScale;
        this.modelId = modelId;
    }

    /** 类型显示名翻译键。 */
    public String translationKey() {
        return "gui.cc_rc.console.comp." + this.id;
    }

    /** 物品注册名（控制柜组件统一前缀）。 */
    public String itemName() {
        return "controller_" + this.itemSuffix;
    }

    /** 设备注册名（含命名空间），用于界面提示。 */
    public String registryName() {
        return CcRc.MODID + ":" + this.itemName();
    }

    /** 该组件对应的物品（界面图标等）。 */
    public Item item() {
        return switch (this) {
            case GAUGE -> ModItems.CONSOLE_GAUGE.get();
            case NUMITRON -> ModItems.CONSOLE_NUMITRON.get();
            case GRAPH -> ModItems.CONSOLE_GRAPH.get();
            // 圆盘记录仪 L（带笔的独立模型）
            case GRAPH_L -> ModItems.CONSOLE_GRAPH_L.get();
            case INDICATOR -> ModItems.CONSOLE_INDICATOR.get();
            case KEYPAD -> ModItems.CONSOLE_KEYPAD.get();
            case TERMINAL -> ModItems.CONSOLE_TERMINAL.get();
            case KNOB_SWITCH -> ModItems.CONSOLE_KNOB_SWITCH.get();
            case LEVER_SWITCH -> ModItems.CONSOLE_LEVER_SWITCH.get();
            case GAUGE_V -> ModItems.CONSOLE_GAUGE_V.get();
            case GAUGE_ARC -> ModItems.CONSOLE_GAUGE_ARC.get();
            case RGB_COLOR -> ModItems.CONSOLE_RGB.get();
            case LEVER_F -> ModItems.CONSOLE_LEVER_F.get();
            case BUTTON_X -> ModItems.CONSOLE_BUTTON_X.get();
            case T_SWITCH -> ModItems.CONSOLE_T_SWITCH.get();
            case T_LEVER -> ModItems.CONSOLE_T_LEVER.get();
            case LAMP_D -> ModItems.CONSOLE_LAMP_D.get();
            case GAUGE_F -> ModItems.CONSOLE_GAUGE_F.get();
            case SYNCHROSCOPE -> ModItems.CONSOLE_SYNCHROSCOPE.get();
            case BUTTON_S -> ModItems.CONSOLE_BUTTON_S.get();
            // 钥匙开关（switch_k）
            case SWITCH_K -> ModItems.CONSOLE_SWITCH_K.get();
            // 警报器（alarm）
            case ALARM -> ModItems.CONSOLE_ALARM.get();
            // 数字键盘（numpad）
            case NUMPAD -> ModItems.CONSOLE_NUMPAD.get();
        };
    }

    /**
     * 默认组件代号前缀（全大写），直接取物品注册名后缀，
     * 保证放下设备时自动生成的 NBT 名称与当前注册名一致。
     */
    public String codePrefix() {
        return this.itemSuffix.toUpperCase(Locale.ROOT);
    }

    /** 按注册名后缀查找，找不到返回 null（用于旧数据容错）。 */
    public static ConsoleCompType byId(String id) {
        for (ConsoleCompType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }

    /** 按序号查找（越界时退回 GAUGE）。 */
    public static ConsoleCompType byOrdinal(int ordinal) {
        ConsoleCompType[] values = values();
        return values[Math.max(0, Math.min(values.length - 1, ordinal))];
    }

    /**
     * 组件在「组件缩放 = 1.0」时的矩形半宽 / 半高（像素，含模型默认缩放）。
     * 实际判定尺寸还会乘以玩家的组件缩放，见 {@link ConsoleBusBlockEntity.CompEntry#halfWidthPx()}。
     */
    public float halfWidthPx() {
        return this.widthPx * this.renderScale / 2.0F;
    }

    public float halfHeightPx() {
        return this.heightPx * this.renderScale / 2.0F;
    }

    /**
     * 该组件能否作为布尔变量来源 / 目标。
     * 开关、指示灯、按钮面板属于开关量；其余组件不提供布尔语义。
     */
    public boolean supportsBoolVariable() {
        return this == INDICATOR || this == KEYPAD
                || this == KNOB_SWITCH || this == LEVER_SWITCH
                // 阀门式拉杆：与拉杆开关相同的布尔开关量
                || this == LEVER_F
                // 旋转按钮：与按钮面板相同的按压量
                || this == BUTTON_X
                // 安全按钮：与按钮面板相同的按压量（盖子开合属于操作过程，不进入变量）
                || this == BUTTON_S
                // RGB 颜色控制器：主名称对应「启用」布尔开关
                || this == RGB_COLOR
                // 双色指示灯：布尔量，亮 / 灭切换模型
                || this == LAMP_D
                // 钥匙开关：只看开关状态的布尔量（是否插着钥匙不进入变量）
                || this == SWITCH_K
                // 警报器：主变量为布尔「报警」，另有只读布尔 <代号>_Q（静音状态）
                || this == ALARM;
    }

    /**
     * 该组件是否带「钥匙模式」设置（钥匙开关：开闭 / 常开 / 常闭）。
     * 常开只有开时能拔钥匙，常闭只有关时能拔，开闭模式都可以拔。
     */
    public boolean hasKeyMode() {
        return this == SWITCH_K;
    }

    /**
     * 该组件是否带有「静音状态」附加布尔变量（警报器：&lt;代号&gt;_Q）。
     * 该变量是只读输出：按下静音后为 true，新的报警上升沿时自动清回 false。
     */
    public boolean hasMuteVariable() {
        return this == ALARM;
    }

    /**
     * 该组件能否作为浮点变量来源 / 目标。
     * 流量仪表、数字显示屏、立式仪表、调节器仪表属于模拟量；
     * 圆盘记录仪（GRAPH）也按单个浮点值绑定，每次写入时记录一个历史采样点。
     * 三状态开关（T_SWITCH / T_LEVER）用浮点值 0 / 1 / 2 表示三个档位。
     */
    public boolean supportsFloatVariable() {
        return this == GAUGE || this == NUMITRON || this == GRAPH
                // 圆盘记录仪 L：与圆盘记录仪相同的浮点量 + 历史采样
                || this == GRAPH_L
                || this == GAUGE_V || this == GAUGE_ARC || this == GAUGE_F
                || this == T_SWITCH || this == T_LEVER
                // 同步指示器：指针位置为浮点量
                || this == SYNCHROSCOPE
                // 数字键盘：屏幕显示的数值为浮点量
                || this == NUMPAD;
    }

    /**
     * 该组件是否带有「转速」附加变量（同步指示器：&lt;代号&gt;_s，浮点，单位 度/tick）。
     * 附加变量只作为输入，不参与指针位置回写。
     */
    public boolean hasSpeedVariable() {
        return this == SYNCHROSCOPE;
    }

    /**
     * 该组件是否带有「灯亮状态」附加布尔变量（同步指示器：&lt;代号&gt;_b）。
     * 该变量是只读输出：每 tick 由实际判断值覆盖，手动写入会被覆盖。
     */
    public boolean hasLampVariable() {
        return this == SYNCHROSCOPE;
    }

    /** 是否为「三档」开关（state 0/1/2 对应三套模型；写变量时为浮点值）。 */
    public boolean isThreeState() {
        return this == T_SWITCH || this == T_LEVER;
    }

    /**
     * 该组件使用的贴图名（不含后缀）。
     *
     * 三状态旋钮开关、双色指示灯、RGB 颜色控制器的模型都源自 panel_props 的
     * knob_switch 系列（共用 knob_switch.mtl → 同一张调色板贴图），因此共用
     * t_switch.png，避免在资源目录里重复放置完全相同的贴图。
     * 三状态拉杆源自 lever_switch 系列，单独使用 t_lever.png。
     */
    public String textureId() {
        return switch (this) {
            case LAMP_D, RGB_COLOR -> T_SWITCH.modelId;
            default -> this.modelId;
        };
    }

    /** 该组件是否需要维护历史采样序列（圆盘记录仪：每写入一次就移位并记录新值）。 */
    public boolean supportsHistory() {
        // 圆盘记录仪 L 与圆盘记录仪一样维护历史采样（供笔与曲线使用）
        return this == GRAPH || this == GRAPH_L;
    }

    /**
     * 该组件对应的变量类型。
     * 不参与变量系统（发信终端）返回 null。
     */
    public ConsoleBusBlockEntity.VarType variableType() {
        if (supportsBoolVariable()) {
            return ConsoleBusBlockEntity.VarType.BOOL;
        }
        if (supportsFloatVariable()) {
            return ConsoleBusBlockEntity.VarType.FLOAT;
        }
        return null;
    }

    /**
     * 该组件的变量是否允许被程序 / 玩家写回。
     * 按钮面板 / 旋转按钮的 state 是「弹起时刻」，只能读取按下状态，写回没有意义。
     */
    public boolean supportsVariableWrite() {
        return this != KEYPAD && this != BUTTON_X && this != BUTTON_S;
    }
}
