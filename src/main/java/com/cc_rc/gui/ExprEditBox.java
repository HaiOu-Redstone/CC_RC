package com.cc_rc.gui;

import com.cc_rc.block.console.ProgramExprParser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;

/**
 * 多行表达式输入框。
 *
 * 内部复用原版 {@link MultilineTextField} 负责文本存储、光标 / 选区、软换行与键盘编辑，
 * 本类只做三件事：
 * <ul>
 *   <li>绘制：按解析结果给变量 / 常量 / 算符分别着色，错误区间标红，并画选区、光标与滚动条；</li>
 *   <li>滚动：按整行上下滚动，光标移动时自动保证可见；</li>
 *   <li>补全：取光标所在单词（变量名）或符号前缀，给出候选列表。</li>
 * </ul>
 * 换行只是原文的一部分：解析器把 {@code \n} 当空白，不影响语义。
 */
final class ExprEditBox extends AbstractWidget {

    /** 文本相对输入框的内边距。 */
    private static final int PAD = 4;
    /** 补全下拉单行高度与最多显示行数。 */
    private static final int SUGGEST_ROW_H = 12;
    private static final int SUGGEST_ROWS = 5;
    /** 竖直滚动条占用宽度（预留给滚动条，避免压住文字）。 */
    private static final int SCROLL_W = 4;

    private static final int COLOR_BG = 0xFF1A1A1A;
    private static final int COLOR_TEXT = 0xFFE0E0E0;
    private static final int COLOR_CONST = 0xFFFFE060;
    private static final int COLOR_OPERATOR = 0xFFFF66D9;
    private static final int COLOR_ERROR = 0xFFFF6B6B;
    private static final int COLOR_SELECTION = 0x663A6EA5;
    private static final int COLOR_CURSOR = 0xFFE0E0E0;
    private static final int COLOR_SCROLL_TRACK = 0xFF2A2A2A;
    private static final int COLOR_SCROLL_THUMB = 0xFF7A7A7A;
    private static final int COLOR_SUGGEST_BG = 0xFF141414;
    private static final int COLOR_SUGGEST_BORDER = 0xFF000000;
    private static final int COLOR_SUGGEST_TEXT = 0xFFCFCFCF;
    private static final int COLOR_SUGGEST_HOVER = 0xFFFFE060;
    private static final int COLOR_SUGGEST_SELECTED_BG = 0xFF3A3A6A;

    /** 补全候选里的运算符（只用于符号前缀补全，如 & → &&）。 */
    private static final String[] SYMBOLS = {
            "+", "-", "*", "/", "//", "%", "==", "!=", "<", ">", "<=", ">=", "&&", "||", "!", "#", "~", "@"
    };

    private final Font font;
    private final MultilineTextField field;
    /** 软换行宽度（与内部输入框一致，用于自算显示行区间）。 */
    private final int wrapWidth;
    /**
     * 显示行在原文中的字符区间 [begin, end)。
     * 原版 {@code MultilineTextField.StringView} 是不可访问的嵌套类型，
     * 因此这里用同样的折行参数自行计算一份。
     */
    private final List<int[]> lineRanges = new ArrayList<>();
    /** 最近一次解析结果（着色与错误区间）；null 表示尚未解析。 */
    private ProgramExprParser.Result diagnostics;
    /** 竖直滚动：首个可见显示行下标。 */
    private int scrollLine;
    /** 是否正在拖动选择。 */
    private boolean dragging;
    /** 内容变化回调（由界面重新解析并刷新提示）。 */
    private Runnable onChanged;
    /** 变量名 → 颜色（布尔蓝 / 浮点绿）；未定义退回普通文字色。 */
    private ToIntFunction<String> nameColor = name -> COLOR_TEXT;
    /** 变量名候选来源。 */
    private Supplier<List<String>> candidates = List::of;
    /** 当前补全候选与替换区间。 */
    private List<String> suggestions = List.of();
    private int suggestIndex;
    private int suggestStart;
    private int suggestEnd;
    /**
     * 候选是否由「空白 / 算符之后」自动弹出。
     * 自动弹出的列表不抢占回车：回车照常换行（表达式框）或提交（单行条件框）。
     */
    private boolean autoOpened;
    /** 空内容时的提示文字；null 表示不提示。 */
    private Component hint;
    /** 单行模式：回车不插入换行，交给界面提交（条件框使用）。 */
    private boolean singleLine;

    ExprEditBox(Font font, int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        this.font = font;
        // 右侧预留滚动条宽度，软换行按这个宽度计算
        this.wrapWidth = Math.max(8, width - PAD * 2 - SCROLL_W);
        this.field = new MultilineTextField(font, this.wrapWidth);
        this.field.setCursorListener(this::refreshSuggestions);
        this.field.setValueListener(value -> {
            this.reflowLines();
            this.refreshSuggestions();
            this.scrollToCursor();
            if (this.onChanged != null) {
                this.onChanged.run();
            }
        });
        this.reflowLines();
    }

    /**
     * 按与原版一致的规则重新计算显示行：跳过空白折行，末尾换行补一个空行。
     */
    private void reflowLines() {
        this.lineRanges.clear();
        String value = this.field.value();
        if (value.isEmpty()) {
            this.lineRanges.add(new int[]{0, 0});
            return;
        }
        this.font.getSplitter().splitLines(value, this.wrapWidth, Style.EMPTY, false,
                (style, start, end) -> this.lineRanges.add(new int[]{start, end}));
        if (value.charAt(value.length() - 1) == '\n') {
            this.lineRanges.add(new int[]{value.length(), value.length()});
        }
    }

    // ---------------------------------------------------------------- 外部接口

    /** 设置内容（会触发内容回调；光标移到末尾）。 */
    void setValue(String value) {
        this.field.setValue(value == null ? "" : value);
        this.scrollLine = 0;
        this.closeSuggestions();
        this.scrollToCursor();
    }

    String getValue() {
        return this.field.value();
    }

    /** 原文最大长度（含换行）。 */
    void setCharacterLimit(int limit) {
        this.field.setCharacterLimit(limit);
    }

    /** 更新解析结果：token 用于着色，error 区间用于标红。 */
    void setDiagnostics(ProgramExprParser.Result result) {
        this.diagnostics = result;
    }

    /** 内容变化回调。 */
    void setOnChanged(Runnable callback) {
        this.onChanged = callback;
    }

    /** 变量名着色回调。 */
    void setNameColor(ToIntFunction<String> callback) {
        this.nameColor = callback == null ? name -> COLOR_TEXT : callback;
    }

    /** 变量名候选来源。 */
    void setCandidates(Supplier<List<String>> source) {
        this.candidates = source == null ? List::of : source;
    }

    void setHint(Component hint) {
        this.hint = hint;
    }

    /** 切换单行模式（回车交给界面，不插入换行）。 */
    void setSingleLine(boolean value) {
        this.singleLine = value;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (focused) {
            // 聚焦时空内容 / 光标停在算符后：直接列出变量候选
            this.refreshSuggestions(true);
        } else {
            this.closeSuggestions();
        }
    }

    /** 按整行滚动（delta > 0 向下）。 */
    void scroll(int delta) {
        int visible = this.visibleLines();
        int max = Math.max(0, this.lineRanges.size() - visible);
        this.scrollLine = Mth.clamp(this.scrollLine + delta, 0, max);
    }

    /** 补全下拉是否展开。 */
    boolean isSuggestionVisible() {
        return this.visible && this.isFocused() && !this.suggestions.isEmpty();
    }

    /** 收起补全下拉。 */
    void closeSuggestions() {
        this.suggestions = List.of();
        this.suggestIndex = 0;
        this.autoOpened = false;
    }

    /**
     * 处理落在补全下拉上的点击。
     * 下拉绘制在输入框之外，因此由界面先转交本方法。
     */
    boolean mouseClickedSuggestion(double mouseX, double mouseY) {
        int index = this.suggestionAt(mouseX, mouseY);
        if (index >= 0) {
            this.applySuggestion(index);
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- 鼠标

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.active || button != 0) {
            return false;
        }
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        this.field.setSelecting(false);
        this.seekCursor(mouseX, mouseY);
        // 点进空内容或算符之后同样自动列出变量候选
        this.refreshSuggestions(true);
        this.dragging = true;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.dragging || button != 0) {
            return false;
        }
        this.field.setSelecting(true);
        this.seekCursor(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.dragging) {
            this.dragging = false;
            this.field.setSelecting(false);
            return true;
        }
        return false;
    }

    /** 把鼠标位置换算成文本坐标并移动光标（含滚动偏移）。 */
    private void seekCursor(double mouseX, double mouseY) {
        int textX = this.getX() + PAD;
        int textTop = this.getY() + PAD;
        double relX = mouseX - textX;
        double relY = mouseY - textTop + this.scrollLine * this.lineHeight();
        this.field.seekCursorToPoint(relX, relY);
        this.scrollToCursor();
    }

    // ---------------------------------------------------------------- 键盘

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.visible || !this.isFocused()) {
            return false;
        }
        // 下拉展开时：Tab 应用候选，上下键在候选间导航，Esc 收起
        if (this.isSuggestionVisible()) {
            if (keyCode == 258) {
                this.applySuggestion(this.suggestIndex);
                return true;
            }
            // 回车 / 小键盘回车：手动唤出的列表用于确认候选；
            // 自动弹出的列表先收起，把回车交还给换行 / 提交逻辑
            if (keyCode == 257 || keyCode == 335) {
                if (!this.autoOpened) {
                    this.applySuggestion(this.suggestIndex);
                    return true;
                }
                this.closeSuggestions();
            }
            if (keyCode == 264) {
                // 手动导航后即视为主动选择，回车可以确认候选
                this.autoOpened = false;
                this.suggestIndex = Math.min(this.suggestions.size() - 1, this.suggestIndex + 1);
                return true;
            }
            if (keyCode == 265) {
                this.autoOpened = false;
                this.suggestIndex = Math.max(0, this.suggestIndex - 1);
                return true;
            }
            if (keyCode == 256) {
                this.closeSuggestions();
                return true;
            }
        }
        // 无候选时 Tab 交给界面（不插入制表符）
        if (keyCode == 258) {
            return false;
        }
        // 单行模式：回车不换行，交给界面提交本行
        if (this.singleLine && (keyCode == 257 || keyCode == 335)) {
            return false;
        }
        // 其余键（含回车换行、退格、方向键、复制粘贴）交给原版多行输入实现
        boolean handled = this.field.keyPressed(keyCode);
        if (handled) {
            this.scrollToCursor();
        }
        return handled;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!this.visible || !this.isFocused()) {
            return false;
        }
        // 换行由回车键处理，这里忽略控制字符
        if (!SharedConstants.isAllowedChatCharacter(codePoint)) {
            return false;
        }
        this.field.insertText(String.valueOf(codePoint));
        this.scrollToCursor();
        return true;
    }

    // ---------------------------------------------------------------- 补全

    /** 按光标所在单词或符号前缀刷新候选（输入时调用）。 */
    private void refreshSuggestions() {
        this.refreshSuggestions(false);
    }

    /**
     * 刷新候选。
     *
     * @param autoOnly true 表示只允许「空白处 / 算符之后」自动列出全部候选，
     *                 用于聚焦或点击时，避免一打开已有行就弹出过滤列表
     */
    private void refreshSuggestions(boolean autoOnly) {
        this.suggestions = List.of();
        this.suggestIndex = 0;
        this.autoOpened = false;
        if (!this.visible || !this.isFocused()) {
            return;
        }
        String value = this.field.value();
        int cursor = Mth.clamp(this.field.cursor(), 0, value.length());
        int start = cursor;
        int end = cursor;
        while (start > 0 && isNamePart(value.charAt(start - 1))) {
            start--;
        }
        while (end < value.length() && isNamePart(value.charAt(end))) {
            end++;
        }
        String prefix = value.substring(start, end);
        if (prefix.isEmpty()) {
            // 空白输入框或算符之后没有字母：不要求前缀，直接列出全部变量候选
            if (!isSuggestPosition(value, cursor)) {
                return;
            }
            start = cursor;
            end = cursor;
        } else if (autoOnly) {
            // 聚焦 / 点击已有文字时不主动弹列表
            return;
        }
        List<String> matches = new ArrayList<>();
        // 符号前缀优先给出剩余部分（例如 & → &&），放在列表最前
        if (!prefix.isEmpty() && isSymbolOnly(prefix)) {
            for (String symbol : SYMBOLS) {
                if (symbol.length() > prefix.length() && symbol.startsWith(prefix)
                        && !matches.contains(symbol)) {
                    matches.add(symbol);
                }
            }
        }
        for (String name : this.candidates.get()) {
            boolean matched = prefix.isEmpty()
                    || (name.length() > prefix.length()
                    && name.regionMatches(true, 0, prefix, 0, prefix.length()));
            if (matched && !matches.contains(name)) {
                matches.add(name);
            }
        }
        if (!matches.isEmpty()) {
            this.suggestStart = start;
            this.suggestEnd = end;
            this.suggestions = matches;
            // 空前缀为自动弹出，回车不用于确认候选
            this.autoOpened = prefix.isEmpty();
        }
    }

    /**
     * 光标前（跳过空白）是文本开头或算符 / 左括号时，视为可以自动列出候选的位置。
     */
    private static boolean isSuggestPosition(String value, int cursor) {
        int i = cursor - 1;
        while (i >= 0 && Character.isWhitespace(value.charAt(i))) {
            i--;
        }
        if (i < 0) {
            return true;
        }
        return "+-*/%!=<>&|#~@(".indexOf(value.charAt(i)) >= 0;
    }

    /** 应用补全：用候选替换当前单词。 */
    private void applySuggestion(int index) {
        if (index < 0 || index >= this.suggestions.size()) {
            return;
        }
        String candidate = this.suggestions.get(index);
        String value = this.field.value();
        int start = Mth.clamp(this.suggestStart, 0, value.length());
        int end = Mth.clamp(this.suggestEnd, start, value.length());
        this.field.setValue(value.substring(0, start) + candidate + value.substring(end));
        this.field.seekCursor(Whence.ABSOLUTE, start + candidate.length());
        this.closeSuggestions();
    }

    private static boolean isNamePart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /** 前缀是否全为运算符符号字符。 */
    private static boolean isSymbolOnly(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ("+-*/%!=<>&|#~@".indexOf(c) < 0) {
                return false;
            }
        }
        return true;
    }

    private int suggestionAt(double mouseX, double mouseY) {
        if (!this.isSuggestionVisible()) {
            return -1;
        }
        int x = this.getX();
        int width = this.getWidth();
        int top = this.suggestionTop();
        int rows = Math.min(SUGGEST_ROWS, this.suggestions.size());
        if (mouseX < x || mouseX >= x + width
                || mouseY < top || mouseY >= top + rows * SUGGEST_ROW_H + 2) {
            return -1;
        }
        int rel = (int) (mouseY - top - 1);
        if (rel < 0) {
            return -1;
        }
        int row = rel / SUGGEST_ROW_H;
        return row < rows ? row : -1;
    }

    /** 下拉顶部：默认贴输入框下方，底部空间不足时改到上方。 */
    private int suggestionTop() {
        int rows = Math.min(SUGGEST_ROWS, this.suggestions.size());
        int height = rows * SUGGEST_ROW_H + 2;
        int below = this.getY() + this.getHeight() + 1;
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        if (below + height <= screenHeight - 1) {
            return below;
        }
        int above = this.getY() - 1 - height;
        return above >= 1 ? above : Math.max(1, screenHeight - 1 - height);
    }

    /** 补全下拉最后绘制，避免被输入框底板覆盖。 */
    void renderSuggestions(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!this.isSuggestionVisible()) {
            return;
        }
        int x = this.getX();
        int width = this.getWidth();
        int top = this.suggestionTop();
        int rows = Math.min(SUGGEST_ROWS, this.suggestions.size());
        int height = rows * SUGGEST_ROW_H + 2;
        graphics.fill(x - 1, top - 1, x + width + 1, top + height + 1, COLOR_SUGGEST_BORDER);
        graphics.fill(x, top, x + width, top + height, COLOR_SUGGEST_BG);
        for (int row = 0; row < rows; row++) {
            String candidate = this.suggestions.get(row);
            int rowY = top + 1 + row * SUGGEST_ROW_H;
            boolean hover = mouseX >= x && mouseX < x + width
                    && mouseY >= rowY && mouseY < rowY + SUGGEST_ROW_H;
            if (row == this.suggestIndex) {
                graphics.fill(x, rowY, x + width, rowY + SUGGEST_ROW_H,
                        COLOR_SUGGEST_SELECTED_BG);
            }
            int color = hover || row == this.suggestIndex
                    ? COLOR_SUGGEST_HOVER : this.suggestColor(candidate);
            graphics.drawString(this.font, candidate, x + 3, rowY + 2, color, false);
        }
    }

    /** 候选文字颜色：常量黄、算符品红、变量按类型。 */
    private int suggestColor(String candidate) {
        if ("true".equalsIgnoreCase(candidate) || "false".equalsIgnoreCase(candidate)) {
            return COLOR_CONST;
        }
        if (isSymbolOnly(candidate)) {
            return COLOR_OPERATOR;
        }
        int color = this.nameColor.applyAsInt(candidate);
        return color == COLOR_TEXT ? COLOR_SUGGEST_TEXT : color;
    }

    // ---------------------------------------------------------------- 绘制

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.getX();
        int y = this.getY();
        int width = this.getWidth();
        int height = this.getHeight();
        this.drawSunken(graphics, x, y, width, height);

        String value = this.field.value();
        int lineHeight = this.lineHeight();
        int textX = x + PAD;
        int textTop = y + PAD;
        int innerBottom = y + height - PAD;
        int[] colors = this.buildColors(value);
        int cursor = this.field.cursor();
        // 选区区间由“光标 + 选中文本长度”反推（原版选区对象不可访问）
        int[] selection = this.selectionRange(value, cursor);

        graphics.enableScissor(x + 1, y + 1, x + width - 1, y + height - 1);
        boolean hasText = false;
        for (int line = this.scrollLine; line < this.lineRanges.size(); line++) {
            int lineY = textTop + (line - this.scrollLine) * lineHeight;
            if (lineY >= innerBottom) {
                break;
            }
            int[] view = this.lineRanges.get(line);
            int begin = view[0];
            int end = view[1];
            if (end > begin) {
                hasText = true;
            }
            // 选区底色
            if (selection != null) {
                int selStart = Math.max(selection[0], begin);
                int selEnd = Math.min(selection[1], end);
                if (selEnd > selStart) {
                    int from = textX + this.font.width(value.substring(begin, selStart));
                    int to = textX + this.font.width(value.substring(begin, selEnd));
                    graphics.fill(from, lineY - 1, to, lineY + lineHeight - 1, COLOR_SELECTION);
                }
            }
            // 按颜色分段绘制（token 颜色数组）
            int pos = begin;
            while (pos < end) {
                int color = pos >= 0 && pos < colors.length ? colors[pos] : COLOR_TEXT;
                int next = pos + 1;
                while (next < end && colorAt(colors, next) == color) {
                    next++;
                }
                graphics.drawString(this.font, value.substring(pos, next),
                        textX + this.font.width(value.substring(begin, pos)),
                        lineY, color, false);
                pos = next;
            }
            // 光标（聚焦时闪烁）
            if (this.isFocused() && cursor >= begin && cursor <= end
                    && (System.currentTimeMillis() / 500L) % 2L == 0L) {
                int caretX = textX + this.font.width(value.substring(begin, cursor));
                graphics.fill(caretX, lineY - 1, caretX + 1, lineY + lineHeight - 1, COLOR_CURSOR);
            }
        }
        // 空内容提示
        if (!hasText && this.hint != null) {
            graphics.drawString(this.font, this.hint, textX, textTop,
                    this.isFocused() ? 0xFF7A7A7A : 0xFF6A6A6A, false);
        }
        this.drawScrollBar(graphics, x, y, width, height);
        graphics.disableScissor();
    }

    /** 深色凹槽底板（与原版输入框视觉一致）。 */
    private void drawSunken(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, COLOR_BG);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 2, y + height - 1, 0xFF373737);
        graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, 0xFF4A4A4A);
        graphics.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, 0xFF4A4A4A);
    }

    /** 内容超出可视行数时画一条竖直滚动条。 */
    private void drawScrollBar(GuiGraphics graphics, int x, int y, int width, int height) {
        int total = this.lineRanges.size();
        int visible = this.visibleLines();
        if (total <= visible) {
            return;
        }
        int trackTop = y + 2;
        int trackHeight = height - 4;
        int thumbHeight = Math.max(6, trackHeight * visible / total);
        int max = total - visible;
        int thumbTop = trackTop + (trackHeight - thumbHeight) * this.scrollLine / Math.max(1, max);
        graphics.fill(x + width - 4, trackTop, x + width - 3, trackTop + trackHeight,
                COLOR_SCROLL_TRACK);
        graphics.fill(x + width - 4, thumbTop, x + width - 3, thumbTop + thumbHeight,
                COLOR_SCROLL_THUMB);
    }

    /**
     * 选区在原文中的 [begin, end)；没有选区返回 null。
     * 光标一定在选区一端，因此用「光标 ± 选中文本长度」反推出另一端。
     */
    private int[] selectionRange(String value, int cursor) {
        if (!this.field.hasSelection()) {
            return null;
        }
        String text = this.field.getSelectedText();
        int length = text.length();
        if (length <= 0) {
            return null;
        }
        // 光标在选区末尾
        if (cursor - length >= 0 && value.startsWith(text, cursor - length)) {
            return new int[]{cursor - length, cursor};
        }
        // 光标在选区开头
        if (cursor + length <= value.length() && value.startsWith(text, cursor)) {
            return new int[]{cursor, cursor + length};
        }
        return new int[]{Math.max(0, cursor - length), Math.min(value.length(), cursor)};
    }

    /** 逐字符颜色数组：默认文字色，token 覆盖，错误区间最后覆盖为红色。 */
    private int[] buildColors(String value) {
        int[] colors = new int[value.length()];
        Arrays.fill(colors, COLOR_TEXT);
        if (this.diagnostics == null) {
            return colors;
        }
        for (ProgramExprParser.Token token : this.diagnostics.tokens) {
            int color = this.tokenColor(token);
            int from = Math.max(0, token.start);
            int to = Math.min(colors.length, token.end);
            for (int i = from; i < to; i++) {
                colors[i] = color;
            }
        }
        ProgramExprParser.Error error = this.diagnostics.error;
        if (error != null && colors.length > 0) {
            int from = Mth.clamp(error.start, 0, colors.length - 1);
            int to = Mth.clamp(error.end, from + 1, colors.length);
            for (int i = from; i < to; i++) {
                colors[i] = COLOR_ERROR;
            }
        }
        return colors;
    }

    private int tokenColor(ProgramExprParser.Token token) {
        switch (token.kind) {
            case NAME:
                return this.nameColor.applyAsInt(token.text);
            case NUMBER:
            case BOOL:
                return COLOR_CONST;
            case OP:
            case LPAREN:
            case RPAREN:
                return COLOR_OPERATOR;
            default:
                return COLOR_TEXT;
        }
    }

    private static int colorAt(int[] colors, int index) {
        return index >= 0 && index < colors.length ? colors[index] : COLOR_TEXT;
    }

    /** 可视行数（至少 1）。 */
    private int visibleLines() {
        return Math.max(1, (this.getHeight() - PAD * 2) / this.lineHeight());
    }

    private int lineHeight() {
        return this.font.lineHeight;
    }

    /** 光标移动后保证所在行可见。 */
    private void scrollToCursor() {
        int line = this.field.getLineAtCursor();
        if (line < 0) {
            return;
        }
        int visible = this.visibleLines();
        int max = Math.max(0, this.lineRanges.size() - visible);
        if (line < this.scrollLine) {
            this.scrollLine = line;
        } else if (line >= this.scrollLine + visible) {
            this.scrollLine = line - visible + 1;
        }
        this.scrollLine = Mth.clamp(this.scrollLine, 0, max);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        // 表达式输入框不提供额外朗读内容
    }
}
