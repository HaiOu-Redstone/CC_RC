package com.cc_rc.gui;

import com.cc_rc.CcRc;
import com.cc_rc.block.console.ConsoleBusBlockEntity;
import com.cc_rc.block.console.ConsoleBusBlockEntity.VarDef;
import com.cc_rc.block.console.ConsoleBusBlockEntity.VarType;
import com.cc_rc.block.console.ProgramVM;
import com.cc_rc.block.console.ConsoleCompType;
import com.cc_rc.block.console.ProgramExprParser;
import com.cc_rc.block.console.ProgramIR;
import com.cc_rc.network.ConsoleProgramPacket;
import com.cc_rc.network.ConsoleVarPacket;
import com.cc_rc.network.ModNetwork;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 控制柜程序界面。
 *
 * 布局：左侧 1/3 是变量表，右侧 2/3 预留给图形化程序（本阶段为空）。
 * 两侧各自滚动；变量表用鼠标滚轮上下滚动，不使用分页。
 *
 * 变量表（本阶段已实现）：
 *  - 每行一个槽位，点击空槽位在该地址创建变量；
 *  - 变量名与设备代号规则一致（字母 / 数字 / 下划线，1~32 位）；
 *  - 输入变量名时按设备代号自动补全，名称与设备对上即自动绑定该设备（类型由设备决定）；
 *  - 点击布尔变量切换开 / 关，点击浮点变量就地输入数值；
 *  - 下方「保存」把整张变量表写回总线，「放弃」丢弃未保存的修改；
 *  - Esc 关闭界面等同保存。
 *
 * 数据全部保存在总线方块实体，界面直接读取；操作通过 {@link ConsoleVarPacket} 提交。
 */
public class ConsoleProgramScreen extends AbstractContainerScreen<ConsoleProgramMenu> {

    private static final int GUI_W = 416;
    /** 界面高度：左右各容纳 8 行（变量行 / 程序行高度均为 24），底部留出保存栏与警告行。 */
    private static final int GUI_H = 260;
    /** 左右外边距（与上下留白保持接近，避免内容贴边）。 */
    private static final int PAD_X = 14;
    /** 左侧变量表宽度（含左侧设备图标列）。 */
    private static final int LEFT_W = 172;
    /** 变量行左侧的设备图标列宽（16×16 图标 + 边距，只画图标不画物品栏底板）。 */
    private static final int ICON_COL_W = 18;
    /** 变量列表可视区域：每页 8 行。 */
    private static final int LIST_X = PAD_X;
    private static final int LIST_Y = 18;
    private static final int LIST_W = LEFT_W - LIST_X - 4;
    private static final int LIST_H = 192;
    private static final int ROW_H = 24;
    /** 变量表每页行数（按可视高度取整）。 */
    private static final int VAR_ROWS_PER_PAGE = LIST_H / ROW_H;
    /** 底部「保存 / 放弃」按钮。 */
    private static final int SAVE_X = PAD_X;
    private static final int BTN_Y = 224;
    private static final int BTN_W = 40;
    private static final int BTN_H = 20;
    /** 删除按钮热区宽度（行右侧）。 */
    private static final int DELETE_W = 13;

    // ---------------------------------------------------------------- 右侧程序区

    /** 右侧面板左边界（相对 GUI）与宽度；与左侧栏之间留出较大间距。 */
    private static final int RIGHT_X = LEFT_W + 14;
    private static final int RIGHT_W = GUI_W - RIGHT_X - PAD_X;
    /** 每页行数与行高（每行上方表达式、下方注释）。 */
    private static final int ROWS_PER_PAGE = 8;
    private static final int LINE_H = 24;
    private static final int PROG_LIST_Y = 18;
    private static final int PROG_LIST_H = 192;
    /** 底部翻页 / 保存栏。 */
    private static final int PROG_BAR_Y = 224;
    private static final int PROG_BAR_H = 20;
    private static final int PAGE_BTN_W = 20;
    private static final int PROG_SAVE_W = 56;
    /** 行右侧「移动」热区宽度；删除行改到行编辑器里，不再占用列表右侧。 */
    private static final int LINE_MOVE_W = 14;
    /** 变量翻页箭头：位于左下「保存 / 放弃」按钮右侧。 */
    private static final int VAR_PREV_X = SAVE_X + (BTN_W + 8) * 2 + 8;
    private static final int VAR_NEXT_X = VAR_PREV_X + PAGE_BTN_W + 4;
    /** 程序翻页箭头（位于右侧面板底部左侧）。 */
    private static final int PROG_PREV_X = RIGHT_X + 4;
    private static final int PROG_NEXT_X = PROG_PREV_X + PAGE_BTN_W + 34;
    /** 行编辑器控件相对 GUI 顶部的 y（左侧两字标签，右侧输入框）。 */
    private static final int EDIT_ENABLE_Y = 24;
    private static final int EDIT_COND_Y = 44;
    private static final int EDIT_TARGET_Y = 64;
    /** 表达式输入框的 Y / 高度（多行文本，可上下滚动）。 */
    private static final int EDIT_EXPR_Y = 88;
    private static final int EDIT_EXPR_H = 76;
    /** 注释输入框（表达式行下方，不参与编译）。 */
    private static final int EDIT_COMMENT_Y = 172;
    /** 两个操作按钮（确定 / 取消）纵向排列在表达式输入框左侧，避开各行标签。 */
    private static final int EDIT_BTN_Y = 108;
    private static final int EDIT_BTN_STEP = 22;
    private static final int EDIT_BTN_H = 18;
    private static final int EDIT_MSG_Y = 188;
    private static final int EDIT_BTN_W = 44;
    /** 表达式输入框相对右侧面板的左缩进（让出左侧按钮列并留出间距）。 */
    private static final int EDIT_FIELD_INSET = EDIT_BTN_W + 6;
    /** 行内输入框高度与左侧标签宽度。 */
    private static final int EDIT_FIELD_H = 14;
    private static final int EDIT_LABEL_W = 28;
    /** 行编辑器左右内边距：所有控件离面板凹槽边缘留出空隙，避免贴边。 */
    private static final int EDIT_PAD_X = 7;

    /** 新建变量表单内部各控件相对面板顶部的 y 偏移（整体放宽）。 */
    private static final int FORM_TITLE_Y = 36;
    private static final int FORM_LABEL_Y = 58;
    private static final int FORM_NAME_Y = 70;
    private static final int FORM_NAME_H = 16;
    /** 提示区：名称框下方、类型按钮上方的空白（平常不显示，仅报错 / 绑定提示时显示）。 */
    private static final int FORM_MSG_Y = 94;
    /** 提示区最多显示行数（每行 10 像素）。 */
    private static final int FORM_MSG_LINES = 3;
    private static final int FORM_TYPE_Y = 140;
    private static final int FORM_BTN_Y = 186;

    /** 自动补全下拉：单行高度与最多同时显示行数。 */
    private static final int SUGGEST_ROW_H = 12;
    private static final int SUGGEST_ROWS = 5;
    /** 下拉配色（仿原版指令补全：深色底、悬停行黄色文字）。 */
    private static final int COLOR_SUGGEST_BG = 0xFF141414;
    private static final int COLOR_SUGGEST_BORDER = 0xFF000000;
    private static final int COLOR_SUGGEST_TEXT = 0xFFCFCFCF;
    private static final int COLOR_SUGGEST_HOVER = 0xFFFFE060;
    private static final int COLOR_SUGGEST_HOVER_BG = 0xFF3A3A6A;

    private static final ResourceLocation BG =
            new ResourceLocation(CcRc.MODID, "textures/gui/console_program_gui.png");
    /**
     * 翻页箭头贴图（64×64，四象限各 32×32）：
     * 左上 = 向前翻页常态，左下 = 向前翻页悬停，右上 = 向后翻页常态，右下 = 向后翻页悬停。
     */
    private static final ResourceLocation ARROWS =
            new ResourceLocation(CcRc.MODID, "textures/gui/page_arrows.png");
    /** 翻页贴图单象限边长（贴图总宽高 64）。 */
    private static final int ARROW_TEX = 32;
    private static final int ARROW_TEX_SIZE = 64;
    /** 单象限内箭头实际包围盒：14×22，左箭头从象限内 x=9 起、右箭头从 x=10 起，y 均从 6 起。 */
    private static final int ARROW_W = 14;
    private static final int ARROW_H = 22;
    private static final int ARROW_TOP = 6;
    private static final int ARROW_LEFT_INSET = 9;
    private static final int ARROW_RIGHT_INSET = 10;

    private static final int COLOR_TEXT = 0xFF303030;
    private static final int COLOR_DIM = 0xFF666666;
    private static final int COLOR_ERROR = 0xFFB02020;
    /** 列表区域是原版风格的深色凹槽：行底色与浅色文字单独一套配色。 */
    private static final int COLOR_LIST_BG = 0xFF141414;
    private static final int COLOR_LIST_HIGHLIGHT = 0xFF4A4A4A;
    private static final int COLOR_TEXT_LIGHT = 0xFFE0E0E0;
    private static final int COLOR_DIM_LIGHT = 0xFF9A9A9A;
    private static final int COLOR_ERROR_LIGHT = 0xFFFF6B6B;
    private static final int COLOR_ROW = 0xFF232323;
    private static final int COLOR_ROW_HOVER = 0xFF2F2F2F;
    private static final int COLOR_ROW_EMPTY = 0xFF1B1B1B;
    private static final int COLOR_ROW_EMPTY_HOVER = 0xFF262626;
    /** 移动模式：源行底色与「可放置」提示文字的高亮色。 */
    private static final int COLOR_ROW_MOVE = 0xFF3A3A22;
    private static final int COLOR_MOVE_TEXT = 0xFFFFE060;

    // ---------------------------------------------------------------- 语法着色
    // 变量表与程序列表都改为原版风格的深色凹槽，统一使用亮色文字。
    // 同一套内：绑定设备的变量用较深一档，未绑定的变量用较浅一档。
    /** 深色底配色：列表 / 输入框 / 悬浮提示共用。 */
    private static final int COLOR_EDIT_TEXT = 0xFFE0E0E0;
    private static final int COLOR_EDIT_BOOL = 0xFFA8CCFF;
    private static final int COLOR_EDIT_BOOL_BOUND = 0xFF7FB4FF;
    private static final int COLOR_EDIT_FLOAT = 0xFFA8F0B0;
    private static final int COLOR_EDIT_FLOAT_BOUND = 0xFF7FE08A;
    private static final int COLOR_EDIT_CONST = 0xFFFFE060;
    private static final int COLOR_EDIT_OPERATOR = 0xFFFF66D9;
    private static final int COLOR_EDIT_COMMENT = 0xFF9A9A9A;

    /** 新建变量名允许的字符集与长度：不能数字开头，不能与 true / false 常量重名。 */
    private static final Pattern VAR_NAME_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,31}");

    /** 列表滚动量（像素，按页对齐）。 */
    private float scroll;
    /** 变量表当前页（滚轮与翻页按钮均按整页切换）。 */
    private int varPage;
    private Button varPrevButton;
    private Button varNextButton;
    /** 正在编辑数值的槽位；-1 表示没有。 */
    private int editingSlot = -1;
    private EditBox valueBox;

    /** 新建变量表单：槽位；-1 表示表单关闭。 */
    private int createSlot = -1;
    private EditBox nameBox;
    private Button typeButton;
    private Button confirmButton;
    private Button cancelButton;
    private Button saveButton;
    private Button discardButton;
    private String formError = "";
    private VarType createType = VarType.FLOAT;
    /** 名称输入框下方的设备名候选（自动补全）。 */
    private final List<DeviceOption> suggestions = new ArrayList<>();
    /** 自动补全下拉是否展开（点击输入框或输入文字时展开）。 */
    private boolean suggestOpen = false;
    /** 自动补全下拉的滚动行偏移。 */
    private int suggestOffset = 0;
    /** 点「放弃」时置 true，跳过 onClose 里的自动保存。 */
    private boolean discarding = false;

    // ---------------------------------------------------------------- 右侧程序区状态

    /** 当前页码（0 起，共 8 页）。 */
    private int page = 0;
    /** 「移动」待放置的源行号；-1 表示未处于移动模式。 */
    private int moveSource = -1;
    /** 「复制」待放置的整行副本；null 表示未处于复制放置模式。 */
    private ProgramIR.Line pendingCopy;
    private Button prevPageButton;
    private Button nextPageButton;
    private Button progSaveButton;
    /** 行编辑器全部按钮（整体显示 / 隐藏）。 */
    private final List<Button> lineEditorButtons = new ArrayList<>();
    /** 行编辑器全部输入框（与变量名称框同样式）。 */
    private final List<EditBox> lineEditorBoxes = new ArrayList<>();
    private Button enableButton;
    private Button lineOkButton;
    private Button lineCancelButton;
    /** 「复制」：把当前行整行收进待放置状态，关闭编辑器后由玩家点空行放置。 */
    private Button copyLineButton;
    /** 「删除」：删除当前编辑的行并关闭编辑器。 */
    private Button deleteLineButton;
    /** 条件表达式输入框（单行；留空表示总是执行）。 */
    private ExprEditBox condBox;
    private EditBox targetBox;
    private EditBox commentBox;
    /** 右值表达式输入框（多行文本，换行不影响语义）。 */
    private ExprEditBox exprBox;

    /** 行编辑器自动补全：下拉所属输入框；null 表示未展开。 */
    private EditBox fieldSuggestBox;
    /** 行编辑器自动补全候选（变量名 / 算符 / 常量）。 */
    private final List<String> fieldSuggestions = new ArrayList<>();
    /** 行编辑器自动补全的滚动行偏移。 */
    private int fieldSuggestOffset = 0;
    /** 输入框 → 候选来源（变量名 / 算符 / 常量）。 */
    private final Map<EditBox, Supplier<List<String>>> fieldSuggestSources = new HashMap<>();

    /** 正在编辑的行号；-1 表示列表模式。 */
    private int editingLine = -1;
    private boolean editEnabled = true;
    /** 正在把已有行载入输入框：抑制中途的即时校验提示。 */
    private boolean loadingLine = false;
    /** 行编辑器内的提示 / 报错文本。 */
    private String lineError = "";

    /** 本地待提交数值：保存成功后由总线同步值覆盖（避免保存后闪烁回旧值）。 */
    private final Map<Integer, Float> pendingFloats = new HashMap<>();
    private final Map<Integer, Boolean> pendingBools = new HashMap<>();

    public ConsoleProgramScreen(ConsoleProgramMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.translatable("gui.cc_rc.program.title"));
        this.imageWidth = GUI_W;
        this.imageHeight = GUI_H;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.leftPos;
        int cy = this.topPos;

        // 保存变量数值并关闭界面（onClose 内部会再保存一次变量表，故直接走 onClose）
        this.saveButton = vanillaButton(cx + SAVE_X, cy + BTN_Y, BTN_W, BTN_H,
                Component.translatable("gui.cc_rc.program.save"), b -> this.onClose());
        this.addRenderableWidget(this.saveButton);

        // 放弃未保存的修改并关闭界面
        this.discardButton = vanillaButton(cx + SAVE_X + BTN_W + 8, cy + BTN_Y, BTN_W, BTN_H,
                Component.translatable("gui.cc_rc.program.discard"), b -> this.discardChanges());
        this.addRenderableWidget(this.discardButton);

        // 单行数值输入框：点击浮点变量时移动到该行
        this.valueBox = new EditBox(this.font, 0, 0, LIST_W - 4 - ICON_COL_W, 12,
                Component.literal("value"));
        this.valueBox.setMaxLength(16);
        this.valueBox.setVisible(false);
        this.addWidget(this.valueBox);

        // 新建变量表单控件（默认隐藏；左右各留出与列表一致的内边距）
        int formX = cx + LIST_X + 8;
        this.nameBox = new EditBox(this.font, formX, cy + FORM_NAME_Y, LIST_W - 16, FORM_NAME_H,
                Component.translatable("gui.cc_rc.program.name"));
        this.nameBox.setMaxLength(32);
        this.nameBox.setVisible(false);
        // 每次输入都刷新候选列表与类型按钮，并展开补全下拉
        this.nameBox.setResponder(value -> {
            // 重新输入即清除上次的报错，并展开补全下拉
            this.formError = "";
            this.suggestOpen = true;
            this.suggestOffset = 0;
            this.updateFormWidgets();
        });
        this.addWidget(this.nameBox);

        this.typeButton = vanillaButton(formX, cy + FORM_TYPE_Y, LIST_W - 16, 18,
                Component.literal(""), b -> {
                    this.createType = this.createType == VarType.BOOL
                            ? VarType.FLOAT : VarType.BOOL;
                    this.updateFormWidgets();
                });
        this.typeButton.visible = false;
        this.addRenderableWidget(this.typeButton);

        this.confirmButton = vanillaButton(formX + 24, cy + FORM_BTN_Y, BTN_W, BTN_H,
                Component.translatable("gui.cc_rc.program.create"), b -> this.confirmCreate());
        this.confirmButton.visible = false;
        this.addRenderableWidget(this.confirmButton);

        this.cancelButton = vanillaButton(formX + 24 + BTN_W + 10, cy + FORM_BTN_Y, BTN_W, BTN_H,
                Component.translatable("gui.cc_rc.program.cancel"), b -> this.closeCreate());
        this.cancelButton.visible = false;
        this.addRenderableWidget(this.cancelButton);

        // ------------------------------------------------ 右侧程序区控件
        int rx = cx + RIGHT_X;
        int barY = cy + PROG_BAR_Y;

        // 变量表翻页箭头（底部，位于程序翻页按钮左侧）
        this.varPrevButton = arrowButton(cx + VAR_PREV_X, barY, PAGE_BTN_W, PROG_BAR_H,
                true, b -> this.changeVarPage(-1));
        this.addRenderableWidget(this.varPrevButton);
        this.varNextButton = arrowButton(cx + VAR_NEXT_X, barY, PAGE_BTN_W, PROG_BAR_H,
                false, b -> this.changeVarPage(1));
        this.addRenderableWidget(this.varNextButton);

        // 上一页 / 下一页
        this.prevPageButton = arrowButton(cx + PROG_PREV_X, barY, PAGE_BTN_W, PROG_BAR_H,
                true, b -> this.changePage(-1));
        this.addRenderableWidget(this.prevPageButton);
        this.nextPageButton = arrowButton(cx + PROG_NEXT_X, barY, PAGE_BTN_W, PROG_BAR_H,
                false, b -> this.changePage(1));
        this.addRenderableWidget(this.nextPageButton);

        // 保存程序并关闭界面
        this.progSaveButton = vanillaButton(cx + GUI_W - PAD_X - PROG_SAVE_W, barY, PROG_SAVE_W, PROG_BAR_H,
                Component.translatable("gui.cc_rc.program.save"), b -> {
                    if (this.saveProgram()) {
                        this.onClose();
                    }
                });
        this.addRenderableWidget(this.progSaveButton);

        // 行编辑器：启用 / 条件 / 目标 / 算式（每行算符 + 操作数）/ 追加 / 确定 / 取消
        this.lineEditorButtons.clear();
        this.lineEditorBoxes.clear();
        this.fieldSuggestSources.clear();
        this.closeFieldSuggest();
        int editX = rx + EDIT_PAD_X;
        int editRight = rx + RIGHT_W - EDIT_PAD_X;
        // 顶部「启用」开关：右侧让出「复制」「删除」两个按钮的位置
        int delBtnX = editRight - EDIT_BTN_W;
        int copyBtnX = delBtnX - 6 - EDIT_BTN_W;
        this.enableButton = vanillaButton(editX, cy + EDIT_ENABLE_Y,
                copyBtnX - 6 - editX,
                EDIT_FIELD_H,
                Component.literal(""), b -> {
                    this.updateLineWidgets();
                });
        this.registerLineEditor(this.enableButton);

        // 复制当前行：把整行收进待放置状态，关闭编辑器后在列表里点空行放置
        this.copyLineButton = vanillaButton(copyBtnX, cy + EDIT_ENABLE_Y,
                EDIT_BTN_W, EDIT_FIELD_H,
                Component.translatable("gui.cc_rc.program.copy"), b -> this.copyLineForward());
        this.registerLineEditor(this.copyLineButton);

        // 删除当前行：提交后关闭编辑器（列表里不再提供 × 热区）
        this.deleteLineButton = vanillaButton(delBtnX, cy + EDIT_ENABLE_Y,
                EDIT_BTN_W, EDIT_FIELD_H,
                Component.translatable("gui.cc_rc.program.delete"), b -> this.deleteEditingLine());
        this.registerLineEditor(this.deleteLineButton);

        // 条件输入框：单行表达式，留空表示总是执行
        int fieldX = editX + EDIT_LABEL_W;
        int fieldW = editRight - fieldX;
        this.condBox = new ExprEditBox(this.font, fieldX, cy + EDIT_COND_Y,
                fieldW, EDIT_FIELD_H);
        this.condBox.setSingleLine(true);
        this.condBox.setCharacterLimit(ProgramIR.Line.MAX_SOURCE);
        this.condBox.setHint(Component.translatable("gui.cc_rc.program.hint_cond"));
        this.condBox.setNameColor(this::exprNameColor);
        this.condBox.setCandidates(this::exprCandidates);
        this.condBox.setOnChanged(() -> {
            if (!this.loadingLine) {
                this.onEditorChanged();
            }
        });
        this.condBox.visible = false;
        this.addRenderableWidget(this.condBox);

        this.targetBox = new EditBox(this.font, fieldX, cy + EDIT_TARGET_Y,
                fieldW, EDIT_FIELD_H, Component.translatable("gui.cc_rc.program.hint_var"));
        this.targetBox.setMaxLength(32);
        this.targetBox.setHint(Component.translatable("gui.cc_rc.program.hint_var"));
        this.bindSuggest(this.targetBox, () -> this.variableNames(false, true));
        this.addWidget(this.targetBox);

        this.lineEditorBoxes.add(this.targetBox);

        // 注释输入框：不参与编译与校验，仅在程序列表中显示于表达式下方
        this.commentBox = new EditBox(this.font, fieldX, cy + EDIT_COMMENT_Y,
                fieldW, EDIT_FIELD_H, Component.translatable("gui.cc_rc.program.hint_comment"));
        this.commentBox.setMaxLength(80);
        this.commentBox.setHint(Component.translatable("gui.cc_rc.program.hint_comment"));
        // 注释整段灰色，直接使用输入框自身的文字颜色
        this.commentBox.setTextColor(COLOR_EDIT_COMMENT);
        this.addWidget(this.commentBox);
        this.lineEditorBoxes.add(this.commentBox);

        // 表达式输入框：多行文本，换行只作原文记录，不参与语义。
        // 左侧留出按钮列宽度，确定 / 取消纵向排列，避免补全下拉覆盖按钮。
        int exprX = editX + EDIT_FIELD_INSET;
        this.exprBox = new ExprEditBox(this.font, exprX, cy + EDIT_EXPR_Y,
                editRight - exprX, EDIT_EXPR_H);
        this.exprBox.setCharacterLimit(ProgramIR.Line.MAX_SOURCE);
        this.exprBox.setHint(Component.translatable("gui.cc_rc.program.hint_operand"));
        this.exprBox.setNameColor(this::exprNameColor);
        this.exprBox.setCandidates(this::exprCandidates);
        this.exprBox.setOnChanged(() -> {
            if (!this.loadingLine) {
                this.onEditorChanged();
            }
        });
        this.exprBox.visible = false;
        this.addRenderableWidget(this.exprBox);

        // 两个按钮纵向排列在「算式」标签下方左侧
        int btnY = cy + EDIT_BTN_Y;
        this.lineOkButton = vanillaButton(editX, btnY, EDIT_BTN_W, EDIT_BTN_H,
                Component.translatable("gui.cc_rc.program.ok"), b -> this.confirmLine());
        this.registerLineEditor(this.lineOkButton);

        this.lineCancelButton = vanillaButton(editX, btnY + EDIT_BTN_STEP, EDIT_BTN_W, EDIT_BTN_H,
                Component.translatable("gui.cc_rc.program.cancel"), b -> this.closeLineEditor());
        this.registerLineEditor(this.lineCancelButton);

        this.setLineEditorVisible(false);
        this.clampScroll();
    }

    /** 登记行编辑器控件并默认隐藏。 */
    private void registerLineEditor(Button button) {
        button.visible = false;
        this.lineEditorButtons.add(button);
        this.addRenderableWidget(button);
    }

    // ---------------------------------------------------------------- 数据读取

    /** 当前总线方块实体；界面打开期间可能因为区块卸载返回 null。 */
    private ConsoleBusBlockEntity bus() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        BlockEntity be = this.minecraft.level.getBlockEntity(this.menu.pos);
        return be instanceof ConsoleBusBlockEntity found ? found : null;
    }

    private boolean locked(ConsoleBusBlockEntity bus) {
        return bus != null && bus.getFormState() == ConsoleBusBlockEntity.State.FULL;
    }

    private boolean boolValue(int slot, ConsoleBusBlockEntity bus) {
        Boolean pending = this.pendingBools.get(slot);
        return pending != null ? pending : bus.getVarBool(slot);
    }

    private float floatValue(int slot, ConsoleBusBlockEntity bus) {
        Float pending = this.pendingFloats.get(slot);
        return pending != null ? pending : bus.getVarFloat(slot);
    }

    /**
     * 设备名称候选：一个设备可能提供两个可绑定名称。
     * 圆盘记录仪的 code2 为「推送触发」布尔名称，其余设备的第二项为空。
     */
    private record DeviceOption(ConsoleBusBlockEntity.CompEntry comp, String name,
                                VarType type, boolean trigger) {
    }

    /** 可以绑定为变量的设备名称（排除已绑定变量的名称）。 */
    private List<DeviceOption> devices() {
        List<DeviceOption> result = new ArrayList<>();
        ConsoleBusBlockEntity bus = bus();
        if (bus == null) {
            return result;
        }
        for (ConsoleBusBlockEntity.CompEntry comp : bus.getComps()) {
            VarType type = comp.type.variableType();
            // RGB 颜色控制器允许无名称（未命名时不显示模型），空代号不进入变量候选
            if (type != null && !comp.code.isEmpty() && bus.findVarSlot(comp.code) < 0) {
                result.add(new DeviceOption(comp, comp.code, type, false));
            }
            // 圆盘记录仪的第二名称：绑定的是「本 tick 是否记录一笔」的布尔触发变量
            if (comp.type.supportsHistory() && !comp.code2.isEmpty()
                    && bus.findVarSlot(comp.code2) < 0) {
                result.add(new DeviceOption(comp, comp.code2, VarType.BOOL, true));
            }
            // 同步指示器：转速附加变量（代号_s，浮点）与灯状态附加变量（代号_b，布尔）
            if (comp.type.hasSpeedVariable() && !comp.code.isEmpty()
                    && bus.findVarSlot(comp.speedVarName()) < 0) {
                result.add(new DeviceOption(comp, comp.speedVarName(), VarType.FLOAT, false));
            }
            if (comp.type.hasLampVariable() && !comp.code.isEmpty()
                    && bus.findVarSlot(comp.lampVarName()) < 0) {
                result.add(new DeviceOption(comp, comp.lampVarName(), VarType.BOOL, false));
            }
            // 警报器：静音状态附加变量（代号_Q，布尔，只读输出）
            if (comp.type.hasMuteVariable() && !comp.code.isEmpty()
                    && bus.findVarSlot(comp.muteVarName()) < 0) {
                result.add(new DeviceOption(comp, comp.muteVarName(), VarType.BOOL, false));
            }
            // RGB 颜色控制器的 R / G / B 通道名 → 浮点变量候选
            if (comp.type == ConsoleCompType.RGB_COLOR) {
                for (String channel : new String[]{comp.rgbR, comp.rgbG, comp.rgbB}) {
                    if (!channel.isEmpty() && bus.findVarSlot(channel) < 0) {
                        result.add(new DeviceOption(comp, channel, VarType.FLOAT, false));
                    }
                }
            }
        }
        return result;
    }

    // ---------------------------------------------------------------- 滚动

    private void clampScroll() {
        // 变量表按整页翻动：页数按槽位总数向上取整
        int pages = Math.max(1, (ConsoleBusBlockEntity.varSlots() + VAR_ROWS_PER_PAGE - 1)
                / VAR_ROWS_PER_PAGE);
        this.varPage = Math.max(0, Math.min(pages - 1, this.varPage));
        // 行像素偏移始终与页对齐，保证每页第一行完整显示
        this.scroll = (float) this.varPage * VAR_ROWS_PER_PAGE * ROW_H;
    }

    /** 变量表翻页（滚轮 / 按钮共用）。 */
    private void changeVarPage(int delta) {
        this.varPage += delta;
        this.clampScroll();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 行编辑器的自动补全下拉优先滚动
        if (this.isInsideFieldSuggest(mouseX, mouseY)) {
            int shown = this.fieldSuggestRowCount();
            int max = Math.max(0, this.fieldSuggestions.size() - shown);
            this.fieldSuggestOffset = Math.max(0, Math.min(max,
                    this.fieldSuggestOffset + (delta > 0 ? -1 : 1)));
            return true;
        }
        // 新建表单的补全下拉优先滚动
        if (this.createSlot >= 0 && this.suggestOpen
                && this.isInsideSuggest(mouseX, mouseY)) {
            int max = Math.max(0, this.suggestions.size() - SUGGEST_ROWS);
            this.suggestOffset = Math.max(0, Math.min(max,
                    this.suggestOffset + (delta > 0 ? -1 : 1)));
            return true;
        }
        // 多行表达式框：滚轮上下滚动显示行
        if (this.exprBox != null && this.exprBox.visible
                && this.exprBox.isMouseOver(mouseX, mouseY)) {
            this.exprBox.scroll(delta > 0 ? -1 : 1);
            return true;
        }
        // 左侧变量表：滚轮整页翻动
        if (this.createSlot < 0
                && mouseX >= this.leftPos + LIST_X && mouseX < this.leftPos + LIST_X + LIST_W
                && mouseY >= this.topPos + LIST_Y && mouseY < this.topPos + LIST_Y + LIST_H) {
            this.changeVarPage(delta > 0 ? -1 : 1);
            return true;
        }
        // 右侧程序列表：滚轮同样整页翻动（行编辑器打开时列表不显示）
        if (this.editingLine < 0
                && mouseX >= this.leftPos + RIGHT_X
                && mouseX < this.leftPos + RIGHT_X + RIGHT_W
                && mouseY >= this.topPos + PROG_LIST_Y
                && mouseY < this.topPos + PROG_LIST_Y + PROG_LIST_H) {
            this.changePage(delta > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    // ---------------------------------------------------------------- 行命中

    /** 列表第 index 行的屏幕 y 坐标（已含滚动）。 */
    private int rowTop(int index) {
        return this.topPos + LIST_Y + index * ROW_H - Math.round(this.scroll);
    }

    /** 鼠标命中的行下标（下标即槽位号，0 号为运行间隔）；不在列表内返回 -1。 */
    private int rowAt(double mouseX, double mouseY) {
        if (mouseX < this.leftPos + LIST_X || mouseX >= this.leftPos + LIST_X + LIST_W
                || mouseY < this.topPos + LIST_Y || mouseY >= this.topPos + LIST_Y + LIST_H) {
            return -1;
        }
        int rel = (int) (mouseY - (this.topPos + LIST_Y) + Math.round(this.scroll));
        if (rel < 0) {
            return -1;
        }
        int index = rel / ROW_H;
        if (index >= ConsoleBusBlockEntity.varSlots()) {
            return -1;
        }
        return index;
    }

    // ---------------------------------------------------------------- 交互

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 点击数值输入框之外时提交当前编辑
        if (this.valueBox.isVisible() && !this.valueBox.isMouseOver(mouseX, mouseY)) {
            this.commitValueEdit();
        }

        // 行编辑器：自动补全候选点击优先；点击变量 / 算符框时展开下拉
        // 变量表单打开时也允许继续操作右侧行编辑器（两个面板互不锁定）
        if (button == 0 && this.editingLine >= 0) {
            // 条件框的补全下拉优先响应点击
            if (this.condBox != null && this.condBox.visible) {
                if (this.condBox.mouseClickedSuggestion(mouseX, mouseY)) {
                    this.setFocused(this.condBox);
                    return true;
                }
                // 点击条件框以外的地方：收起它的候选
                if (!this.condBox.isMouseOver(mouseX, mouseY)) {
                    this.condBox.closeSuggestions();
                }
            }
            // 表达式框的补全下拉优先响应点击
            if (this.exprBox != null && this.exprBox.visible) {
                if (this.exprBox.mouseClickedSuggestion(mouseX, mouseY)) {
                    this.setFocused(this.exprBox);
                    return true;
                }
                // 点击表达式框以外的地方：收起它的候选
                if (!this.exprBox.isMouseOver(mouseX, mouseY)) {
                    this.exprBox.closeSuggestions();
                }
            }
            int picked = this.fieldSuggestionAt(mouseX, mouseY);
            if (picked >= 0) {
                this.applyFieldSuggestion(picked);
                return true;
            }
            EditBox box = this.suggestBoxAt(mouseX, mouseY);
            if (box != null) {
                // 手动转交点击，保证光标位置与聚焦正确
                box.mouseClicked(mouseX, mouseY, button);
                this.setFocused(box);
                this.openFieldSuggest(box);
                return true;
            }
            // 下拉已展开时，吞掉底板内（边框 / 滚动条 / 被遮挡输入框）的点击
            if (this.isInsideFieldSuggest(mouseX, mouseY)) {
                return true;
            }
            // 点击别处：先收起下拉，其余交给常规处理
            this.closeFieldSuggest();
        }

        // 新建表单打开时：处理补全下拉的展开 / 选择 / 收起
        if (button == 0 && this.createSlot >= 0) {
            if (this.nameBox.isMouseOver(mouseX, mouseY)) {
                // 点击名称框即展开全部设备候选（即使什么都没输入）
                this.suggestOpen = true;
                this.suggestOffset = 0;
                this.syncTypeButtonVisibility();
            } else if (this.suggestOpen) {
                int index = this.suggestionAt(mouseX, mouseY);
                if (index >= 0) {
                    this.nameBox.setValue(this.suggestions.get(index).name());
                    this.nameBox.setFocused(true);
                    this.setFocused(this.nameBox);
                    return true;
                }
                boolean inList = this.isInsideSuggest(mouseX, mouseY);
                this.suggestOpen = false;
                this.syncTypeButtonVisibility();
                if (inList) {
                    return true;
                }
            }
        }

        if (button == 0 && this.createSlot < 0) {
            int index = this.rowAt(mouseX, mouseY);
            if (index >= 0) {
                int slot = index;
                ConsoleBusBlockEntity bus = bus();
                if (bus != null) {
                    if (this.locked(bus)) {
                        this.localHint("message.cc_rc.controller.var_locked");
                        return true;
                    }
                    // 0 号槽位：运行间隔，只能改值（不能删除 / 改名 / 改类型）
                    if (slot == ConsoleBusBlockEntity.VAR_SLOT_PERIOD) {
                        this.beginValueEdit(slot, index, bus);
                        return true;
                    }
                    VarDef def = bus.getVarDef(slot);
                    int top = rowTop(index);
                    int rowX = this.leftPos + LIST_X;
                    boolean deleteHit = mouseX >= rowX + LIST_W - DELETE_W
                            && mouseY < top + 12;
                    if (def == null) {
                        this.openCreate(slot);
                        return true;
                    }
                    if (deleteHit) {
                        this.deleteVar(slot);
                        return true;
                    }
                    if (def.type == VarType.BOOL) {
                        this.pendingBools.put(slot, !boolValue(slot, bus));
                        return true;
                    }
                    this.beginValueEdit(slot, index, bus);
                    return true;
                }
            }
        }
        // 右侧程序区：勾选启用 / 点击行编辑 / 点「移」进入移动模式
        // 变量表单打开时也允许操作程序行（两个面板互不锁定）
        if (button == 0 && this.editingLine < 0) {
            int index = programLineAt(mouseX, mouseY);
            if (index >= 0) {
                ConsoleBusBlockEntity bus = bus();
                if (bus != null) {
                    if (this.locked(bus)) {
                        this.localHint("message.cc_rc.controller.prog_locked");
                        return true;
                    }
                    ProgramIR.Line line = bus.getProgramLine(index);
                    int rowX = this.leftPos + RIGHT_X;
                    // 行右侧「移」热区（删除行已移到行编辑器里）
                    boolean moveHit = mouseX >= rowX + RIGHT_W - LINE_MOVE_W;
                    // 移动模式：点空行放置；点其它行的「移」换源行；点别处取消
                    if (this.moveSource >= 0) {
                        if (line == null) {
                            this.moveLine(this.moveSource, index);
                            return true;
                        }
                        this.moveSource = moveHit ? index : -1;
                        return true;
                    }
                    // 复制放置模式：点空行粘贴副本，点别处取消
                    if (this.pendingCopy != null) {
                        if (line == null) {
                            this.pasteLine(index);
                        } else {
                            this.pendingCopy = null;
                        }
                        return true;
                    }
                    if (line == null) {
                        this.openLineEditor(index);
                        return true;
                    }
                    if (moveHit) {
                        // 进入移动模式：再点击任意空行即把本行搬到该位置
                        this.moveSource = index;
                        return true;
                    }
                    if (mouseX < checkMarkX(index, rowX) + 10
                            && mouseX >= checkMarkX(index, rowX) - 2) {
                        // 左侧勾选区：切换启用状态
                        ProgramIR.Line[] snapshot = this.snapshotLines(bus);
                        snapshot[index] = line.withEnabled(!line.enabled);
                        this.sendProgram(snapshot);
                        return true;
                    }
                    this.openLineEditor(index);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 打开浮点数值编辑框（自动保证整行在可视区域内）。 */
    private void beginValueEdit(int slot, int index, ConsoleBusBlockEntity bus) {
        // 翻到该行所在页，保证整行可见（变量表按整页显示）
        this.varPage = index / VAR_ROWS_PER_PAGE;
        this.clampScroll();
        int top = rowTop(index);
        this.editingSlot = slot;
        this.valueBox.setX(this.leftPos + LIST_X + 2 + ICON_COL_W);
        this.valueBox.setY(top + 11);
        this.valueBox.setWidth(LIST_W - 4 - ICON_COL_W);
        this.valueBox.setValue(formatFloat(floatValue(slot, bus)));
        this.valueBox.setVisible(true);
        this.valueBox.setFocused(true);
        this.setFocused(this.valueBox);
    }

    /** 提交数值编辑框内容到本地待保存值。 */
    private void commitValueEdit() {
        if (this.editingSlot < 0) {
            return;
        }
        try {
            float value = Float.parseFloat(this.valueBox.getValue().trim());
            if (Float.isFinite(value)) {
                if (this.editingSlot == ConsoleBusBlockEntity.VAR_SLOT_PERIOD) {
                    // 运行间隔：浮点自动取整、负值归零、超过上限按上限
                    this.pendingFloats.put(this.editingSlot, (float) sanitizePeriod(value));
                } else {
                    this.pendingFloats.put(this.editingSlot, value);
                }
            }
        } catch (NumberFormatException ignored) {
            // 非法输入直接丢弃，保留原值
        }
        this.editingSlot = -1;
        this.valueBox.setVisible(false);
        this.valueBox.setFocused(false);
        this.setFocused(null);
    }

    /** 运行间隔取值规则：非负、四舍五入取整、不超过上限。 */
    private static int sanitizePeriod(float value) {
        float safe = Float.isFinite(value) ? value : 0.0F;
        double rounded = Math.round(Math.max(0.0F, Math.min(
                ConsoleBusBlockEntity.MAX_PERIOD_TICKS, safe)));
        return (int) rounded;
    }

    private void cancelValueEdit() {
        this.editingSlot = -1;
        this.valueBox.setVisible(false);
        this.valueBox.setFocused(false);
    }

    private void deleteVar(int slot) {
        this.pendingFloats.remove(slot);
        this.pendingBools.remove(slot);
        if (this.editingSlot == slot) {
            this.cancelValueEdit();
        }
        ModNetwork.CHANNEL.sendToServer(ConsoleVarPacket.delete(this.menu.pos, slot));
    }

    /** 保存整张变量表：先提交正在编辑的值，再发送完整数组。 */
    private void saveValues() {
        this.commitValueEdit();
        ConsoleBusBlockEntity bus = bus();
        if (bus == null) {
            return;
        }
        if (this.locked(bus)) {
            this.localHint("message.cc_rc.controller.var_locked");
            return;
        }
        int slots = ConsoleBusBlockEntity.varSlots();
        float[] floats = new float[slots];
        boolean[] bools = new boolean[slots];
        for (int slot = 1; slot < slots; slot++) {
            VarDef def = bus.getVarDef(slot);
            if (def == null) {
                continue;
            }
            if (def.type == VarType.FLOAT) {
                floats[slot] = floatValue(slot, bus);
            } else {
                bools[slot] = boolValue(slot, bus);
            }
        }
        ModNetwork.CHANNEL.sendToServer(
                ConsoleVarPacket.saveValues(this.menu.pos, floats, bools));
        // 运行间隔单独提交：0 号槽位不在变量数组的保存范围内
        Float period = this.pendingFloats.get(ConsoleBusBlockEntity.VAR_SLOT_PERIOD);
        if (period != null) {
            ModNetwork.CHANNEL.sendToServer(
                    ConsoleVarPacket.savePeriod(this.menu.pos, period));
        }
        // 不清空待提交值：等服务端同步回总线后再由 containerTick 清理，避免界面闪烁
    }

    // ---------------------------------------------------------------- 新建变量

    private void openCreate(int slot) {
        ConsoleBusBlockEntity bus = bus();
        if (bus == null) {
            return;
        }
        this.cancelValueEdit();
        this.createSlot = slot;
        this.formError = "";
        this.createType = VarType.FLOAT;
        this.suggestOpen = true;
        this.suggestOffset = 0;
        this.nameBox.setValue("");
        this.nameBox.setVisible(true);
        this.setFormVisible(true);
        this.nameBox.setFocused(true);
        this.setFocused(this.nameBox);
        this.updateFormWidgets();
    }

    private void closeCreate() {
        this.createSlot = -1;
        this.formError = "";
        this.suggestOpen = false;
        this.suggestOffset = 0;
        this.setFormVisible(false);
        this.nameBox.setFocused(false);
        this.setFocused(null);
    }

    private void setFormVisible(boolean visible) {
        this.nameBox.setVisible(visible);
        this.typeButton.visible = visible;
        this.confirmButton.visible = visible;
        this.cancelButton.visible = visible;
        // 表单打开时隐藏底部按钮，避免误触与遮挡
        if (this.saveButton != null) {
            this.saveButton.visible = !visible;
        }
        if (this.discardButton != null) {
            this.discardButton.visible = !visible;
        }
        // 表单打开时同样隐藏变量翻页箭头，避免误触切换分页
        if (this.varPrevButton != null) {
            this.varPrevButton.visible = !visible;
        }
        if (this.varNextButton != null) {
            this.varNextButton.visible = !visible;
        }
    }

    /** 名称与某个设备名称完全一致时返回该候选（即「已绑定」）。 */
    private DeviceOption matchedDevice() {
        ConsoleBusBlockEntity bus = bus();
        String name = this.nameBox == null ? "" : this.nameBox.getValue().trim();
        if (bus == null || name.isEmpty()) {
            return null;
        }
        for (ConsoleBusBlockEntity.CompEntry comp : bus.getComps()) {
            if (comp.code.equalsIgnoreCase(name)) {
                VarType type = comp.type.variableType();
                return type == null ? null : new DeviceOption(comp, comp.code, type, false);
            }
            // 圆盘记录仪的第二名称 → 布尔推送触发变量
            if (comp.type.supportsHistory() && !comp.code2.isEmpty()
                    && comp.code2.equalsIgnoreCase(name)) {
                return new DeviceOption(comp, comp.code2, VarType.BOOL, true);
            }
            // 同步指示器：转速名 → 浮点变量，灯状态名 → 布尔变量
            if (comp.type.hasSpeedVariable() && !comp.code.isEmpty()
                    && comp.speedVarName().equalsIgnoreCase(name)) {
                return new DeviceOption(comp, comp.speedVarName(), VarType.FLOAT, false);
            }
            if (comp.type.hasLampVariable() && !comp.code.isEmpty()
                    && comp.lampVarName().equalsIgnoreCase(name)) {
                return new DeviceOption(comp, comp.lampVarName(), VarType.BOOL, false);
            }
            // 警报器：静音变量名 → 布尔变量
            if (comp.type.hasMuteVariable() && !comp.code.isEmpty()
                    && comp.muteVarName().equalsIgnoreCase(name)) {
                return new DeviceOption(comp, comp.muteVarName(), VarType.BOOL, false);
            }
            // RGB 颜色控制器的通道名 → 浮点变量
            if (comp.type == ConsoleCompType.RGB_COLOR) {
                for (String channel : new String[]{comp.rgbR, comp.rgbG, comp.rgbB}) {
                    if (!channel.isEmpty() && channel.equalsIgnoreCase(name)) {
                        return new DeviceOption(comp, channel, VarType.FLOAT, false);
                    }
                }
            }
        }
        return null;
    }

    /** 下拉左边界（与名称输入框对齐）。 */
    private int suggestX() {
        return this.leftPos + LIST_X + 8;
    }

    /** 下拉上边界（紧贴名称输入框下方）。 */
    private int suggestTop() {
        return this.topPos + FORM_NAME_Y + FORM_NAME_H + 1;
    }

    /** 下拉宽度（与名称输入框一致）。 */
    private int suggestW() {
        return LIST_W - 16;
    }

    /** 下拉当前显示的行数。 */
    private int suggestRowCount() {
        return Math.min(SUGGEST_ROWS, this.suggestions.size());
    }

    /** 补全下拉当前是否真的在绘制（展开且存在候选）。 */
    private boolean suggestVisible() {
        return this.createSlot >= 0 && this.suggestOpen && !this.suggestions.isEmpty();
    }

    /**
     * 同步类型按钮可见性：下拉绘制时会盖住该区域，而文字渲染批次晚于色块无法真正遮挡，
     * 因此下拉展开期间直接隐藏类型按钮（背景 + 文字）。
     */
    private void syncTypeButtonVisibility() {
        if (this.typeButton != null) {
            this.typeButton.visible = this.createSlot >= 0 && !this.suggestVisible();
        }
    }

    /** 下拉当前高度（含 1px 边框）。 */
    private int suggestH() {
        return this.suggestRowCount() * SUGGEST_ROW_H + 2;
    }

    /** 鼠标是否在下拉列表内。 */
    private boolean isInsideSuggest(double mouseX, double mouseY) {
        if (!this.suggestOpen || this.suggestions.isEmpty()) {
            return false;
        }
        int x = this.suggestX();
        int y = this.suggestTop();
        return mouseX >= x && mouseX < x + this.suggestW()
                && mouseY >= y && mouseY < y + this.suggestH();
    }

    /** 鼠标命中的候选下标（已含滚动偏移）；未命中返回 -1。 */
    private int suggestionAt(double mouseX, double mouseY) {
        if (!this.isInsideSuggest(mouseX, mouseY)) {
            return -1;
        }
        int rel = (int) (mouseY - this.suggestTop() - 1);
        if (rel < 0) {
            return -1;
        }
        int row = rel / SUGGEST_ROW_H + this.suggestOffset;
        return row >= 0 && row < this.suggestions.size() ? row : -1;
    }

    /**
     * 按当前输入刷新自动补全候选（只列出可绑定且尚未绑定的设备）。
     * 输入为空时列出全部可绑定设备（点击输入框即可看到所有设备）。
     */
    private void updateSuggestions() {
        this.suggestions.clear();
        String typed = this.nameBox == null ? ""
                : this.nameBox.getValue().trim().toLowerCase(Locale.ROOT);
        for (DeviceOption option : devices()) {
            if (!typed.isEmpty()) {
                // 完全一致的名称已是绑定状态，不再作为候选
                if (option.name().equalsIgnoreCase(typed)) {
                    continue;
                }
                if (!option.name().toLowerCase(Locale.ROOT).startsWith(typed)) {
                    continue;
                }
            }
            this.suggestions.add(option);
        }
        // 按名称排序，保证滚动 / 过滤后顺序稳定
        this.suggestions.sort(Comparator.comparing(o -> o.name().toLowerCase(Locale.ROOT)));
        int max = Math.max(0, this.suggestions.size() - SUGGEST_ROWS);
        this.suggestOffset = Math.max(0, Math.min(max, this.suggestOffset));
        this.syncTypeButtonVisibility();
    }

    private void updateFormWidgets() {
        if (this.typeButton == null) {
            return;
        }
        DeviceOption matched = this.matchedDevice();
        VarType boundType = matched == null ? null : matched.type();
        if (boundType != null) {
            // 名称对上设备：自动绑定，类型由设备决定且不可改
            this.createType = boundType;
            this.typeButton.active = false;
        } else {
            this.typeButton.active = true;
        }
        this.typeButton.setMessage(Component.translatable("gui.cc_rc.program.type",
                Component.translatable(this.createType == VarType.BOOL
                        ? "gui.cc_rc.program.bool" : "gui.cc_rc.program.float")));
        this.updateSuggestions();
    }

    private void confirmCreate() {
        ConsoleBusBlockEntity bus = bus();
        if (bus == null || this.createSlot < 0 || this.locked(bus)) {
            this.closeCreate();
            return;
        }
        String name = this.nameBox.getValue().trim();
        // 提交时收起补全下拉，让名称框下方的报错提示可见
        this.suggestOpen = false;
        this.syncTypeButtonVisibility();
        // 程序行里操作数输入框用 true / false / 数字 表示常量，
        // 因此变量名不能数字开头、也不能与这两个常量重名，否则无法区分
        if (!VAR_NAME_PATTERN.matcher(name).matches()
                || name.equalsIgnoreCase("true") || name.equalsIgnoreCase("false")) {
            this.formError = Component.translatable("gui.cc_rc.program.name_rule").getString();
            return;
        }
        if (bus.findVarSlot(name) >= 0) {
            this.formError = Component.translatable(
                    "message.cc_rc.controller.var_dup_name").getString();
            return;
        }
        DeviceOption matched = this.matchedDevice();
        int compId = 0;
        VarType type = this.createType;
        if (matched != null) {
            // 名称与设备名称一致即绑定该设备，服务端会再次按名称校验
            compId = matched.comp().id;
            type = matched.type();
        } else {
            // 名称属于不提供变量的设备（发信终端 / 圆盘记录仪主代号）时禁止占用，避免歧义
            for (ConsoleBusBlockEntity.CompEntry comp : bus.getComps()) {
                if (comp.code.equalsIgnoreCase(name) && comp.type.variableType() == null) {
                    this.formError = Component.translatable(
                            "message.cc_rc.controller.var_name_used").getString();
                    return;
                }
            }
        }
        ModNetwork.CHANNEL.sendToServer(ConsoleVarPacket.create(
                this.menu.pos, this.createSlot, name, type.ordinal(), compId));
        this.closeCreate();
    }

    // ---------------------------------------------------------------- 每 tick 刷新

    @Override
    protected void containerTick() {
        super.containerTick();
        ConsoleBusBlockEntity bus = bus();
        if (bus == null) {
            return;
        }
        if (this.saveButton != null) {
            this.saveButton.active = !this.locked(bus);
        }
        // 右侧翻页与保存按钮状态
        int pages = ConsoleBusBlockEntity.MAX_PROGRAM_LINES / ROWS_PER_PAGE;
        if (this.prevPageButton != null) {
            this.prevPageButton.active = this.page > 0;
        }
        if (this.nextPageButton != null) {
            this.nextPageButton.active = this.page < pages - 1;
        }
        if (this.progSaveButton != null) {
            this.progSaveButton.active = !this.locked(bus);
        }
        int slots = ConsoleBusBlockEntity.varSlots();
        // 变量表翻页箭头：首页禁用向前、末页禁用向后
        int varPages = Math.max(1, (slots + VAR_ROWS_PER_PAGE - 1) / VAR_ROWS_PER_PAGE);
        if (this.varPrevButton != null) {
            this.varPrevButton.active = this.varPage > 0;
        }
        if (this.varNextButton != null) {
            this.varNextButton.active = this.varPage < varPages - 1;
        }
        // 总线值已经等于本地待提交值时（保存成功同步回来）丢弃本地缓存
        this.pendingFloats.entrySet().removeIf(entry -> {
            int slot = entry.getKey();
            if (slot == ConsoleBusBlockEntity.VAR_SLOT_PERIOD) {
                return Math.abs(bus.getVarFloat(slot) - entry.getValue()) < 1.0E-6F;
            }
            VarDef def = slot > 0 && slot < slots ? bus.getVarDef(slot) : null;
            return def == null || def.type != VarType.FLOAT
                    || Math.abs(bus.getVarFloat(slot) - entry.getValue()) < 1.0E-6F;
        });
        this.pendingBools.entrySet().removeIf(entry -> {
            int slot = entry.getKey();
            VarDef def = slot > 0 && slot < slots ? bus.getVarDef(slot) : null;
            return def == null || def.type != VarType.BOOL
                    || bus.getVarBool(slot) == entry.getValue();
        });
        this.clampScroll();
    }

    // ---------------------------------------------------------------- 渲染

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // 静态底板全部烘焙进贴图（416×260，与界面 1:1）：灰色底面、窗口左上高光 / 右下阴影、
        // 中间装饰分隔线（暗线 + 亮线）。这里只绘制会随状态变化的区域。
        graphics.blit(BG, this.leftPos, this.topPos, GUI_W, GUI_H,
                0.0F, 0.0F, GUI_W, GUI_H, GUI_W, GUI_H);
        int px = this.leftPos;
        int py = this.topPos;

        // 变量列表：原版凹陷深色槽；新建变量时改为浅色凸起子面板
        int listX = px + LIST_X;
        int listY = py + LIST_Y;
        if (this.createSlot >= 0) {
            drawRaised(graphics, listX - 2, listY - 2, LIST_W + 4, LIST_H + 4, 0xFFC6C6C6);
        } else {
            drawSunken(graphics, listX - 2, listY - 2, LIST_W + 4, LIST_H + 4, COLOR_LIST_BG);
        }

        // 程序列表：原版凹陷深色槽；行编辑时改为浅色凸起子面板
        int progX = px + RIGHT_X;
        int progY = py + PROG_LIST_Y;
        if (this.editingLine >= 0) {
            drawRaised(graphics, progX - 2, progY - 2, RIGHT_W + 4, PROG_LIST_H + 4, 0xFFC6C6C6);
        } else {
            drawSunken(graphics, progX - 2, progY - 2, RIGHT_W + 4, PROG_LIST_H + 4, COLOR_LIST_BG);
        }
    }

    /** 原版凸起斜面：外圈黑边，左上白色高光、右下深灰阴影（面板 / 按钮）。 */
    private static void drawRaised(GuiGraphics graphics, int x, int y, int w, int h, int face) {
        graphics.fill(x, y, x + w, y + h, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, face);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 2, y + h - 1, 0xFFFFFFFF);
        graphics.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xFF555555);
        graphics.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, 0xFF555555);
    }

    /** 原版凹陷槽：外圈黑边，左上深灰、右下亮色（列表 / 输入框底）。 */
    private static void drawSunken(GuiGraphics graphics, int x, int y, int w, int h, int face) {
        graphics.fill(x, y, x + w, y + h, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, face);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 2, y + h - 1, 0xFF373737);
        graphics.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, COLOR_LIST_HIGHLIGHT);
        graphics.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, COLOR_LIST_HIGHLIGHT);
    }

    /** 输入框底板：未聚焦时也画深色凹槽，保证亮色文字在原版浅灰面板上可读。 */
    private static void drawFieldBg(GuiGraphics graphics, EditBox box) {
        drawSunken(graphics, box.getX(), box.getY(),
                box.getWidth(), box.getHeight(), 0xFF1A1A1A);
    }

    /** 原版风格按钮工厂：统一浅灰斜面样式。 */
    private static Button vanillaButton(int x, int y, int w, int h,
                                       Component text, Button.OnPress onPress) {
        return new VanillaButton(x, y, w, h, text, onPress);
    }

    /**
     * 原版风格按钮（参考素材中的配方书按钮）：
     * 浅灰凸起斜面，文字为深色，悬停时内侧描红框，禁用时整体压暗。
     */
    private static final class VanillaButton extends Button {
        private VanillaButton(int x, int y, int w, int h,
                              Component message, OnPress onPress) {
            super(x, y, w, h, message, onPress, Button.DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            int w = this.getWidth();
            int h = this.getHeight();
            boolean hover = this.active && mouseX >= x && mouseX < x + w
                    && mouseY >= y && mouseY < y + h;
            // 禁用时压暗按钮面
            drawRaised(graphics, x, y, w, h, this.active ? 0xFFC6C6C6 : 0xFF9E9E9E);
            // 悬停时内侧一圈深红描边（原版选中态）
            if (hover) {
                graphics.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFFB00000);
                graphics.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xFFB00000);
                graphics.fill(x + 1, y + 1, x + 2, y + h - 1, 0xFFB00000);
                graphics.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, 0xFFB00000);
            }
            // 按钮文字统一使用深色，悬停时加深，禁用时中灰
            int color = !this.active ? 0xFF707070 : (hover ? 0xFF000000 : 0xFF303030);
            // 手动画无阴影文字（drawCenteredString 默认带阴影，深色文字会形成重影）
            Font font = Minecraft.getInstance().font;
            Component message = this.getMessage();
            int tx = x + (w - font.width(message)) / 2;
            int ty = y + (h - 8) / 2;
            graphics.drawString(font, message, tx, ty, color, false);
        }
    }

    /** 翻页箭头按钮工厂：直接裁剪 page_arrows.png 的对应象限绘制。 */
    private static Button arrowButton(int x, int y, int w, int h,
                                      boolean left, Button.OnPress onPress) {
        return new ArrowButton(x, y, w, h, left, onPress);
    }

    /**
     * 翻页箭头：使用贴图 page_arrows.png 的四象限。
     * 左上 = 向前翻页常态、左下 = 向前翻页悬停、右上 = 向后翻页常态、右下 = 向后翻页悬停；
     * 禁用时整体压暗（绘制一层半透明黑），不额外使用其它象限。
     */
    private static final class ArrowButton extends Button {
        private final boolean left;

        private ArrowButton(int x, int y, int w, int h, boolean left, OnPress onPress) {
            super(x, y, w, h, Component.empty(), onPress, Button.DEFAULT_NARRATION);
            this.left = left;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            int w = this.getWidth();
            int h = this.getHeight();
            boolean hover = this.active && mouseX >= x && mouseX < x + w
                    && mouseY >= y && mouseY < y + h;
            // 按象限取 UV：u = 左/右（0 / 32），v = 常态/悬停（0 / 32），
            // 再按象限内箭头包围盒裁剪，1:1 绘制保持像素清晰
            int srcX = (this.left ? 0 : ARROW_TEX)
                    + (this.left ? ARROW_LEFT_INSET : ARROW_RIGHT_INSET);
            int srcY = (hover ? ARROW_TEX : 0) + ARROW_TOP;
            int dx = x + (w - ARROW_W) / 2;
            int dy = y + (h - ARROW_H) / 2;
            // 禁用时用压暗的颜色绘制箭头：只影响不透明像素，不会把三角以外的空白染成方块
            if (!this.active) {
                graphics.setColor(0.42F, 0.42F, 0.42F, 1.0F);
            }
            graphics.blit(ARROWS, dx, dy, ARROW_W, ARROW_H,
                    (float) srcX, (float) srcY, ARROW_W, ARROW_H, ARROW_TEX_SIZE, ARROW_TEX_SIZE);
            if (!this.active) {
                graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font,
                Component.translatable("gui.cc_rc.program.vars"), LIST_X, 6, COLOR_TEXT, false);
        if (this.editingLine >= 0) {
            // 行编辑时标题显示当前行号
            graphics.drawString(this.font,
                    Component.translatable("gui.cc_rc.program.line_title",
                            pad(this.editingLine)),
                    RIGHT_X, 6, COLOR_TEXT, false);
        } else {
            graphics.drawString(this.font,
                    Component.translatable("gui.cc_rc.program.code"), RIGHT_X, 6,
                    COLOR_TEXT, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        // 先同步按钮显隐：被补全下拉遮挡的按钮不绘制，避免文字穿透底板
        this.syncEditorButtons();
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        if (this.createSlot < 0) {
            this.renderVarRows(graphics, mouseX, mouseY);
        } else {
            this.renderCreateForm(graphics, mouseX, mouseY);
        }
        // 右侧程序区：行列表或行编辑器
        if (this.editingLine < 0) {
            this.renderProgramRows(graphics, mouseX, mouseY);
            this.renderRunWarnings(graphics, mouseX, mouseY);
        } else {
            this.renderLineEditor(graphics);
            // 行编辑器输入框按需绘制（条件框在「true」模式下隐藏；被下拉遮挡的不绘制）
            for (EditBox box : this.lineEditorBoxes) {
                if (box.visible && !this.coveredBySuggest(box)) {
                    // 先画原版凹陷输入框底，保证亮色文字在浅灰面板上可读
                    drawFieldBg(graphics, box);
                    // 再给输入框自身的文字上色，最后覆盖绘制更精细的分段颜色
                    this.syncBoxTextColor(box);
                    box.render(graphics, mouseX, mouseY, partialTick);
                    this.renderBoxTokenColors(graphics, box);
                }
            }
            // 自动补全下拉最后绘制，盖住输入框与按钮
            this.renderFieldSuggest(graphics, mouseX, mouseY);
            // 表达式框补全下拉同样最后绘制
            this.exprBox.renderSuggestions(graphics, mouseX, mouseY);
            // 条件框的补全下拉
            this.condBox.renderSuggestions(graphics, mouseX, mouseY);
        }
        // 数值输入框跟随所在行（滚动后重新定位）
        if (this.valueBox.isVisible() && this.editingSlot >= 0) {
            this.valueBox.setY(rowTop(this.editingSlot) + 11);
            this.valueBox.setX(this.leftPos + LIST_X + 2 + ICON_COL_W);
            drawFieldBg(graphics, this.valueBox);
            this.valueBox.render(graphics, mouseX, mouseY, partialTick);
        }
        if (this.createSlot >= 0) {
            drawFieldBg(graphics, this.nameBox);
            this.nameBox.render(graphics, mouseX, mouseY, partialTick);
            // 补全下拉最后绘制：盖住类型按钮及其文字、提示区等所有表单元素
            this.renderSuggestions(graphics, mouseX, mouseY);
        }
    }

    /** 绘制变量列表（只画可视范围内的行；行下标即槽位号）。 */
    private void renderVarRows(GuiGraphics graphics, int mouseX, int mouseY) {
        ConsoleBusBlockEntity bus = bus();
        int listX = this.leftPos + LIST_X;
        int listTop = this.topPos + LIST_Y;
        int listBottom = listTop + LIST_H;
        int count = ConsoleBusBlockEntity.varSlots();
        // 悬停在变量名上时显示的完整名称 / 绑定设备（在关闭裁剪后统一绘制）
        String tipName = null;
        String tipBind = null;
        int tipColor = COLOR_EDIT_TEXT;

        graphics.enableScissor(listX, listTop, listX + LIST_W, listBottom);
        for (int slot = 0; slot < count; slot++) {
            int top = rowTop(slot);
            if (top + ROW_H <= listTop || top >= listBottom) {
                continue;
            }
            boolean hover = mouseX >= listX && mouseX < listX + LIST_W
                    && mouseY >= top && mouseY < top + ROW_H;
            // 0 号槽位：运行间隔，固定存在，只能改值
            if (slot == ConsoleBusBlockEntity.VAR_SLOT_PERIOD) {
                graphics.fill(listX, top, listX + LIST_W, top + ROW_H - 1,
                        hover ? COLOR_ROW_HOVER : COLOR_ROW);
                int rowX = listX + 3 + ICON_COL_W;
                String prefix = "#" + pad(slot);
                graphics.drawString(this.font, prefix, rowX, top + 3, COLOR_DIM_LIGHT, false);
                int nameX = rowX + this.font.width(prefix) + 3;
                graphics.drawString(this.font,
                        Component.translatable("gui.cc_rc.program.period"),
                        nameX, top + 3, COLOR_TEXT_LIGHT, false);
                String unit = Component.translatable("gui.cc_rc.program.tick").getString();
                graphics.drawString(this.font, unit, rowX, top + 12, COLOR_DIM_LIGHT, false);
                graphics.drawString(this.font, "= " + formatFloat(floatValue(slot, bus)),
                        rowX + this.font.width(unit) + 4, top + 12, COLOR_TEXT_LIGHT, false);
                continue;
            }
            VarDef def = bus == null ? null : bus.getVarDef(slot);
            if (def == null) {
                graphics.fill(listX, top, listX + LIST_W, top + ROW_H - 1,
                        hover ? COLOR_ROW_EMPTY_HOVER : COLOR_ROW_EMPTY);
                int rowX = listX + 3 + ICON_COL_W;
                graphics.drawString(this.font, "#" + pad(slot),
                        rowX, top + 3, COLOR_DIM_LIGHT, false);
                graphics.drawString(this.font,
                        Component.translatable("gui.cc_rc.program.empty"),
                        rowX, top + 12, COLOR_DIM_LIGHT, false);
            } else {
                graphics.fill(listX, top, listX + LIST_W, top + ROW_H - 1,
                        hover ? COLOR_ROW_HOVER : COLOR_ROW);
                int rowX = listX + 3 + ICON_COL_W;
                graphics.drawString(this.font, "#" + pad(slot),
                        rowX, top + 3, COLOR_DIM_LIGHT, false);
                String prefix = "#" + pad(slot);
                int nameX = rowX + this.font.width(prefix) + 3;
                // 绑定设备时在行左侧渲染设备物品图标（只画贴图，不画物品栏底板）
                ConsoleBusBlockEntity.CompEntry boundComp = bus.findComp(def.compId);
                if (boundComp != null) {
                    graphics.renderItem(new ItemStack(boundComp.type.item()),
                            listX + 3, top + 4);
                }
                // 名称按可用宽度截断显示；鼠标悬停名称区域时用 tooltip 显示全名与绑定设备
                String shownName = this.font.plainSubstrByWidth(def.name,
                        listX + LIST_W - DELETE_W - nameX);
                // 变量名按类型着色：布尔蓝、浮点绿，绑定设备的更深一档
                graphics.drawString(this.font, shownName, nameX, top + 3,
                        this.varColor(def, false), false);
                if (hover && mouseX >= nameX && mouseX < nameX + this.font.width(shownName)
                        && mouseY >= top + 3 && mouseY < top + 12) {
                    tipName = def.name;
                    tipColor = this.varColor(def, true);
                    if (boundComp != null) {
                        // 第二行只显示「设备中文名（设备注册名）」，不再重复变量名
                        tipBind = Component.translatable(boundComp.type.translationKey()).getString()
                                + "（" + boundComp.type.registryName() + "）";
                    }
                }
                String type = Component.translatable(def.type == VarType.BOOL
                        ? "gui.cc_rc.program.bool" : "gui.cc_rc.program.float").getString();
                graphics.drawString(this.font, type, rowX, top + 12, COLOR_DIM_LIGHT, false);
                String value = def.type == VarType.BOOL
                        ? Component.translatable(boolValue(slot, bus)
                                ? "gui.cc_rc.program.on" : "gui.cc_rc.program.off").getString()
                        : formatFloat(floatValue(slot, bus));
                graphics.drawString(this.font, "= " + value,
                        rowX + this.font.width(type) + 4, top + 12, COLOR_TEXT_LIGHT, false);
                // 删除按钮
                int dx = listX + LIST_W - DELETE_W + 2;
                boolean hoverDelete = hover && mouseX >= listX + LIST_W - DELETE_W
                        && mouseY < top + 12;
                graphics.drawString(this.font, "×", dx, top + 2,
                        hoverDelete ? COLOR_ERROR_LIGHT : COLOR_DIM_LIGHT, false);
            }
        }
        graphics.disableScissor();
        // 名称 tooltip：第一行完整变量名，第二行「设备中文名（设备注册名）」或未绑定
        if (tipName != null) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(tipName).withStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb(tipColor & 0xFFFFFF))));
            tooltip.add(tipBind != null
                    ? Component.literal(tipBind)
                    : Component.translatable("gui.cc_rc.program.unbound"));
            graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    /** 新建变量表单的文字部分（按钮 / 输入框由控件自行绘制）。 */
    private void renderCreateForm(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.leftPos + LIST_X + 8;
        int y = this.topPos;
        // 下拉展开时盖住类型按钮所在区域：文字渲染批次晚于色块，无法靠绘制顺序遮挡，
        // 因此直接把类型按钮（背景 + 文字）隐藏，等下拉收起后再显示
        boolean covered = this.suggestVisible();
        this.typeButton.visible = !covered;
        graphics.drawString(this.font,
                Component.translatable("gui.cc_rc.program.new_var", pad(this.createSlot)),
                x, y + FORM_TITLE_Y, COLOR_TEXT, false);
        graphics.drawString(this.font, Component.translatable("gui.cc_rc.program.name"),
                x, y + FORM_LABEL_Y, COLOR_DIM, false);
        // 提示区（名称框下方 / 类型按钮上方）：平常留空，只在报错或已绑定设备时显示，自动换行
        String message = this.formError;
        int color = COLOR_ERROR;
        if (message.isEmpty()) {
            DeviceOption matched = this.matchedDevice();
            if (matched != null) {
                message = Component.translatable("gui.cc_rc.program.bound",
                        matched.name()).getString();
                color = COLOR_TEXT;
            }
        }
        // 提示区同样位于下拉覆盖范围内，展开下拉时不绘制，避免文字透出
        if (!message.isEmpty() && !covered) {
            this.drawWrapped(graphics, message, x, y + FORM_MSG_Y,
                    LIST_W - 16, FORM_MSG_LINES, color);
        }
    }

    /** 按宽度自动换行绘制文本（最多 maxLines 行，超出部分截断）。 */
    private void drawWrapped(GuiGraphics graphics, String text, int x, int y,
                             int width, int maxLines, int color) {
        java.util.List<net.minecraft.util.FormattedCharSequence> lines =
                this.font.split(Component.literal(text), width);
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            graphics.drawString(this.font, lines.get(i), x, y + i * 10, color, false);
        }
    }

    /**
     * 名称输入框下方的自动补全下拉（仿原版指令补全）。
     * 深色底板 + 黄色高亮悬停行，支持滚轮滚动，悬停时显示设备类型的中文名。
     */
    private void renderSuggestions(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!this.suggestOpen || this.suggestions.isEmpty()) {
            return;
        }
        int x = this.suggestX();
        int top = this.suggestTop();
        int w = this.suggestW();
        int rows = this.suggestRowCount();
        int h = rows * SUGGEST_ROW_H + 2;
        // 1px 黑边 + 深色底板
        graphics.fill(x - 1, top - 1, x + w + 1, top + h + 1, COLOR_SUGGEST_BORDER);
        graphics.fill(x, top, x + w, top + h, COLOR_SUGGEST_BG);

        int hovered = -1;
        for (int row = 0; row < rows; row++) {
            int index = row + this.suggestOffset;
            if (index >= this.suggestions.size()) {
                break;
            }
            int rowY = top + 1 + row * SUGGEST_ROW_H;
            boolean hover = mouseX >= x && mouseX < x + w
                    && mouseY >= rowY && mouseY < rowY + SUGGEST_ROW_H;
            if (hover) {
                hovered = index;
                graphics.fill(x, rowY, x + w, rowY + SUGGEST_ROW_H, COLOR_SUGGEST_HOVER_BG);
            }
            graphics.drawString(this.font,
                    this.font.plainSubstrByWidth(this.suggestions.get(index).name(), w - 8),
                    x + 3, rowY + 2,
                    hover ? COLOR_SUGGEST_HOVER : COLOR_SUGGEST_TEXT, false);
        }
        // 内容超出可视行数时右侧画滚动条
        if (this.suggestions.size() > SUGGEST_ROWS) {
            int barX = x + w - 2;
            int trackH = rows * SUGGEST_ROW_H;
            graphics.fill(barX, top + 1, barX + 2, top + 1 + trackH, 0x30FFFFFF);
            int thumbH = Math.max(6, trackH * rows / this.suggestions.size());
            int maxOffset = this.suggestions.size() - rows;
            int travel = trackH - thumbH;
            int thumbY = top + 1 + travel * this.suggestOffset / Math.max(1, maxOffset);
            graphics.fill(barX, thumbY, barX + 2, thumbY + thumbH, 0xFFA0A0A0);
        }
        // 悬停显示「这是什么设备」（中文类型名；触发名称额外标注）
        if (hovered >= 0) {
            DeviceOption option = this.suggestions.get(hovered);
            Component tip = Component.translatable(option.comp().type.translationKey());
            if (option.trigger()) {
                tip = tip.copy().append(Component.literal(" · 推送触发"));
            }
            graphics.renderTooltip(this.font, tip, mouseX, mouseY);
        }
    }

    // ---------------------------------------------------------------- 右侧程序区

    /** 鼠标命中的程序行号（已含页码）；不在行列表内返回 -1。 */
    private int programLineAt(double mouseX, double mouseY) {
        int x = this.leftPos + RIGHT_X;
        int top = this.topPos + PROG_LIST_Y;
        if (mouseX < x || mouseX >= x + RIGHT_W
                || mouseY < top || mouseY >= top + PROG_LIST_H) {
            return -1;
        }
        int row = (int) ((mouseY - top) / LINE_H);
        int index = this.page * ROWS_PER_PAGE + row;
        return index >= 0 && index < ConsoleBusBlockEntity.MAX_PROGRAM_LINES ? index : -1;
    }

    /** 程序行左侧勾选区文字的起始 X（绘制与点击命中共用，保证位置一致）。 */
    private int checkMarkX(int index, int rowX) {
        return rowX + 3 + this.font.width(pad(index)) + 4;
    }

    /** 翻页（超出范围自动夹紧）。 */
    private void changePage(int delta) {
        int pages = ConsoleBusBlockEntity.MAX_PROGRAM_LINES / ROWS_PER_PAGE;
        this.page = Math.max(0, Math.min(pages - 1, this.page + delta));
    }

    /** 取整张程序表的快照（下标即行号）。 */
    private ProgramIR.Line[] snapshotLines(ConsoleBusBlockEntity bus) {
        ProgramIR.Line[] lines =
                new ProgramIR.Line[ConsoleBusBlockEntity.MAX_PROGRAM_LINES];
        for (int i = 0; i < lines.length; i++) {
            lines[i] = bus.getProgramLine(i);
        }
        return lines;
    }

    /** 提交整张程序表；服务端做全量校验后同步回来刷新界面。 */
    private void sendProgram(ProgramIR.Line[] lines) {
        ModNetwork.CHANNEL.sendToServer(
                ConsoleProgramPacket.saveLines(this.menu.pos, lines));
    }

    /**
     * 把源行的程序整行搬到目标空行，源行随即清空。
     *
     * 目标必须是空行（由点击处保证）；搬动后立即提交整张程序表，
     * 程序行只按变量槽位引用数据，因此行号变化不影响运行语义。
     */
    private void moveLine(int from, int to) {
        ConsoleBusBlockEntity bus = bus();
        if (bus == null || from == to) {
            this.moveSource = -1;
            return;
        }
        ProgramIR.Line line = bus.getProgramLine(from);
        if (line == null || bus.getProgramLine(to) != null) {
            this.moveSource = -1;
            return;
        }
        ProgramIR.Line[] snapshot = this.snapshotLines(bus);
        snapshot[to] = line;
        snapshot[from] = null;
        this.sendProgram(snapshot);
        this.moveSource = -1;
        // 跳到目标行所在页，让搬动的结果立即可见
        this.page = to / ROWS_PER_PAGE;
        this.localHint("message.cc_rc.controller.prog_moved", pad(to));
    }

    /**
     * 复制当前编辑行：把整行收进待放置状态并退出编辑器，
     * 随后在程序列表里点任意空行即可放置副本（与「移动」的操作方式一致）。
     */
    private void copyLineForward() {
        ConsoleBusBlockEntity bus = bus();
        if (bus == null || this.editingLine < 0) {
            return;
        }
        if (this.locked(bus)) {
            this.lineError = text("message.cc_rc.controller.prog_locked");
            return;
        }
        ProgramIR.Line current = bus.getProgramLine(this.editingLine);
        if (current == null) {
            // 空行没有可复制的内容
            this.lineError = text("gui.cc_rc.program.copy_empty");
            return;
        }
        int from = this.editingLine;
        // 整行原样复制（条件 / 目标 / 算式 / 注释 / 启用状态）
        this.pendingCopy = current;
        this.moveSource = -1;
        this.closeLineEditor();
        this.localHint("message.cc_rc.controller.prog_copy_ready", pad(from));
    }

    /**
     * 把待放置的副本写入目标空行，并跳到目标行所在页。
     * 目标必须是空行（由点击处保证）。
     */
    private void pasteLine(int to) {
        ConsoleBusBlockEntity bus = bus();
        ProgramIR.Line line = this.pendingCopy;
        this.pendingCopy = null;
        if (bus == null || line == null || bus.getProgramLine(to) != null) {
            return;
        }
        ProgramIR.Line[] snapshot = this.snapshotLines(bus);
        snapshot[to] = line;
        this.sendProgram(snapshot);
        // 跳到目标行所在页，让放置结果立即可见
        this.page = to / ROWS_PER_PAGE;
        this.localHint("message.cc_rc.controller.prog_pasted", pad(to));
    }

    /** 删除当前编辑的行（提交空行）并关闭编辑器。 */
    private void deleteEditingLine() {
        ConsoleBusBlockEntity bus = bus();
        if (bus == null || this.editingLine < 0) {
            return;
        }
        if (this.locked(bus)) {
            this.lineError = text("message.cc_rc.controller.prog_locked");
            return;
        }
        ProgramIR.Line[] snapshot = this.snapshotLines(bus);
        snapshot[this.editingLine] = null;
        this.sendProgram(snapshot);
        this.closeLineEditor();
    }

    /** 底部「保存」：重新提交当前快照（每行编辑其实已即时保存）；成功返回 true。 */
    private boolean saveProgram() {
        ConsoleBusBlockEntity bus = bus();
        if (bus == null) {
            return false;
        }
        if (this.locked(bus)) {
            this.localHint("message.cc_rc.controller.prog_locked");
            return false;
        }
        this.sendProgram(this.snapshotLines(bus));
        return true;
    }

    /** 打开指定行的编辑器；空行则按默认值新建。 */
    private void openLineEditor(int index) {
        ConsoleBusBlockEntity bus = bus();
        if (bus == null) {
            return;
        }
        this.cancelValueEdit();
        this.moveSource = -1;
        this.pendingCopy = null;
        this.editingLine = index;
        this.lineError = "";
        this.closeFieldSuggest();
        // 载入期间抑制输入框的即时校验，避免逐项赋值时闪出错误
        this.loadingLine = true;
        ProgramIR.Scope scope = bus.getProgramScope();
        ProgramIR.Line line = bus.getProgramLine(index);
        if (line == null) {
            List<Integer> slots = this.definedSlots(bus);
            this.editEnabled = true;
            this.condBox.setValue("");
            // 目标变量默认留空，由玩家自行填写，避免误把第一个变量当作默认目标
            this.targetBox.setValue("");
            this.commentBox.setValue("");
            this.exprBox.setValue("0");
            if (slots.isEmpty()) {
                this.lineError = text("gui.cc_rc.program.pick_var");
            }
        } else {
            this.editEnabled = line.enabled;
            // 条件：优先原文；否则用条件树生成；再否则回落到旧三态条件
            String condText;
            if (!line.condSource.isEmpty()) {
                condText = line.condSource;
            } else if (line.condExpr != null) {
                condText = line.condExpr.toText(scope);
            } else if (line.cond == ProgramIR.Cond.ALWAYS) {
                condText = "";
            } else {
                condText = (line.cond == ProgramIR.Cond.FALSE_VAR ? "!" : "")
                        + scope.nameOf(line.condSlot);
            }
            this.condBox.setValue(condText);
            this.targetBox.setValue(scope.nameOf(line.targetSlot));
            this.commentBox.setValue(line.comment);
            // 优先回显玩家原文（保留换行）；旧数据没有原文时用树生成等价的单行文本
            this.exprBox.setValue(line.exprSource.isEmpty()
                    ? (line.expr == null ? "" : line.expr.toText(scope))
                    : line.exprSource);
        }
        this.loadingLine = false;
        this.setLineEditorVisible(true);
        this.refreshLineError();
        // 焦点交给目标输入框：变量表单同时打开时，Enter / Esc 仍按行编辑器处理
        this.targetBox.setFocused(true);
        this.setFocused(this.targetBox);
    }

    /** 关闭行编辑器（不保存，行编辑是即时提交的）。 */
    private void closeLineEditor() {
        this.editingLine = -1;
        this.lineError = "";
        this.setLineEditorVisible(false);
    }

    /**
     * 解析表达式原文：变量名按总线变量表解析，目标类型用于 0 / 1 的布尔判定与赋值类型校验。
     * 结果同时带上 token 区间，供输入框着色与错误标红。
     */
    private ProgramExprParser.Result analyzeExpr(ConsoleBusBlockEntity bus) {
        return ProgramExprParser.parse(this.exprBox.getValue(), this.exprVars(bus),
                this.targetType(bus));
    }

    /** 解析条件原文：期望类型固定为布尔；空串由调用方视为「总是执行」。 */
    private ProgramExprParser.Result analyzeCond(ConsoleBusBlockEntity bus) {
        return ProgramExprParser.parse(this.condBox.getValue(), this.exprVars(bus), VarType.BOOL);
    }

    /** 表达式解析用的变量表：名称 → 槽位（忽略大小写），并兼作类型作用域。 */
    private ProgramExprParser.Vars exprVars(ConsoleBusBlockEntity bus) {
        return new ProgramExprParser.Vars() {
            @Override
            public int findSlot(String name) {
                // 解析器约定未定义返回 -1（总线同样使用 -1）
                return bus == null ? -1 : bus.findVarSlot(name);
            }

            @Override
            public VarType typeOf(int slot) {
                VarDef def = bus == null ? null : bus.getVarDef(slot);
                return def == null ? null : def.type;
            }

            @Override
            public String nameOf(int slot) {
                VarDef def = bus == null ? null : bus.getVarDef(slot);
                return def == null ? "#" + slot : def.name;
            }
        };
    }

    /** 赋值目标类型；未填写或不可写入时返回 null，表示不做赋值类型校验。 */
    private VarType targetType(ConsoleBusBlockEntity bus) {
        ConsoleBusBlockEntity.VarDef def = resolveWritable(bus, this.targetBox.getValue().trim());
        return def == null ? null : def.type;
    }

    /** 表达式变量名着色：按变量类型取亮色，未定义用普通文字色。 */
    private int exprNameColor(String name) {
        ConsoleBusBlockEntity bus = bus();
        int slot = bus == null ? 0 : bus.findVarSlot(name);
        return slot > 0 ? this.varColor(bus, slot, true) : COLOR_EDIT_TEXT;
    }

    /** 表达式补全候选：布尔常量 + 全部变量名（符号由输入框自行补全）。 */
    private List<String> exprCandidates() {
        List<String> names = new ArrayList<>();
        names.add("true");
        names.add("false");
        names.addAll(this.variableNames(false, false));
        return names;
    }

    /** 解析错误 → 界面提示文案（具体字符已由输入框标红）。 */
    private static String exprErrorText(ProgramExprParser.Error error) {
        if (error == null) {
            return text("gui.cc_rc.program.err_expr");
        }
        if (ProgramExprParser.ERR_NAME.equals(error.code)) {
            return text("gui.cc_rc.program.err_name");
        }
        if (ProgramExprParser.ERR_TYPE.equals(error.code)) {
            return text("gui.cc_rc.program.err_type");
        }
        if (ProgramExprParser.ERR_DEPTH.equals(error.code)) {
            return text("gui.cc_rc.program.err_deep");
        }
        return text("gui.cc_rc.program.err_expr");
    }

    /** 条件解析错误 → 界面提示文案（具体字符已由条件框标红）。 */
    private static String condErrorText(ProgramExprParser.Error error) {
        if (error == null) {
            return text("gui.cc_rc.program.err_cond");
        }
        if (ProgramExprParser.ERR_NAME.equals(error.code)) {
            return text("gui.cc_rc.program.err_name");
        }
        if (ProgramExprParser.ERR_DEPTH.equals(error.code)) {
            return text("gui.cc_rc.program.err_deep");
        }
        return text("gui.cc_rc.program.err_cond");
    }

    /** 行编辑器控件整体显示 / 隐藏（与列表、翻页按钮互斥）。 */
    private void setLineEditorVisible(boolean visible) {
        for (Button button : this.lineEditorButtons) {
            button.visible = visible;
        }
        if (this.prevPageButton != null) {
            this.prevPageButton.visible = !visible;
        }
        if (this.nextPageButton != null) {
            this.nextPageButton.visible = !visible;
        }
        if (this.progSaveButton != null) {
            this.progSaveButton.visible = !visible;
        }
        if (this.exprBox != null) {
            this.exprBox.visible = visible;
            if (!visible) {
                this.exprBox.setFocused(false);
                this.exprBox.closeSuggestions();
            }
        }
        if (this.condBox != null) {
            this.condBox.visible = visible;
            if (!visible) {
                this.condBox.setFocused(false);
                this.condBox.closeSuggestions();
            }
        }
        if (!visible) {
            this.closeFieldSuggest();
            for (EditBox box : this.lineEditorBoxes) {
                box.visible = false;
                box.setFocused(false);
            }
            return;
        }
        for (EditBox box : this.lineEditorBoxes) {
            box.visible = true;
        }
        this.updateLineWidgets();
    }

    /** 刷新行编辑器按钮文案。 */
    private void updateLineWidgets() {
        if (this.editingLine < 0) {
            return;
        }
        this.enableButton.setMessage(Component.translatable("gui.cc_rc.program.enabled",
                Component.translatable(this.editEnabled
                        ? "gui.cc_rc.program.yes" : "gui.cc_rc.program.no")));
    }

    /** 输入框内容变化：刷新控件可见性并即时校验。 */
    private void onEditorChanged() {
        this.updateLineWidgets();
        if (!this.loadingLine) {
            this.refreshLineError();
        }
    }

    /** 即时校验：输入的名称不存在、类型不符或表达式语法错误时给出提示。 */
    private void refreshLineError() {
        ConsoleBusBlockEntity bus = bus();
        if (bus == null || this.editingLine < 0) {
            return;
        }
        this.lineError = "";
        // 先解析表达式与条件：解析结果同时驱动输入框着色与错误区间标红
        ProgramExprParser.Result result = this.analyzeExpr(bus);
        this.exprBox.setDiagnostics(result);
        ProgramExprParser.Result condResult = this.analyzeCond(bus);
        this.condBox.setDiagnostics(condResult);
        String targetName = this.targetBox.getValue().trim();
        if (!targetName.isEmpty()) {
            ConsoleBusBlockEntity.VarDef targetDef = resolveWritable(bus, targetName);
            if (targetDef == null) {
                this.lineError = text("gui.cc_rc.program.err_target");
                return;
            }
        }
        // 条件留空表示总是执行；非空时必须解析成功且为布尔
        if (!this.condBox.getValue().trim().isEmpty() && !condResult.ok()) {
            this.lineError = condErrorText(condResult.error);
            return;
        }
        if (!result.ok()) {
            this.lineError = this.exprErrorText(result.error);
            return;
        }
        // 语法通过后再做一次完整校验，给出赋值类型 / 嵌套深度错误
        ProgramIR.Line line = this.buildLine(bus, result, condResult);
        if (line != null) {
            ProgramIR.Line.Error error = line.validate(
                    bus.getProgramScope(), ConsoleBusBlockEntity.varSlots());
            if (error != ProgramIR.Line.Error.OK) {
                this.lineError = errorText(error);
            }
        }
    }

    /** 按名称取可写入的目标变量定义；不存在或只读返回 null。 */
    private ConsoleBusBlockEntity.VarDef resolveWritable(ConsoleBusBlockEntity bus, String name) {
        int slot = bus.findVarSlot(name);
        ConsoleBusBlockEntity.VarDef def = slot > 0 ? bus.getVarDef(slot) : null;
        if (def == null) {
            return null;
        }
        // 绑定按钮面板的变量只读，不能作为赋值目标
        if (def.compId != 0) {
            ConsoleBusBlockEntity.CompEntry comp = bus.findComp(def.compId);
            if (comp != null && !comp.type.supportsVariableWrite()) {
                return null;
            }
        }
        return def;
    }

    /** 按当前输入框构建程序行（复用调用方已解析的结果）；内容不完整或非法返回 null。 */
    private ProgramIR.Line buildLine(ConsoleBusBlockEntity bus,
                                     ProgramExprParser.Result result,
                                     ProgramExprParser.Result condResult) {
        String targetName = this.targetBox.getValue().trim();
        ConsoleBusBlockEntity.VarDef targetDef = resolveWritable(bus, targetName);
        int targetSlot = targetDef == null ? -1 : bus.findVarSlot(targetName);
        if (targetSlot <= 0) {
            return null;
        }
        int condSlot = 0;
        ProgramIR.Expr condExpr = null;
        String condText = this.condBox.getValue().trim();
        if (!condText.isEmpty()) {
            // 新版条件表达式；失败返回 null 由调用方处理
            if (condResult == null || !condResult.ok()) {
                return null;
            }
            condExpr = condResult.expr;
        }
        if (result == null || !result.ok()) {
            return null;
        }
        // 原文一并保存，之后回显保留玩家自己的换行与写法
        return new ProgramIR.Line(this.editEnabled, ProgramIR.Cond.ALWAYS, condSlot, targetSlot,
                result.expr, this.commentBox.getValue().trim(), this.exprBox.getValue(),
                condExpr, condText);
    }

    /** 确认当前行：本地先校验，再提交整张程序表。 */
    private void confirmLine() {
        ConsoleBusBlockEntity bus = bus();
        if (bus == null || this.editingLine < 0) {
            return;
        }
        if (this.locked(bus)) {
            this.lineError = text("message.cc_rc.controller.prog_locked");
            return;
        }
        String targetName = this.targetBox.getValue().trim();
        if (targetName.isEmpty()) {
            this.lineError = text("gui.cc_rc.program.pick_var");
            return;
        }
        if (this.resolveWritable(bus, targetName) == null) {
            this.lineError = text("gui.cc_rc.program.err_target");
            return;
        }
        // 条件语法错误时同样禁止提交（留空表示总是执行）
        ProgramExprParser.Result condResult = this.analyzeCond(bus);
        this.condBox.setDiagnostics(condResult);
        if (!this.condBox.getValue().trim().isEmpty() && !condResult.ok()) {
            this.lineError = condErrorText(condResult.error);
            return;
        }
        // 表达式语法错误时禁止提交，并把错误区间标红
        ProgramExprParser.Result result = this.analyzeExpr(bus);
        this.exprBox.setDiagnostics(result);
        if (!result.ok()) {
            this.lineError = this.exprErrorText(result.error);
            return;
        }
        ProgramIR.Line line = this.buildLine(bus, result, condResult);
        if (line == null) {
            this.lineError = text("gui.cc_rc.program.err_expr");
            return;
        }
        ProgramIR.Line.Error error =
                line.validate(bus.getProgramScope(), ConsoleBusBlockEntity.varSlots());
        if (error != ProgramIR.Line.Error.OK) {
            this.lineError = this.errorText(error);
            return;
        }
        ProgramIR.Line[] snapshot = this.snapshotLines(bus);
        snapshot[this.editingLine] = line;
        this.sendProgram(snapshot);
        this.closeLineEditor();
    }

    /** 校验错误 → 界面提示文案。 */
    private static String errorText(ProgramIR.Line.Error error) {
        return switch (error) {
            case BAD_TARGET -> text("gui.cc_rc.program.err_target");
            case BAD_COND -> text("gui.cc_rc.program.err_cond");
            case TYPE_MISMATCH -> text("gui.cc_rc.program.err_type");
            case TOO_DEEP -> text("gui.cc_rc.program.err_deep");
            default -> text("gui.cc_rc.program.err_expr");
        };
    }

    /** 已定义的变量槽位（升序）。 */
    private List<Integer> definedSlots(ConsoleBusBlockEntity bus) {
        List<Integer> result = new ArrayList<>();
        for (int slot = 1; slot < ConsoleBusBlockEntity.varSlots(); slot++) {
            if (bus.getVarDef(slot) != null) {
                result.add(slot);
            }
        }
        return result;
    }

    /** 绘制右侧程序行列表（只画当前页）。 */
    private void renderProgramRows(GuiGraphics graphics, int mouseX, int mouseY) {
        ConsoleBusBlockEntity bus = bus();
        ProgramIR.Scope scope = bus == null ? null : bus.getProgramScope();
        int x = this.leftPos + RIGHT_X;
        int top = this.topPos + PROG_LIST_Y;
        // 悬停在行文字上时显示的完整表达式（文字被截断后仍可读到全文）
        List<Component> tip = null;
        graphics.fill(x, top, x + RIGHT_W, top + PROG_LIST_H, COLOR_LIST_BG);
        for (int row = 0; row < ROWS_PER_PAGE; row++) {
            int index = this.page * ROWS_PER_PAGE + row;
            int y = top + row * LINE_H;
            boolean hover = mouseX >= x && mouseX < x + RIGHT_W
                    && mouseY >= y && mouseY < y + LINE_H;
            ProgramIR.Line line = bus == null ? null : bus.getProgramLine(index);
            boolean isMoveSource = index == this.moveSource;
            graphics.fill(x, y, x + RIGHT_W, y + LINE_H - 1,
                    isMoveSource ? COLOR_ROW_MOVE
                            : hover ? COLOR_ROW_HOVER : (line == null ? COLOR_ROW_EMPTY : COLOR_ROW));
            String number = pad(index);
            graphics.drawString(this.font, number, x + 3, y + 4, COLOR_DIM_LIGHT, false);
            int tx = this.checkMarkX(index, x);
            if (line == null) {
                // 移动 / 复制放置模式下把空行标成可放置目标
                String key = this.pendingCopy != null
                        ? "gui.cc_rc.program.paste_target"
                        : this.moveSource >= 0
                        ? "gui.cc_rc.program.move_target"
                        : "gui.cc_rc.program.empty_line";
                boolean dropTarget = this.moveSource >= 0 || this.pendingCopy != null;
                graphics.drawString(this.font, Component.translatable(key),
                        tx, y + 4, dropTarget ? COLOR_MOVE_TEXT : COLOR_DIM_LIGHT, false);
                continue;
            }
            // 勾选区：✓ 表示启用，· 表示禁用
            graphics.drawString(this.font, line.enabled ? "✓" : "·", tx, y + 4,
                    line.enabled ? COLOR_TEXT_LIGHT : COLOR_DIM_LIGHT, false);
            int textX = tx + 9;
            int avail = x + RIGHT_W - LINE_MOVE_W - textX - 2;
            // 上方：表达式（按词法着色：变量按类型、常量黄、算符品红）
            List<StyledText> body = this.lineSegments(line, scope, bus, true);
            this.drawSegments(graphics, body, textX, y + 4, Math.max(0, avail));
            // 下方：注释（可为空，灰色）
            if (!line.comment.isEmpty()) {
                graphics.drawString(this.font,
                        this.font.plainSubstrByWidth(line.comment, Math.max(0, avail)),
                        textX, y + 14, COLOR_EDIT_COMMENT, false);
            }
            int moveX = x + RIGHT_W - LINE_MOVE_W;
            boolean hoverMove = hover && mouseX >= moveX;
            // 「移」：进入移动模式后，再点一个空行即把本行搬过去
            graphics.drawString(this.font,
                    Component.translatable("gui.cc_rc.program.move_short"),
                    moveX + 2, y + 7,
                    hoverMove || isMoveSource ? COLOR_MOVE_TEXT : COLOR_DIM_LIGHT, false);
            // 悬停在行文字区域（排除右侧移动区）时显示完整表达式与注释
            if (hover && !hoverMove && mouseX >= textX) {
                tip = new ArrayList<>();
                // 悬浮提示是深色底，用亮一档的配色
                tip.add(styledLine(this.lineSegments(line, scope, bus, true)));
                if (!line.comment.isEmpty()) {
                    tip.add(Component.literal(line.comment).withStyle(
                            Style.EMPTY.withColor(TextColor.fromRgb(COLOR_EDIT_COMMENT & 0xFFFFFF))));
                }
            }
        }
        // 页码显示在两个翻页按钮之间
        String pageText = (this.page + 1) + " / "
                + (ConsoleBusBlockEntity.MAX_PROGRAM_LINES / ROWS_PER_PAGE);
        int px = this.leftPos + PROG_PREV_X + PAGE_BTN_W + 4;
        graphics.drawString(this.font, pageText,
                px + (26 - this.font.width(pageText)) / 2,
                this.topPos + PROG_BAR_Y + 6, COLOR_DIM, false);
        if (tip != null) {
            graphics.renderComponentTooltip(this.font, tip, mouseX, mouseY);
        }
    }

    // ---------------------------------------------------------------- 语法着色

    /** 一段带颜色的文字。 */
    private record StyledText(String text, int color) {
    }

    /** 变量文字颜色：布尔蓝、浮点绿；绑定设备时用更深的一档。 */
    private int varColor(VarDef def, boolean bright) {
        if (def == null) {
            return COLOR_EDIT_TEXT;
        }
        boolean bound = def.isBound();
        if (def.type == VarType.BOOL) {
            return bound ? COLOR_EDIT_BOOL_BOUND : COLOR_EDIT_BOOL;
        }
        return bound ? COLOR_EDIT_FLOAT_BOUND : COLOR_EDIT_FLOAT;
    }

    /** 按槽位取变量颜色；槽位未定义时返回普通文字色。 */
    private int varColor(ConsoleBusBlockEntity bus, int slot, boolean bright) {
        VarDef def = bus == null ? null : bus.getVarDef(slot);
        return this.varColor(def, bright);
    }

    private static int operatorColor(boolean bright) {
        return COLOR_EDIT_OPERATOR;
    }

    private static int constColor(boolean bright) {
        return COLOR_EDIT_CONST;
    }

    /** 程序行的彩色分段：条件 / 目标 / 表达式，各元素按语法着色。 */
    private List<StyledText> lineSegments(ProgramIR.Line line, ProgramIR.Scope scope,
                                          ConsoleBusBlockEntity bus, boolean bright) {
        List<StyledText> segs = new ArrayList<>();
        if (line == null || scope == null) {
            return segs;
        }
        int opColor = operatorColor(bright);
        segs.add(new StyledText("[", opColor));
        if (line.condExpr != null) {
            // 新版条件表达式：按树着色
            this.appendExprSegments(line.condExpr, scope, bus, bright, segs);
        } else if (line.cond == ProgramIR.Cond.ALWAYS) {
            segs.add(new StyledText("true", constColor(bright)));
        } else {
            if (line.cond == ProgramIR.Cond.FALSE_VAR) {
                segs.add(new StyledText("!", opColor));
            }
            segs.add(new StyledText(scope.nameOf(line.condSlot),
                    this.varColor(bus, line.condSlot, bright)));
        }
        segs.add(new StyledText("] ", opColor));
        segs.add(new StyledText(scope.nameOf(line.targetSlot),
                this.varColor(bus, line.targetSlot, bright)));
        segs.add(new StyledText(" = ", opColor));
        this.appendExprSegments(line.expr, scope, bus, bright, segs);
        return segs;
    }

    /** 递归展开表达式：与 Expr.toText 的结构一致，但按节点类型着色。 */
    private void appendExprSegments(ProgramIR.Expr expr, ProgramIR.Scope scope,
                                    ConsoleBusBlockEntity bus, boolean bright,
                                    List<StyledText> out) {
        if (expr == null) {
            return;
        }
        int opColor = operatorColor(bright);
        switch (expr.op) {
            case FLOAT:
                out.add(new StyledText(ProgramIR.Expr.formatNumber(expr.number),
                        constColor(bright)));
                break;
            case BOOL:
                out.add(new StyledText(expr.flag ? "1" : "0", constColor(bright)));
                break;
            case VAR:
                out.add(new StyledText(scope == null ? "#" + expr.slot : scope.nameOf(expr.slot),
                        this.varColor(bus, expr.slot, bright)));
                break;
            case NEG:
                out.add(new StyledText("-", opColor));
                this.appendWrappedSegments(expr.left, scope, bus, bright, out);
                break;
            case NOT:
                out.add(new StyledText("!", opColor));
                this.appendWrappedSegments(expr.left, scope, bus, bright, out);
                break;
            case CAST:
                out.add(new StyledText("float", opColor));
                out.add(new StyledText("(", opColor));
                this.appendExprSegments(expr.left, scope, bus, bright, out);
                out.add(new StyledText(")", opColor));
                break;
            // 取整 / 取小数 / 绝对值：前缀符号 + 子表达式（子表达式为二元时加括号）
            case FLOOR:
            case FRAC:
            case ABS:
                out.add(new StyledText(ProgramIR.Expr.symbol(expr.op), opColor));
                this.appendWrappedSegments(expr.left, scope, bus, bright, out);
                break;
            default:
                this.appendWrappedSegments(expr.left, scope, bus, bright, out);
                out.add(new StyledText(" " + ProgramIR.Expr.symbol(expr.op) + " ", opColor));
                this.appendWrappedSegments(expr.right, scope, bus, bright, out);
                break;
        }
    }

    /** 子节点是二元运算时加括号（与 Expr.toText 的显示规则一致）。 */
    private void appendWrappedSegments(ProgramIR.Expr expr, ProgramIR.Scope scope,
                                       ConsoleBusBlockEntity bus, boolean bright,
                                       List<StyledText> out) {
        if (expr == null) {
            return;
        }
        boolean binary = expr.op != ProgramIR.Op.FLOAT && expr.op != ProgramIR.Op.BOOL
                && expr.op != ProgramIR.Op.VAR && expr.op != ProgramIR.Op.NEG
                && expr.op != ProgramIR.Op.NOT && expr.op != ProgramIR.Op.CAST
                && expr.op != ProgramIR.Op.FLOOR && expr.op != ProgramIR.Op.FRAC
                && expr.op != ProgramIR.Op.ABS;
        if (binary) {
            out.add(new StyledText("(", operatorColor(bright)));
        }
        this.appendExprSegments(expr, scope, bus, bright, out);
        if (binary) {
            out.add(new StyledText(")", operatorColor(bright)));
        }
    }

    /** 按分段绘制彩色文字；超出 maxWidth 时逐段截断。 */
    private void drawSegments(GuiGraphics graphics, List<StyledText> segs, int x, int y,
                              int maxWidth) {
        int cursor = x;
        for (StyledText seg : segs) {
            int remain = maxWidth - (cursor - x);
            if (remain <= 0) {
                return;
            }
            String text = this.font.plainSubstrByWidth(seg.text(), remain);
            if (!text.isEmpty()) {
                graphics.drawString(this.font, text, cursor, y, seg.color(), false);
                cursor += this.font.width(text);
            }
            if (text.length() < seg.text().length()) {
                return;
            }
        }
    }

    /** 分段转成带颜色的文本组件（悬浮提示用）。 */
    private static Component styledLine(List<StyledText> segs) {
        MutableComponent text = Component.empty();
        for (StyledText seg : segs) {
            text.append(Component.literal(seg.text()).withStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb(seg.color() & 0xFFFFFF))));
        }
        return text;
    }

    /** 设置输入框自身文字的颜色：作为底色，保证编辑界面一定看得到语法着色。 */
    private void syncBoxTextColor(EditBox box) {
        if (box == this.commentBox) {
            return;
        }
        ConsoleBusBlockEntity bus = bus();
        if (box == this.targetBox) {
            int slot = bus == null ? 0 : bus.findVarSlot(box.getValue().trim());
            int color = this.varColor(bus, slot, true);
            box.setTextColor(color);
            box.setTextColorUneditable(color);
        }
    }

    /** 输入框内文字按词法着色：目标框只放变量名（注释由输入框自身灰色绘制）。 */
    private void renderBoxTokenColors(GuiGraphics graphics, EditBox box) {
        String value = box.getValue();
        if (value.isEmpty() || box == this.commentBox) {
            return;
        }
        List<StyledText> segs = new ArrayList<>();
        if (box == this.targetBox) {
            // 目标框只放变量名
            ConsoleBusBlockEntity bus = bus();
            int slot = bus == null ? 0 : bus.findVarSlot(value.trim());
            segs.add(new StyledText(value, this.varColor(bus, slot, true)));
        } else {
            return;
        }
        this.overlayBoxText(graphics, box, segs);
    }

    /**
     * 在输入框文字位置覆盖绘制彩色文字。
     * 文字超出可视宽度时输入框会自行横向滚动，此时不覆盖，避免与滚动后的文字错位。
     */
    private void overlayBoxText(GuiGraphics graphics, EditBox box, List<StyledText> segs) {
        if (segs.isEmpty()) {
            return;
        }
        String value = box.getValue();
        if (this.font.width(value) > box.getInnerWidth() - 1) {
            return;
        }
        // 与原版 EditBox 的文字位置一致：左侧内边距 4，垂直居中
        this.drawSegments(graphics, segs, box.getX() + 4,
                box.getY() + (box.getHeight() - 8) / 2, box.getInnerWidth());
    }

    /** 补全候选文字颜色：变量按类型、常量黄（下拉是深色底，用亮色）。 */
    private int suggestColor(String candidate) {
        if (isNumber(candidate) || "true".equalsIgnoreCase(candidate)
                || "false".equalsIgnoreCase(candidate)) {
            return COLOR_EDIT_CONST;
        }
        ConsoleBusBlockEntity bus = bus();
        int slot = bus == null ? 0 : bus.findVarSlot(candidate);
        return slot > 0 ? this.varColor(bus, slot, true) : COLOR_SUGGEST_TEXT;
    }

    private static boolean isNumber(String text) {
        try {
            Float.parseFloat(text);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /**
     * 绘制最近一次执行的运行期警告（列表底部一行，悬停显示全部）。
     */
    private void renderRunWarnings(GuiGraphics graphics, int mouseX, int mouseY) {
        ConsoleBusBlockEntity bus = bus();
        if (bus == null || bus.getRunWarnings().isEmpty()) {
            return;
        }
        List<ProgramVM.Warning> warnings = bus.getRunWarnings();
        String first = warningText(warnings.get(0));
        String line = "⚠ " + first;
        if (warnings.size() > 1) {
            line = line + " +" + (warnings.size() - 1);
        }
        int x = this.leftPos + RIGHT_X;
        int y = this.topPos + GUI_H - 11;
        graphics.drawString(this.font, this.font.plainSubstrByWidth(line, RIGHT_W),
                x, y, COLOR_ERROR, false);
        if (mouseX >= x && mouseY >= y && mouseY < y + 9) {
            List<Component> tip = new ArrayList<>();
            for (ProgramVM.Warning warning : warnings) {
                tip.add(Component.literal(warningText(warning)));
            }
            graphics.renderComponentTooltip(this.font, tip, mouseX, mouseY);
        }
    }

    /** 运行期警告文案（来源失效没有行号）。 */
    private static String warningText(ProgramVM.Warning warning) {
        return switch (warning.reason) {
            case DIV_ZERO -> Component.translatable(
                    "gui.cc_rc.program.warn_div_zero", warning.line).getString();
            case NON_FINITE -> Component.translatable(
                    "gui.cc_rc.program.warn_non_finite", warning.line).getString();
            case SOURCE_INVALID -> text("gui.cc_rc.program.warn_source");
        };
    }

    /** 绘制行编辑器左侧标签与底部提示 / 报错。 */
    private void renderLineEditor(GuiGraphics graphics) {
        int x = this.leftPos + RIGHT_X + EDIT_PAD_X;
        int innerW = RIGHT_W - EDIT_PAD_X * 2;
        int top = this.topPos;
        // 两字标签与输入框垂直居中对齐
        this.drawEditorLabel(graphics, "gui.cc_rc.program.label_cond", x, top + EDIT_COND_Y);
        this.drawEditorLabel(graphics, "gui.cc_rc.program.label_target", x, top + EDIT_TARGET_Y);
        this.drawEditorLabel(graphics, "gui.cc_rc.program.label_expr", x, top + EDIT_EXPR_Y);
        this.drawEditorLabel(graphics, "gui.cc_rc.program.label_comment", x, top + EDIT_COMMENT_Y);
        if (!this.lineError.isEmpty()
                && !this.isCoveredBySuggest(x, top + EDIT_MSG_Y, innerW, 18)) {
            this.drawWrapped(graphics, this.lineError, x,
                    top + EDIT_MSG_Y, innerW, 2, COLOR_ERROR);
        }
    }

    /** 在输入框左侧绘制标签（被补全下拉遮挡时不绘制）。 */
    private void drawEditorLabel(GuiGraphics graphics, String key, int x, int y) {
        if (this.isCoveredBySuggest(x, y, 22, 10)) {
            return;
        }
        graphics.drawString(this.font, Component.translatable(key), x, y + 3, COLOR_DIM, false);
    }

    // ---------------------------------------------------------------- 行编辑器自动补全

    /** 绑定输入框的自动补全：输入变化时刷新候选，同时保留原有的即时校验。 */
    private void bindSuggest(EditBox box, Supplier<List<String>> source) {
        this.fieldSuggestSources.put(box, source);
        box.setResponder(value -> {
            this.onEditorChanged();
            this.refreshFieldSuggest(box);
        });
    }

    /** 点击输入框：聚焦并展开候选（空输入时列出全部）。 */
    private void openFieldSuggest(EditBox box) {
        if (this.loadingLine || !this.fieldSuggestSources.containsKey(box)) {
            return;
        }
        this.fieldSuggestBox = box;
        this.fieldSuggestOffset = 0;
        this.refreshFieldSuggest(box);
    }

    /** 按输入内容过滤候选项；不满足条件时收起下拉。 */
    private void refreshFieldSuggest(EditBox box) {
        if (this.loadingLine || box == null || !box.isFocused() || !box.visible) {
            return;
        }
        Supplier<List<String>> source = this.fieldSuggestSources.get(box);
        if (source == null) {
            return;
        }
        String typed = box.getValue().trim();
        String lower = typed.toLowerCase(Locale.ROOT);
        List<String> pool = source.get();
        this.fieldSuggestions.clear();
        for (String candidate : pool) {
            String nameLower = candidate.toLowerCase(Locale.ROOT);
            // 完全一致的项已经有效，不再作为候选
            if (nameLower.equals(lower) || !nameLower.startsWith(lower)) {
                continue;
            }
            this.fieldSuggestions.add(candidate);
        }
        this.fieldSuggestOffset = Math.max(0, Math.min(
                Math.max(0, this.fieldSuggestions.size() - SUGGEST_ROWS), this.fieldSuggestOffset));
        this.fieldSuggestBox = this.fieldSuggestions.isEmpty() ? null : box;
    }

    /** 收起行编辑器自动补全下拉。 */
    private void closeFieldSuggest() {
        this.fieldSuggestBox = null;
        this.fieldSuggestions.clear();
        this.fieldSuggestOffset = 0;
    }

    /** 下拉是否实际绘制中。 */
    private boolean fieldSuggestVisible() {
        return this.editingLine >= 0 && this.fieldSuggestBox != null
                && this.fieldSuggestBox.visible && !this.fieldSuggestions.isEmpty();
    }

    /** 下拉左边界：对齐到输入框左侧。 */
    private int fieldSuggestX() {
        return this.fieldSuggestBox.getX();
    }

    private int fieldSuggestW() {
        int panelRight = this.leftPos + RIGHT_X + RIGHT_W - EDIT_PAD_X;
        return Math.max(60, Math.min(this.fieldSuggestBox.getWidth(),
                panelRight - this.fieldSuggestX()));
    }

    private int fieldSuggestRowCount() {
        int rows = Math.min(SUGGEST_ROWS, this.fieldSuggestions.size());
        // 按上下可用空间收缩行数，避免下拉被挤出界面
        int below = this.topPos + GUI_H - 2 - (this.fieldSuggestBox.getY()
                + this.fieldSuggestBox.getHeight() + 1);
        int above = this.fieldSuggestBox.getY() - 1 - (this.topPos + 2);
        int space = Math.max(below, above);
        if (space > 0) {
            rows = Math.min(rows, Math.max(1, space / SUGGEST_ROW_H));
        }
        return rows;
    }

    /** 下拉上边界：默认贴输入框下方，空间不足改到上方，保证不遮挡正在编辑的输入框。 */
    private int fieldSuggestTop() {
        int height = this.fieldSuggestRowCount() * SUGGEST_ROW_H + 2;
        int below = this.fieldSuggestBox.getY() + this.fieldSuggestBox.getHeight() + 1;
        int minTop = this.topPos + 2;
        int maxTop = this.topPos + GUI_H - 2 - height;
        if (below <= maxTop) {
            return below;
        }
        int above = this.fieldSuggestBox.getY() - 1 - height;
        if (above >= minTop) {
            return above;
        }
        return Math.max(minTop, Math.min(maxTop, below));
    }

    private boolean isInsideFieldSuggest(double mouseX, double mouseY) {
        if (!this.fieldSuggestVisible()) {
            return false;
        }
        int x = this.fieldSuggestX();
        int top = this.fieldSuggestTop();
        int w = this.fieldSuggestW();
        int h = this.fieldSuggestRowCount() * SUGGEST_ROW_H + 2;
        return mouseX >= x && mouseX < x + w && mouseY >= top && mouseY < top + h;
    }

    /** 鼠标命中的候选下标；未命中返回 -1。 */
    private int fieldSuggestionAt(double mouseX, double mouseY) {
        if (!this.isInsideFieldSuggest(mouseX, mouseY)) {
            return -1;
        }
        int rel = (int) (mouseY - this.fieldSuggestTop() - 1);
        if (rel < 0) {
            return -1;
        }
        int row = rel / SUGGEST_ROW_H + this.fieldSuggestOffset;
        return row >= 0 && row < this.fieldSuggestions.size() ? row : -1;
    }

    /** 鼠标命中的可补全输入框；未命中返回 null。 */
    private EditBox suggestBoxAt(double mouseX, double mouseY) {
        for (EditBox box : this.fieldSuggestSources.keySet()) {
            if (box.visible && !this.coveredBySuggest(box) && box.isMouseOver(mouseX, mouseY)) {
                return box;
            }
        }
        return null;
    }

    /**
     * 输入框是否被补全下拉遮挡。
     * GUI 的文字批次总是晚于色块绘制，底板无法压住已绘制的文字，
     * 因此下拉展开时直接忽略被遮挡的输入框（不绘制、不响应点击）。
     */
    private boolean coveredBySuggest(EditBox box) {
        if (box == this.fieldSuggestBox) {
            return false;
        }
        return this.isCoveredBySuggest(box.getX(), box.getY(), box.getWidth(), box.getHeight());
    }

    /** 指定矩形是否与当前展开的补全下拉相交。 */
    private boolean isCoveredBySuggest(int x, int y, int w, int h) {
        if (!this.fieldSuggestVisible()) {
            return false;
        }
        int sx = this.fieldSuggestX();
        int sy = this.fieldSuggestTop();
        int sw = this.fieldSuggestW();
        int sh = this.fieldSuggestRowCount() * SUGGEST_ROW_H + 2;
        return x < sx + sw && x + w > sx && y < sy + sh && y + h > sy;
    }

    /** 下拉展开时隐藏被遮挡的「确定 / 取消」按钮。 */
    private void syncEditorButtons() {
        boolean editor = this.editingLine >= 0;
        Button[] buttons = {this.lineOkButton, this.lineCancelButton};
        for (Button button : buttons) {
            if (button == null) {
                continue;
            }
            button.visible = editor && !this.isCoveredBySuggest(
                    button.getX(), button.getY(), button.getWidth(), button.getHeight());
        }
    }

    /** 应用候选项：写入输入框并收起下拉。 */
    private void applyFieldSuggestion(int index) {
        if (this.fieldSuggestBox == null || index < 0 || index >= this.fieldSuggestions.size()) {
            return;
        }
        this.fieldSuggestBox.setValue(this.fieldSuggestions.get(index));
        this.closeFieldSuggest();
    }

    /** 绘制行编辑器自动补全下拉（最后绘制）。 */
    private void renderFieldSuggest(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!this.fieldSuggestVisible()) {
            return;
        }
        int x = this.fieldSuggestX();
        int top = this.fieldSuggestTop();
        int w = this.fieldSuggestW();
        int rows = this.fieldSuggestRowCount();
        int h = rows * SUGGEST_ROW_H + 2;
        graphics.fill(x - 1, top - 1, x + w + 1, top + h + 1, COLOR_SUGGEST_BORDER);
        graphics.fill(x, top, x + w, top + h, COLOR_SUGGEST_BG);
        for (int row = 0; row < rows; row++) {
            int index = row + this.fieldSuggestOffset;
            if (index >= this.fieldSuggestions.size()) {
                break;
            }
            int rowY = top + 1 + row * SUGGEST_ROW_H;
            boolean hover = mouseX >= x && mouseX < x + w
                    && mouseY >= rowY && mouseY < rowY + SUGGEST_ROW_H;
            if (hover) {
                graphics.fill(x, rowY, x + w, rowY + SUGGEST_ROW_H, COLOR_SUGGEST_HOVER_BG);
            }
            String candidate = this.fieldSuggestions.get(index);
            graphics.drawString(this.font,
                    this.font.plainSubstrByWidth(candidate, w - 8),
                    x + 3, rowY + 2,
                    hover ? COLOR_SUGGEST_HOVER : this.suggestColor(candidate), false);
        }
        // 内容超出可视行数时右侧画滚动条
        if (this.fieldSuggestions.size() > rows) {
            int barX = x + w - 2;
            int trackH = rows * SUGGEST_ROW_H;
            graphics.fill(barX, top + 1, barX + 2, top + 1 + trackH, 0x30FFFFFF);
            int thumbH = Math.max(6, trackH * rows / this.fieldSuggestions.size());
            int maxOffset = this.fieldSuggestions.size() - rows;
            int travel = trackH - thumbH;
            int thumbY = top + 1 + travel * this.fieldSuggestOffset / Math.max(1, maxOffset);
            graphics.fill(barX, thumbY, barX + 2, thumbY + thumbH, 0xFFA0A0A0);
        }
        // 悬停在变量候选上时显示完整变量名与绑定设备（与左侧变量表一致）
        this.renderFieldSuggestTooltip(graphics, mouseX, mouseY);
    }

    /** 自动补全变量候选的悬浮提示：完整变量名 + 绑定设备（代号与类型中文名）。 */
    private void renderFieldSuggestTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int index = this.fieldSuggestionAt(mouseX, mouseY);
        if (index < 0) {
            return;
        }
        String candidate = this.fieldSuggestions.get(index);
        ConsoleBusBlockEntity bus = bus();
        int slot = bus == null ? -1 : bus.findVarSlot(candidate);
        VarDef def = bus == null || slot <= 0 ? null : bus.getVarDef(slot);
        if (def == null) {
            return;
        }
        List<Component> tooltip = new ArrayList<>();
        // 变量名按类型着色（悬浮提示是深色底，用亮色）
        tooltip.add(Component.literal(def.name).withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(this.varColor(def, true) & 0xFFFFFF))));
        ConsoleBusBlockEntity.CompEntry bound = bus.findComp(def.compId);
        // 触发变量显示第二名称，普通变量显示主代号
        String boundName = bound == null ? "" : (def.trigger ? bound.code2 : bound.code);
        tooltip.add(bound != null
                ? Component.translatable("gui.cc_rc.program.bound",
                        boundName + "（"
                                + Component.translatable(bound.type.translationKey()).getString()
                                + "）")
                : Component.translatable("gui.cc_rc.program.unbound"));
        graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
    }

    /** 已定义变量名（boolOnly 只保留布尔，writableOnly 只保留可写入）。 */
    private List<String> variableNames(boolean boolOnly, boolean writableOnly) {
        ConsoleBusBlockEntity bus = bus();
        List<String> names = new ArrayList<>();
        if (bus == null) {
            return names;
        }
        for (int slot = 1; slot < ConsoleBusBlockEntity.varSlots(); slot++) {
            VarDef def = bus.getVarDef(slot);
            if (def == null) {
                continue;
            }
            if (boolOnly && def.type != VarType.BOOL) {
                continue;
            }
            if (writableOnly && this.resolveWritable(bus, def.name) == null) {
                continue;
            }
            names.add(def.name);
        }
        names.sort(Comparator.comparing(name -> name.toLowerCase(Locale.ROOT)));
        return names;
    }

    // ---------------------------------------------------------------- 其它

    /**
     * 关闭界面：默认保存一次（Esc / 背包关闭同样保存）；
     * 点「放弃」时 {@link #discarding} 为 true，跳过保存。
     */
    @Override
    public void onClose() {
        if (!this.discarding) {
            this.saveValues();
        }
        super.onClose();
    }

    /** 放弃未保存的修改并关闭界面。 */
    private void discardChanges() {
        this.discarding = true;
        this.pendingFloats.clear();
        this.pendingBools.clear();
        this.cancelValueEdit();
        this.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 变量表单与行编辑器可能同时打开：按键按当前焦点所在面板处理
        boolean lineActive = this.editingLine >= 0
                && (this.createSlot < 0 || this.lineEditorFocused());
        boolean formActive = this.createSlot >= 0
                && (this.editingLine < 0 || this.nameBox.isFocused());
        boolean valueActive = this.editingSlot >= 0 && this.valueBox.isFocused();

        // 程序列表的移动 / 复制放置模式：Esc 取消
        if (this.editingLine < 0 && (this.moveSource >= 0 || this.pendingCopy != null)
                && keyCode == 256) {
            this.moveSource = -1;
            this.pendingCopy = null;
            return true;
        }

        // 行编辑器打开时：Enter 确认，Esc 关闭编辑器（不关界面）
        if (lineActive) {
            if (this.condBox.isFocused()) {
                // 条件框（单行）：有候选时回车先应用候选，其余按键交给它
                if (this.condBox.keyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }
                if (keyCode == 257 || keyCode == 335) {
                    this.confirmLine();
                    return true;
                }
                if (keyCode == 256) {
                    this.closeLineEditor();
                    return true;
                }
            } else if (this.exprBox.isFocused()) {
                // 表达式框内：Ctrl+Enter 提交；其余按键（回车换行、Tab 补全、Esc 收候选）交给它
                if ((keyCode == 257 || keyCode == 335) && Screen.hasControlDown()) {
                    this.confirmLine();
                    return true;
                }
                if (this.exprBox.keyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }
                if (keyCode == 256) {
                    this.closeLineEditor();
                    return true;
                }
            } else {
                // Esc 先收起自动补全下拉
                if (keyCode == 256 && this.fieldSuggestVisible()) {
                    this.closeFieldSuggest();
                    return true;
                }
                if (keyCode == 257 || keyCode == 335) {
                    this.confirmLine();
                    return true;
                }
                if (keyCode == 256) {
                    this.closeLineEditor();
                    return true;
                }
            }
        }
        // 新建表单活动时：Enter 确认新建，Esc 收下拉 / 关闭表单
        if (formActive) {
            if (keyCode == 257 || keyCode == 335) {
                this.confirmCreate();
                return true;
            }
            if (keyCode == 256) {
                if (this.suggestOpen) {
                    this.suggestOpen = false;
                    this.syncTypeButtonVisibility();
                } else {
                    this.closeCreate();
                }
                return true;
            }
        }
        // Enter：提交数值编辑
        if (valueActive && (keyCode == 257 || keyCode == 335)) {
            this.commitValueEdit();
            return true;
        }
        // 拦截背包键：编辑界面不应被背包键关闭
        if (this.minecraft != null
                && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** 行编辑器的任一输入框（含条件 / 多行表达式框）是否持有焦点。 */
    private boolean lineEditorFocused() {
        if (this.exprBox != null && this.exprBox.isFocused()) {
            return true;
        }
        if (this.condBox != null && this.condBox.isFocused()) {
            return true;
        }
        for (EditBox box : this.lineEditorBoxes) {
            if (box.isFocused()) {
                return true;
            }
        }
        return false;
    }

    /** 客户端本地提示（不经过服务端）；args 用于填充 %s 占位。 */
    private void localHint(String key, Object... args) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                    Component.translatable(key, args), true);
        }
    }

    /** 槽位号补零显示。 */
    private static String pad(int slot) {
        return String.format(Locale.ROOT, "%02d", slot);
    }

    /** 翻译文本的纯字符串形式（按钮文案与提示复用）。 */
    private static String text(String key) {
        return Component.translatable(key).getString();
    }

    /** 浮点显示：整数不带小数点，其余保留两位。 */
    private static String formatFloat(float value) {
        if (Math.abs(value - Math.round(value)) < 0.0001F) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
