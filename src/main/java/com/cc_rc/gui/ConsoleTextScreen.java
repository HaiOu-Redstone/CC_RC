package com.cc_rc.gui;

import com.cc_rc.CcRc;
import com.cc_rc.block.console.ConsoleBusBlockEntity;
import com.cc_rc.network.ConsoleTextPacket;
import com.cc_rc.network.ModNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;

/**
 * 控制台结构表面文字编辑界面（专用 GUI）。
 *
 * 上半部分沿用普通文字编辑界面的风格（编辑文字 / 颜色 / 字体）：
 *  - 文字输入框、完成按钮、B/I/U/S 格式按钮、16 个染料色块；
 * 下半部分新增：
 *  - 锚点坐标 X / Y（单位：结构像素，1 格 = 16 像素，可负值，点击表面中心为 0）；
 *  - 缩放倍率（默认值来自配置项 console_text.default_scale，默认 0.15）；
 *  - 9 种锚点组合：垂直 上/中/下 + 水平 左/中/右，表示 (X,Y) 对齐文字框的哪个角/边/中心。
 *
 * 提交时把全部参数经 ConsoleTextPacket 写回总线方块实体；坐标为负或越界会被夹在表面范围内。
 */
public class ConsoleTextScreen extends AbstractContainerScreen<ConsoleTextMenu> {

    private static final ResourceLocation GUI_BG =
            new ResourceLocation(CcRc.MODID, "textures/gui/console_text_gui.png");

    /**
     * 面板尺寸与专属背景贴图一致（192x234）。
     * 贴图按原编辑工具界面的样式重新绘制：1 像素黑色圆角描边、
     * 上/左 2 像素白色高光、下/右 2 像素深灰阴影、内部淡灰纯色（198,198,198）。
     */
    private static final int BG_WIDTH = 192;
    private static final int BG_HEIGHT = 234;

    // 上半部分（与普通文字编辑界面一致）
    private static final int INPUT_X = 12;
    private static final int INPUT_Y = 34;
    private static final int INPUT_W = 152;
    private static final int INPUT_H = 16;
    private static final int DONE_X = 12;
    private static final int DONE_Y = 68;
    private static final int DONE_W = 152;
    private static final int DONE_H = 20;
    private static final int FORMAT_Y = 96;
    private static final int FORMAT_W = 34;
    private static final int FORMAT_H = 16;
    private static final int FORMAT_GAP = 6;
    private static final int COLOR_Y = 118;
    private static final int COLOR_W = 16;
    private static final int COLOR_H = 10;
    private static final int COLOR_GAP_X = 2;
    private static final int COLOR_GAP_Y = 2;
    private static final int COLOR_COLS = 8;

    // 下半部分（新增：位置 / 锚点 / 缩放）
    private static final int POS_Y = 170;
    private static final int POS_H = 16;
    // 三个输入框之间留出更大间距，方便区分与阅读（每组：标签 + 输入框）
    private static final int X_LABEL_X = 8;
    private static final int X_BOX_X = 17;
    private static final int X_BOX_W = 42;
    private static final int Y_LABEL_X = 71;
    private static final int Y_BOX_X = 80;
    private static final int Y_BOX_W = 42;
    private static final int S_LABEL_X = 134;
    private static final int S_BOX_X = 156;
    private static final int S_BOX_W = 30;
    private static final int ANCHOR_V_Y = 190;
    private static final int ANCHOR_H_Y = 208;
    private static final int ANCHOR_BTN_X = 32;
    private static final int ANCHOR_BTN_W = 42;
    private static final int ANCHOR_BTN_H = 16;
    private static final int ANCHOR_GAP = 2;

    private static final String[] V_TAGS = {"上", "中", "下"};
    private static final String[] H_TAGS = {"左", "中", "右"};

    // 文字样式状态
    private int color;
    private boolean bold;
    private boolean italic;
    private boolean underline;
    private boolean strikethrough;
    private int anchorH;
    private int anchorV;

    private EditBox input;
    private EditBox uBox;
    private EditBox vBox;
    private EditBox scaleBox;
    private final List<Button> anchorVButtons = new ArrayList<>();
    private final List<Button> anchorHButtons = new ArrayList<>();

    public ConsoleTextScreen(ConsoleTextMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.translatable("item.cc_rc.edit_tool"));
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.leftPos;
        int cy = this.topPos;

        // 初值来自菜单（已有文字带出原内容/样式/锚点/缩放）
        this.color = menu.color;
        this.bold = menu.bold;
        this.italic = menu.italic;
        this.underline = menu.underline;
        this.strikethrough = menu.strikethrough;
        this.anchorH = Math.max(0, Math.min(2, menu.anchorH));
        this.anchorV = Math.max(0, Math.min(2, menu.anchorV));

        // 文字输入框
        this.input = new EditBox(this.font, cx + INPUT_X, cy + INPUT_Y, INPUT_W, INPUT_H,
                Component.literal("输入显示文字"));
        this.input.setMaxLength(32);
        this.input.setValue(menu.currentText.getString());
        this.addWidget(this.input);
        this.setInitialFocus(this.input);

        // 完成按钮
        Button done = Button.builder(Component.literal("完成"), b -> this.submitAndClose())
                .bounds(cx + DONE_X, cy + DONE_Y, DONE_W, DONE_H)
                .build();
        this.addRenderableWidget(done);

        // 格式按钮 B / I / U / S
        addFormatButton(cx + INPUT_X + 0 * (FORMAT_W + FORMAT_GAP), cy + FORMAT_Y,
                "B", "粗体", () -> this.bold, v -> this.bold = v);
        addFormatButton(cx + INPUT_X + 1 * (FORMAT_W + FORMAT_GAP), cy + FORMAT_Y,
                "I", "斜体", () -> this.italic, v -> this.italic = v);
        addFormatButton(cx + INPUT_X + 2 * (FORMAT_W + FORMAT_GAP), cy + FORMAT_Y,
                "U", "下划线", () -> this.underline, v -> this.underline = v);
        addFormatButton(cx + INPUT_X + 3 * (FORMAT_W + FORMAT_GAP), cy + FORMAT_Y,
                "S", "删除线", () -> this.strikethrough, v -> this.strikethrough = v);

        // 染料色块
        DyeColor[] dyes = DyeColor.values();
        for (int i = 0; i < dyes.length; i++) {
            int rgb = dyes[i].getTextColor();
            int x = cx + INPUT_X + (i % COLOR_COLS) * (COLOR_W + COLOR_GAP_X);
            int y = cy + COLOR_Y + (i / COLOR_COLS) * (COLOR_H + COLOR_GAP_Y);
            ColorSwatch swatch = new ColorSwatch(x, y, rgb, Component.translatable(
                    "color.minecraft." + dyes[i].getName()));
            swatch.setTooltip(Tooltip.create(Component.translatable(
                    "color.minecraft." + dyes[i].getName())));
            this.addRenderableWidget(swatch);
        }

        // 锚点坐标 X / Y 与缩放
        this.uBox = new EditBox(this.font, cx + X_BOX_X, cy + POS_Y, X_BOX_W, POS_H,
                Component.literal("X"));
        this.uBox.setMaxLength(8);
        this.uBox.setValue(formatFloat(menu.u));
        this.uBox.setTooltip(Tooltip.create(Component.literal("锚点 X（像素，向右为正）")));
        this.addWidget(this.uBox);

        this.vBox = new EditBox(this.font, cx + Y_BOX_X, cy + POS_Y, Y_BOX_W, POS_H,
                Component.literal("Y"));
        this.vBox.setMaxLength(8);
        this.vBox.setValue(formatFloat(menu.v));
        this.vBox.setTooltip(Tooltip.create(Component.literal("锚点 Y（像素，向上为正）")));
        this.addWidget(this.vBox);

        this.scaleBox = new EditBox(this.font, cx + S_BOX_X, cy + POS_Y, S_BOX_W, POS_H,
                Component.literal("缩放"));
        this.scaleBox.setMaxLength(6);
        this.scaleBox.setValue(formatFloat(menu.scale));
        this.scaleBox.setTooltip(Tooltip.create(Component.literal("缩放倍率（默认 "
                + formatFloat(com.cc_rc.Config.getConsoleTextDefaultScale()) + "）")));
        this.addWidget(this.scaleBox);

        // 垂直锚点：上 / 中 / 下
        for (int i = 0; i < 3; i++) {
            final int value = i;
            Button btn = Button.builder(Component.empty(), b -> {
                        this.anchorV = value;
                        refreshAnchorButtons();
                    })
                    .bounds(cx + ANCHOR_BTN_X + i * (ANCHOR_BTN_W + ANCHOR_GAP), cy + ANCHOR_V_Y,
                            ANCHOR_BTN_W, ANCHOR_BTN_H)
                    .build();
            btn.setTooltip(Tooltip.create(Component.literal("垂直锚点：" + V_TAGS[i])));
            this.anchorVButtons.add(btn);
            this.addRenderableWidget(btn);
        }
        // 水平锚点：左 / 中 / 右
        for (int i = 0; i < 3; i++) {
            final int value = i;
            Button btn = Button.builder(Component.empty(), b -> {
                        this.anchorH = value;
                        refreshAnchorButtons();
                    })
                    .bounds(cx + ANCHOR_BTN_X + i * (ANCHOR_BTN_W + ANCHOR_GAP), cy + ANCHOR_H_Y,
                            ANCHOR_BTN_W, ANCHOR_BTN_H)
                    .build();
            btn.setTooltip(Tooltip.create(Component.literal("水平锚点：" + H_TAGS[i])));
            this.anchorHButtons.add(btn);
            this.addRenderableWidget(btn);
        }
        refreshAnchorButtons();
    }

    /** 创建格式切换按钮：激活 = 粗体。 */
    private void addFormatButton(int x, int y, String tag, String name,
                                 java.util.function.BooleanSupplier getter,
                                 java.util.function.Consumer<Boolean> setter) {
        Button btn = Button.builder(Component.empty(), b -> {
                    setter.accept(!getter.getAsBoolean());
                    refreshFormatButton(b, tag, getter.getAsBoolean());
                })
                .bounds(x, y, FORMAT_W, FORMAT_H)
                .build();
        btn.setTooltip(Tooltip.create(Component.literal(name)));
        refreshFormatButton(btn, tag, getter.getAsBoolean());
        this.addRenderableWidget(btn);
    }

    private void refreshFormatButton(Button btn, String tag, boolean active) {
        btn.setMessage(Component.literal(tag).withStyle(
                Style.EMPTY.withBold(active).withColor(0xFFFFFF)));
    }

    /** 刷新 6 个锚点按钮：当前选中项加粗。 */
    private void refreshAnchorButtons() {
        for (int i = 0; i < this.anchorVButtons.size(); i++) {
            this.anchorVButtons.get(i).setMessage(Component.literal(V_TAGS[i])
                    .withStyle(Style.EMPTY.withBold(this.anchorV == i)));
        }
        for (int i = 0; i < this.anchorHButtons.size(); i++) {
            this.anchorHButtons.get(i).setMessage(Component.literal(H_TAGS[i])
                    .withStyle(Style.EMPTY.withBold(this.anchorH == i)));
        }
    }

    /** 数字格式化：去掉多余的小数位（整数不带小数点）。 */
    private static String formatFloat(float value) {
        if (Math.abs(value - Math.round(value)) < 0.0001F) {
            return Integer.toString(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    /** 解析输入框（非法输入回退默认值）。 */
    private static float parseFloat(EditBox box, float fallback) {
        try {
            return Float.parseFloat(box.getValue().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** 提交并关闭：坐标夹在表面范围内，缩放夹在允许区间后发给服务端。 */
    private void submitAndClose() {
        float u = parseFloat(this.uBox, menu.u);
        float v = parseFloat(this.vBox, menu.v);
        float scale = parseFloat(this.scaleBox, com.cc_rc.Config.getConsoleTextDefaultScale());

        u = Math.max(-menu.halfW, Math.min(menu.halfW, u));
        v = Math.max(-menu.halfH, Math.min(menu.halfH, v));
        scale = Math.max(ConsoleBusBlockEntity.TextEntry.MIN_SCALE,
                Math.min(ConsoleBusBlockEntity.TextEntry.MAX_SCALE, scale));

        ModNetwork.CHANNEL.sendToServer(new ConsoleTextPacket(
                menu.pos, menu.textId, menu.faceOrd, u, v, this.input.getValue(),
                this.color, this.bold, this.italic, this.underline, this.strikethrough,
                this.anchorH, this.anchorV, scale));
        this.onClose();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        // 专属背景贴图已按完整面板尺寸绘制，直接整张铺上即可（无需再拼贴纯色扩展区）
        // 必须用带 textureWidth/textureHeight 的 11 参数重载；7 参数重载按 256x256 计算 UV，
        // 会把贴图右侧与底部裁掉（表现为右边框、下边框消失）
        graphics.blit(GUI_BG, this.leftPos, this.topPos, BG_WIDTH, BG_HEIGHT,
                0.0F, 0.0F, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        String current = menu.currentText.getString();
        if (!current.isEmpty()) {
            graphics.drawString(this.font, "当前：" + current,
                    this.titleLabelX + this.font.width(this.title) + 10,
                    this.titleLabelY, 0x404040, false);
        }

        // 新增区域的小标签（坐标相对 GUI 左上角）
        int labelColor = 0x404040;
        graphics.drawString(this.font, "X", X_LABEL_X, POS_Y + 4, labelColor, false);
        graphics.drawString(this.font, "Y", Y_LABEL_X, POS_Y + 4, labelColor, false);
        graphics.drawString(this.font, "缩放", S_LABEL_X, POS_Y + 4, labelColor, false);
        graphics.drawString(this.font, "垂直", X_LABEL_X, ANCHOR_V_Y + 4, labelColor, false);
        graphics.drawString(this.font, "水平", X_LABEL_X, ANCHOR_H_Y + 4, labelColor, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        // EditBox 不会被 AbstractContainerScreen 自动渲染
        this.input.render(graphics, mouseX, mouseY, partialTick);
        this.uBox.render(graphics, mouseX, mouseY, partialTick);
        this.vBox.render(graphics, mouseX, mouseY, partialTick);
        this.scaleBox.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean enter = keyCode == 257 || keyCode == 335;
        if (enter && (this.input.isFocused() || this.uBox.isFocused()
                || this.vBox.isFocused() || this.scaleBox.isFocused())) {
            this.submitAndClose();
            return true;
        }
        // 拦截背包键：编辑界面不应被背包键关闭
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** 染料色块按钮：自绘纯色方块 + 边框，白色边框 = 当前选中颜色。 */
    private class ColorSwatch extends Button {

        private final int rgb;

        ColorSwatch(int x, int y, int rgb, Component message) {
            super(x, y, COLOR_W, COLOR_H, message, b -> color = rgb, DEFAULT_NARRATION);
            this.rgb = rgb;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int argb = 0xFF000000 | this.rgb;
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width,
                    this.getY() + this.height, argb);
            int border = color == this.rgb ? 0xFFFFFFFF : 0xFF303030;
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width,
                    this.getY() + 1, border);
            graphics.fill(this.getX(), this.getY() + this.height - 1,
                    this.getX() + this.width, this.getY() + this.height, border);
            graphics.fill(this.getX(), this.getY(), this.getX() + 1,
                    this.getY() + this.height, border);
            graphics.fill(this.getX() + this.width - 1, this.getY(),
                    this.getX() + this.width, this.getY() + this.height, border);
        }
    }
}
