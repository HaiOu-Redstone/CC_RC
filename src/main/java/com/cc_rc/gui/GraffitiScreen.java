package com.cc_rc.gui;

import com.cc_rc.CcRc;
import com.cc_rc.block.graffiti.GraffitiBlockEntity;
import com.cc_rc.network.GraffitiConfigPacket;
import com.cc_rc.network.ModNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Locale;
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
 * 涂鸦文字编辑界面（专用 GUI）。
 *
 * 复用编辑工具文字界面的风格（1 像素描边面板 + 淡灰底）：
 *  - 文字输入框（单行，最长 {@link GraffitiBlockEntity#MAX_TEXT} 字符）；
 *  - 完成按钮；
 *  - B / I / U / S 字体格式按钮；
 *  - 16 个染料色块（颜色）；
 *  - dx / dy 偏移（单位格，向右 / 向上为正）；
 *  - 大小（行高，单位格，0.5 = 半格高）。
 *
 * 「完成」经 GraffitiConfigPacket 把全部参数写回方块实体。
 */
public class GraffitiScreen extends AbstractContainerScreen<GraffitiMenu> {

    /** 背景沿用控制台文字编辑界面的面板贴图（192x234）。 */
    private static final ResourceLocation GUI_BG =
            new ResourceLocation(CcRc.MODID, "textures/gui/console_text_gui.png");

    private static final int BG_WIDTH = 192;
    private static final int BG_HEIGHT = 234;

    // 文字输入 / 完成
    private static final int INPUT_X = 12;
    private static final int INPUT_Y = 32;
    private static final int INPUT_W = 168;
    private static final int INPUT_H = 16;
    private static final int DONE_Y = 54;
    private static final int DONE_H = 20;

    // 字体格式 B / I / U / S
    private static final int FORMAT_Y = 82;
    private static final int FORMAT_W = 34;
    private static final int FORMAT_H = 16;
    private static final int FORMAT_GAP = 6;

    // 染料色块
    private static final int COLOR_Y = 104;
    private static final int COLOR_W = 16;
    private static final int COLOR_H = 10;
    private static final int COLOR_GAP_X = 2;
    private static final int COLOR_GAP_Y = 2;
    private static final int COLOR_COLS = 8;

    // dx / dy 偏移
    private static final int POS_Y = 136;
    private static final int POS_H = 16;
    private static final int DX_BOX_X = 26;
    private static final int DX_BOX_W = 58;
    private static final int DY_LABEL_X = 94;
    private static final int DY_BOX_X = 110;
    private static final int DY_BOX_W = 58;

    // 大小
    private static final int SCALE_Y = 158;
    private static final int SCALE_BOX_X = 50;
    private static final int SCALE_BOX_W = 58;

    // 文字样式状态
    private int color;
    private boolean bold;
    private boolean italic;
    private boolean underline;
    private boolean strikethrough;

    private EditBox input;
    private EditBox dxBox;
    private EditBox dyBox;
    private EditBox scaleBox;

    public GraffitiScreen(GraffitiMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.translatable("block.cc_rc.graffiti"));
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.leftPos;
        int cy = this.topPos;

        // 初值来自菜单（已有涂鸦带出原内容 / 颜色 / 大小 / 偏移 / 格式）
        this.color = menu.color & 0xFFFFFF;
        this.bold = menu.bold;
        this.italic = menu.italic;
        this.underline = menu.underline;
        this.strikethrough = menu.strikethrough;

        // 文字输入框
        this.input = new EditBox(this.font, cx + INPUT_X, cy + INPUT_Y, INPUT_W, INPUT_H,
                Component.literal("输入涂鸦文字"));
        this.input.setMaxLength(GraffitiBlockEntity.MAX_TEXT);
        this.input.setValue(menu.text);
        this.addWidget(this.input);
        this.setInitialFocus(this.input);

        // 完成按钮
        Button done = Button.builder(Component.literal("完成"), b -> this.submitAndClose())
                .bounds(cx + INPUT_X, cy + DONE_Y, INPUT_W, DONE_H)
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

        // dx / dy 偏移与大小
        this.dxBox = new EditBox(this.font, cx + DX_BOX_X, cy + POS_Y, DX_BOX_W, POS_H,
                Component.literal("dx"));
        this.dxBox.setMaxLength(8);
        this.dxBox.setValue(formatFloat(menu.dx));
        this.dxBox.setTooltip(Tooltip.create(Component.literal("水平偏移（格，向右为正）")));
        this.addWidget(this.dxBox);

        this.dyBox = new EditBox(this.font, cx + DY_BOX_X, cy + POS_Y, DY_BOX_W, POS_H,
                Component.literal("dy"));
        this.dyBox.setMaxLength(8);
        this.dyBox.setValue(formatFloat(menu.dy));
        this.dyBox.setTooltip(Tooltip.create(Component.literal("垂直偏移（格，向上为正）")));
        this.addWidget(this.dyBox);

        this.scaleBox = new EditBox(this.font, cx + SCALE_BOX_X, cy + SCALE_Y, SCALE_BOX_W, POS_H,
                Component.literal("大小"));
        this.scaleBox.setMaxLength(6);
        this.scaleBox.setValue(formatFloat(menu.scale));
        this.scaleBox.setTooltip(Tooltip.create(Component.literal(
                "文字大小：一行文字的高度（格），0.5 = 半格高")));
        this.addWidget(this.scaleBox);
    }

    /** 创建格式切换按钮：激活时加粗显示。 */
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

    /** 数字格式化：去掉多余的小数位（整数不带小数点）。 */
    private static String formatFloat(float value) {
        if (Math.abs(value - Math.round(value)) < 0.0001F) {
            return Integer.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    /** 解析输入框（非法输入回退默认值）。 */
    private static float parseFloat(EditBox box, float fallback) {
        try {
            return Float.parseFloat(box.getValue().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** 提交并关闭：数值夹在允许区间后发给服务端。 */
    private void submitAndClose() {
        float dx = parseFloat(this.dxBox, menu.dx);
        float dy = parseFloat(this.dyBox, menu.dy);
        float scale = parseFloat(this.scaleBox, GraffitiBlockEntity.DEFAULT_SCALE);

        dx = Math.max(-GraffitiBlockEntity.MAX_OFFSET,
                Math.min(GraffitiBlockEntity.MAX_OFFSET, dx));
        dy = Math.max(-GraffitiBlockEntity.MAX_OFFSET,
                Math.min(GraffitiBlockEntity.MAX_OFFSET, dy));
        scale = Math.max(GraffitiBlockEntity.MIN_SCALE,
                Math.min(GraffitiBlockEntity.MAX_SCALE, scale));

        ModNetwork.CHANNEL.sendToServer(new GraffitiConfigPacket(
                menu.pos, this.input.getValue(), this.color, scale, dx, dy,
                GraffitiMenu.flags(this.bold, this.italic, this.underline, this.strikethrough)));
        this.onClose();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        // 11 参数重载：显式给出贴图尺寸，避免按 256x256 计算 UV 裁掉边框
        graphics.blit(GUI_BG, this.leftPos, this.topPos, BG_WIDTH, BG_HEIGHT,
                0.0F, 0.0F, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        int labelColor = 0x404040;
        graphics.drawString(this.font, "dx", INPUT_X - 2, POS_Y + 4, labelColor, false);
        graphics.drawString(this.font, "dy", DY_LABEL_X, POS_Y + 4, labelColor, false);
        graphics.drawString(this.font, "大小", INPUT_X - 2, SCALE_Y + 4, labelColor, false);
        graphics.drawString(this.font, "行高（格）", SCALE_BOX_X + SCALE_BOX_W + 4,
                SCALE_Y + 4, 0x707070, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        // EditBox 不会被 AbstractContainerScreen 自动渲染
        this.input.render(graphics, mouseX, mouseY, partialTick);
        this.dxBox.render(graphics, mouseX, mouseY, partialTick);
        this.dyBox.render(graphics, mouseX, mouseY, partialTick);
        this.scaleBox.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean enter = keyCode == 257 || keyCode == 335;
        if (enter && (this.input.isFocused() || this.dxBox.isFocused()
                || this.dyBox.isFocused() || this.scaleBox.isFocused())) {
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
