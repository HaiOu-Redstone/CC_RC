package com.cc_rc.client;

import com.cc_rc.block.console.draw.ConsoleDrawPresets;
import com.cc_rc.block.console.draw.ConsoleShape;
import com.cc_rc.block.console.draw.ConsoleShapeType;
import com.cc_rc.item.DrawToolItem;
import com.cc_rc.network.DrawToolSettingsPacket;
import com.cc_rc.network.ModNetwork;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 画图工具的设置界面（shift + 右键空气打开）。
 *
 * 设置直接写到手上画图工具的 NBT，并同步给服务端（快速绘画时服务端按这份设置生成图案）：
 *  - 类型：矩形 / 空心矩形 / 线段 / 箭头 / 预设；
 *  - 快速绘画：开启后右键面板直接生成，不再打开属性界面；
 *  - 吸附整数：右键坐标四舍五入到整像素；
 *  - 平直线优先：线段 / 箭头在单方向超过另一方向 2 倍时拉成水平 / 垂直；
 *  - 颜色 / 贴图样式 / 线宽 / 缩放 / 图层。
 */
public class DrawQuickScreen extends Screen {

    /** 界面尺寸与属性界面（ConsoleDrawScreen）一致，便于两处排版对齐。 */
    private static final int W = 386;
    private static final int H = 200;

    /** 类型按钮排布（与属性界面相同：5 个等宽按钮一行）。 */
    private static final int TYPE_X = 6;
    private static final int TYPE_Y = 31;
    private static final int TYPE_W = 46;
    private static final int TYPE_H = 16;
    private static final int TYPE_STEP = 48;

    /** 三个开关（同一行）。 */
    private static final int TOGGLE_Y = 55;
    private static final int QUICK_X = 8;
    private static final int QUICK_W = 92;
    private static final int SNAP_X = 104;
    private static final int SNAP_W = 70;
    private static final int STRAIGHT_X = 178;
    private static final int STRAIGHT_W = 72;

    /** 颜色色块（8 列 × 2 行）。 */
    private static final int SWATCH_X = 8;
    private static final int SWATCH_Y = 86;
    private static final int SWATCH_W = 14;
    private static final int SWATCH_H = 12;
    private static final int SWATCH_GAP = 2;
    private static final int SWATCH_COLS = 8;

    /** 线宽 / 缩放 / 图层所在行。 */
    private static final int LABEL_Y = 120;
    private static final int INPUT_Y = 131;
    private static final int WIDTH_X = 8;
    private static final int SCALE_X = 78;
    private static final int LAYER_X = 170;
    /** 关闭按钮。 */
    private static final int CLOSE_Y = 170;
    /** 右侧贴图样式列表：4 列 × 3 行 = 12 个预览框。 */
    private static final int PRESET_X = 260;
    private static final int PRESET_Y = 32;
    private static final int PRESET_CELL = 24;
    private static final int PRESET_GAP = 4;
    private static final int PRESET_COLS = 4;
    /** 列表内最多显示的行数（图案更多时用滚轮翻看）。 */
    private static final int PRESET_VISIBLE_ROWS = 3;
    /** 列表的可视区域上下边界相对 GUI 左上角的 Y（只会画裁剪后的部分）。 */
    private static final int PRESET_AREA_TOP = 30;
    private static final int PRESET_AREA_BOTTOM = 118;

    private static final int[] DYE_COLORS = {
            0xFFFFFF, 0xF9801D, 0xC74EBD, 0x3AB3DA,
            0xFED83D, 0x80C71F, 0xF38BAA, 0x474F52,
            0x9D9D97, 0x169C9C, 0x8932B8, 0x3C44AA,
            0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21
    };

    private static final ConsoleShapeType[] TYPES = {
            ConsoleShapeType.RECT, ConsoleShapeType.RECT_OUTLINE, ConsoleShapeType.LINE,
            ConsoleShapeType.ARROW, ConsoleShapeType.PRESET
    };

    private final ItemStack stack;
    private int leftPos;
    private int topPos;

    private int color;
    /** 线宽（不能叫 width，否则遮蔽 Screen.width 屏幕宽度）。 */
    private float lineWidth;
    private float scale;
    private int layer;
    private ConsoleShapeType type;
    private String presetId;
    /** 贴图样式列表的滚动行偏移（0 = 从第一行开始）。 */
    private int presetScroll;
    private boolean quick;
    private boolean snap;
    private boolean straight;

    private EditBox hexBox;
    private EditBox widthBox;
    private EditBox scaleBox;
    private Button quickButton;
    private Button snapButton;
    private Button straightButton;

    public DrawQuickScreen(ItemStack stack) {
        super(Component.translatable("gui.cc_rc.draw.quick_title"));
        this.stack = stack;
        DrawToolItem.Settings s = DrawToolItem.readSettings(stack);
        this.color = s.color();
        this.lineWidth = s.width();
        this.scale = s.scale();
        this.layer = s.layer();
        this.type = s.type();
        this.presetId = s.preset();
        this.quick = s.quick();
        this.snap = s.snap();
        this.straight = s.straight();
    }

    /** 客户端打开设置界面。 */
    public static void open(ItemStack stack) {
        Minecraft.getInstance().setScreen(new DrawQuickScreen(stack));
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - W) / 2;
        this.topPos = (this.height - H) / 2;
        int x = this.leftPos;
        int y = this.topPos;

        // 类型按钮
        for (int i = 0; i < TYPES.length; i++) {
            final ConsoleShapeType target = TYPES[i];
            addRenderableWidget(Button.builder(typeName(target), b -> {
                captureInputs();
                this.type = target;
                push();
            }).bounds(x + TYPE_X + i * TYPE_STEP, y + TYPE_Y, TYPE_W, TYPE_H).build());
        }

        // 三个开关：同一行排开，宽度按文字长度分配（原布局会互相重叠）
        this.quickButton = addRenderableWidget(Button.builder(toggleLabel("gui.cc_rc.draw.quick",
                        this.quick), b -> {
                    captureInputs();
                    this.quick = !this.quick;
                    push();
                    b.setMessage(toggleLabel("gui.cc_rc.draw.quick", this.quick));
                }).bounds(x + QUICK_X, y + TOGGLE_Y, QUICK_W, 16).build());
        this.snapButton = addRenderableWidget(Button.builder(toggleLabel("gui.cc_rc.draw.snap",
                        this.snap), b -> {
                    captureInputs();
                    this.snap = !this.snap;
                    push();
                    b.setMessage(toggleLabel("gui.cc_rc.draw.snap", this.snap));
                }).bounds(x + SNAP_X, y + TOGGLE_Y, SNAP_W, 16).build());
        this.straightButton = addRenderableWidget(Button.builder(
                        toggleLabel("gui.cc_rc.draw.straight", this.straight), b -> {
                    captureInputs();
                    this.straight = !this.straight;
                    push();
                    b.setMessage(toggleLabel("gui.cc_rc.draw.straight", this.straight));
                }).bounds(x + STRAIGHT_X, y + TOGGLE_Y, STRAIGHT_W, 16).build());

        // 颜色（输入框与色块第一行同高，不再压到图样列表上）
        this.hexBox = new EditBox(this.font, x + 146, y + SWATCH_Y + 2, 64, 14,
                Component.translatable("gui.cc_rc.draw.color"));
        this.hexBox.setMaxLength(7);
        this.hexBox.setValue(String.format("#%06X", this.color));
        this.hexBox.setResponder(text -> {
            try {
                String value = text.startsWith("#") ? text.substring(1) : text;
                this.color = Integer.parseInt(value, 16) & 0xFFFFFF;
            } catch (NumberFormatException ignored) {
                // 输入未完成时忽略
            }
        });
        addRenderableWidget(this.hexBox);

        // 线宽 / 缩放
        this.widthBox = new EditBox(this.font, x + WIDTH_X, y + INPUT_Y, 50, 14,
                Component.translatable("gui.cc_rc.draw.width"));
        this.widthBox.setMaxLength(5);
        this.widthBox.setValue(fmt(this.lineWidth));
        addRenderableWidget(this.widthBox);

        this.scaleBox = new EditBox(this.font, x + SCALE_X, y + INPUT_Y, 50, 14,
                Component.translatable("gui.cc_rc.draw.preset_label"));
        this.scaleBox.setMaxLength(5);
        this.scaleBox.setValue(fmt(this.scale));
        addRenderableWidget(this.scaleBox);

        // 图层 0~7
        for (int i = 0; i < ConsoleShape.LAYERS; i++) {
            final int target = i;
            addRenderableWidget(Button.builder(Component.literal(String.valueOf(i)), b -> {
                captureInputs();
                this.layer = target;
                push();
            }).bounds(x + LAYER_X + i * 17, y + INPUT_Y, 15, 14).build());
        }

        // 关闭（与属性界面一样放在底部按钮行）
        addRenderableWidget(Button.builder(Component.translatable("gui.cc_rc.draw.close"),
                        b -> onClose())
                .bounds(x + 8, y + CLOSE_Y, 76, 20).build());
    }

    /** 把界面当前值写回物品 NBT 并同步服务端。 */
    private void push() {
        this.lineWidth = ConsoleShape.quantize(Math.max(ConsoleShape.MIN_WIDTH,
                Math.min(ConsoleShape.MAX_WIDTH, this.lineWidth)));
        this.scale = ConsoleShape.quantize(Math.max(ConsoleShape.MIN_SCALE,
                Math.min(ConsoleShape.MAX_SCALE, this.scale)));
        this.layer = Math.max(0, Math.min(ConsoleShape.LAYERS - 1, this.layer));
        DrawToolItem.Settings settings = new DrawToolItem.Settings(this.color, this.lineWidth,
                this.scale, this.layer, this.type, this.presetId,
                this.quick, this.snap, this.straight);
        DrawToolItem.writeSettings(this.stack, settings);
        ModNetwork.CHANNEL.sendToServer(new DrawToolSettingsPacket(settings));
    }

    /** 用输入框当前内容更新内存值（非法输入保留原值）。 */
    private void captureInputs() {
        this.lineWidth = parse(this.widthBox, this.lineWidth);
        this.scale = parse(this.scaleBox, this.scale);
        // 颜色输入框可能处于半输入状态，合法时以输入框为准
        String hex = this.hexBox.getValue().trim();
        try {
            String value = hex.startsWith("#") ? hex.substring(1) : hex;
            this.color = Integer.parseInt(value, 16) & 0xFFFFFF;
        } catch (NumberFormatException ignored) {
            // 保持原颜色
        }
    }

    @Override
    public void onClose() {
        captureInputs();
        push();
        super.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        drawPanel(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** 面板背景、标题与静态标签（在控件之前绘制）。 */
    private void drawPanel(GuiGraphics graphics) {
        int x = this.leftPos;
        int y = this.topPos;
        graphics.fill(x, y, x + W, y + H, 0xE0101010);
        border(graphics, x, y, x + W, y + H, 0xFF808080);
        graphics.drawString(this.font, this.title, x + 8, y + 6, 0xFFFFFFFF, false);

        graphics.drawString(this.font, Component.translatable("gui.cc_rc.draw.type"),
                x + 8, y + 20, 0xFFFFFFFF, false);
        graphics.drawString(this.font, Component.translatable("gui.cc_rc.draw.color"),
                x + 8, y + 76, 0xFFFFFFFF, false);
        graphics.drawString(this.font, Component.translatable("gui.cc_rc.draw.width"),
                x + WIDTH_X, y + LABEL_Y, 0xFFFFFFFF, false);
        graphics.drawString(this.font, Component.translatable("gui.cc_rc.draw.scale_short"),
                x + SCALE_X, y + LABEL_Y, 0xFFFFFFFF, false);
        graphics.drawString(this.font, Component.translatable("gui.cc_rc.draw.layer"),
                x + LAYER_X, y + LABEL_Y, 0xFFFFFFFF, false);
        // 图样列表面板（与属性界面一致：深色底 + 灰边框 + 标题）
        int panelLeft = x + PRESET_X - 6;
        int panelRight = x + PRESET_X + PRESET_COLS * (PRESET_CELL + PRESET_GAP);
        int panelTop = y + PRESET_AREA_TOP - 16;
        int panelBottom = y + PRESET_AREA_BOTTOM;
        graphics.fill(panelLeft, panelTop, panelRight + 6, panelBottom, 0xFF1E1E1E);
        border(graphics, panelLeft, panelTop, panelRight + 6, panelBottom, 0xFF606060);
        graphics.drawString(this.font, Component.translatable("gui.cc_rc.draw.preset_list"),
                x + PRESET_X, y + 18, 0xFFFFFFFF, false);
        // 类型 / 图层选中高亮
        highlight(graphics, x + TYPE_X + typeIndex() * TYPE_STEP, y + TYPE_Y, TYPE_W, TYPE_H);
        highlight(graphics, x + LAYER_X + this.layer * 17, y + INPUT_Y, 15, 14);

        // 颜色色块
        for (int i = 0; i < DYE_COLORS.length; i++) {
            int sx = x + swatchX(i);
            int sy = y + swatchY(i);
            graphics.fill(sx, sy, sx + SWATCH_W, sy + SWATCH_H, 0xFF000000 | DYE_COLORS[i]);
            if ((this.color & 0xFFFFFF) == DYE_COLORS[i]) {
                highlight(graphics, sx, sy, SWATCH_W, SWATCH_H);
            }
        }

        // 贴图样式缩略图：超出可视区域的部分裁掉，滚轮翻看（见 mouseScrolled）
        List<ConsoleDrawPresets.Preset> all = ConsoleDrawPresets.all();
        graphics.enableScissor(x + PRESET_X - 4, y + PRESET_AREA_TOP,
                x + PRESET_X + PRESET_COLS * (PRESET_CELL + PRESET_GAP), y + PRESET_AREA_BOTTOM);
        for (int i = 0; i < all.size(); i++) {
            int row = i / PRESET_COLS - this.presetScroll;
            if (row < 0 || row >= PRESET_VISIBLE_ROWS) {
                continue;
            }
            ConsoleDrawPresets.Preset preset = all.get(i);
            int cx = x + presetCellX(i);
            int cy = y + PRESET_Y + row * (PRESET_CELL + PRESET_GAP);
            graphics.fill(cx, cy, cx + PRESET_CELL, cy + PRESET_CELL, 0xFF2A2A2A);
            int inner = PRESET_CELL - 4;
            graphics.blit(preset.texture(), cx + 2, cy + 2, inner, inner,
                    0.0F, 0.0F, preset.size(), preset.size(),
                    preset.size(), preset.size());
            boolean selected = this.type == ConsoleShapeType.PRESET
                    && this.presetId.equals(preset.id());
            border(graphics, cx, cy, cx + PRESET_CELL, cy + PRESET_CELL,
                    selected ? 0xFFFFFFFF : 0xFF606060);
        }
        graphics.disableScissor();
        // 可滚动时在列表右侧画一条滚动条指示
        int maxScroll = presetMaxScroll();
        if (maxScroll > 0) {
            int barX = x + PRESET_X + PRESET_COLS * (PRESET_CELL + PRESET_GAP) - 2;
            int trackTop = y + PRESET_AREA_TOP;
            int trackBottom = y + PRESET_AREA_BOTTOM;
            int trackHeight = trackBottom - trackTop;
            int rows = presetRowCount();
            int thumbHeight = Math.max(8, trackHeight * PRESET_VISIBLE_ROWS / rows);
            int thumbTop = trackTop + (trackHeight - thumbHeight) * this.presetScroll / maxScroll;
            graphics.fill(barX, trackTop, barX + 2, trackBottom, 0xFF303030);
            graphics.fill(barX, thumbTop, barX + 2, thumbTop + thumbHeight, 0xFFA0A0A0);
        }
    }

    /** 贴图样式总行数（向上取整）。 */
    private static int presetRowCount() {
        int count = ConsoleDrawPresets.all().size();
        return (count + PRESET_COLS - 1) / PRESET_COLS;
    }

    /** 贴图样式列表的最大滚动行偏移。 */
    private static int presetMaxScroll() {
        return Math.max(0, presetRowCount() - PRESET_VISIBLE_ROWS);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int areaLeft = this.leftPos + PRESET_X - 4;
        int areaRight = this.leftPos + PRESET_X + PRESET_COLS * (PRESET_CELL + PRESET_GAP);
        int areaTop = this.topPos + PRESET_AREA_TOP;
        int areaBottom = this.topPos + PRESET_AREA_BOTTOM;
        if (mouseX >= areaLeft && mouseX < areaRight
                && mouseY >= areaTop && mouseY < areaBottom
                && presetMaxScroll() > 0) {
            int next = this.presetScroll - (int) Math.signum(delta);
            this.presetScroll = Math.max(0, Math.min(presetMaxScroll(), next));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // 贴图样式
            List<ConsoleDrawPresets.Preset> all = ConsoleDrawPresets.all();
            for (int i = 0; i < all.size(); i++) {
                // 只命中当前滚动位置下可见的格子
                int row = i / PRESET_COLS - this.presetScroll;
                if (row < 0 || row >= PRESET_VISIBLE_ROWS) {
                    continue;
                }
                int cx = this.leftPos + presetCellX(i);
                int cy = this.topPos + PRESET_Y + row * (PRESET_CELL + PRESET_GAP);
                if (mouseX >= cx && mouseX < cx + PRESET_CELL
                        && mouseY >= cy && mouseY < cy + PRESET_CELL) {
                    captureInputs();
                    this.presetId = all.get(i).id();
                    this.type = ConsoleShapeType.PRESET;
                    push();
                    return true;
                }
            }
            // 颜色色块
            for (int i = 0; i < DYE_COLORS.length; i++) {
                int sx = this.leftPos + swatchX(i);
                int sy = this.topPos + swatchY(i);
                if (mouseX >= sx && mouseX < sx + SWATCH_W
                        && mouseY >= sy && mouseY < sy + SWATCH_H) {
                    captureInputs();
                    this.color = DYE_COLORS[i];
                    this.hexBox.setValue(String.format("#%06X", this.color));
                    push();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int typeIndex() {
        for (int i = 0; i < TYPES.length; i++) {
            if (TYPES[i] == this.type) {
                return i;
            }
        }
        return 0;
    }

    private static Component typeName(ConsoleShapeType type) {
        return Component.translatable("gui.cc_rc.draw.tool."
                + type.name().toLowerCase(Locale.ROOT));
    }

    private static Component toggleLabel(String key, boolean on) {
        return Component.translatable(key).append(": ").append(Component.translatable(
                on ? "gui.cc_rc.draw.on" : "gui.cc_rc.draw.off"));
    }

    private static int swatchX(int index) {
        return SWATCH_X + (index % SWATCH_COLS) * (SWATCH_W + SWATCH_GAP);
    }

    private static int swatchY(int index) {
        return SWATCH_Y + (index / SWATCH_COLS) * (SWATCH_H + SWATCH_GAP);
    }

    private static int presetCellX(int index) {
        return PRESET_X + (index % PRESET_COLS) * (PRESET_CELL + PRESET_GAP);
    }

    private static void highlight(GuiGraphics graphics, int x, int y, int w, int h) {
        border(graphics, x - 1, y - 1, x + w + 1, y + h + 1, 0xFFFFFFFF);
    }

    private static void border(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        graphics.fill(x0, y0, x1, y0 + 1, color);
        graphics.fill(x0, y1 - 1, x1, y1, color);
        graphics.fill(x0, y0, x0 + 1, y1, color);
        graphics.fill(x1 - 1, y0, x1, y1, color);
    }

    /** 数值格式化：整数不带小数点，其余保留 1 位小数。 */
    private static String fmt(float value) {
        if (Math.abs(value - Math.round(value)) < 0.05F) {
            return String.valueOf(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static float parse(EditBox box, float fallback) {
        if (box == null) {
            return fallback;
        }
        try {
            return Float.parseFloat(box.getValue().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
