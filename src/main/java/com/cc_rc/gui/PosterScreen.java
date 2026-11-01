package com.cc_rc.gui;

import com.cc_rc.block.poster.PosterImages;
import com.cc_rc.network.ModNetwork;
import com.cc_rc.network.PosterConfigPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 海报设置界面（自绘深色面板，不使用背景贴图）。
 *
 * 布局（界面 212 x 190）：
 *  - 顶部：标题「海报设置」，右侧提示「单位：格（可输入 0.5）」；
 *  - 中部：7 列 x 4 行缩略图（全部可选海报图片按原始比例等比缩放显示，索引 0 = 无材质），
 *    点击选择，鼠标悬停显示原始图片名称；选中项绘制白色边框；
 *  - 下部：三个输入框「缩放倍率」「垂直偏移 dy」「水平偏移 dz」+「应用」按钮。
 *
 * 说明：
 *  - 「应用」通过 PosterConfigPacket 写回方块实体，界面保持打开，便于连续微调；
 *  - 缩放倍率 1.0 = 图片宽 1 格；dy 向上为正，dz 观者视角向右为正（单位：格，可输入 0.5）。
 */
public class PosterScreen extends AbstractContainerScreen<PosterMenu> {

    private static final int BG_WIDTH = 212;
    private static final int BG_HEIGHT = 190;

    // 缩略图网格
    private static final int GRID_X = 10;
    private static final int GRID_Y = 20;
    private static final int COLS = 7;
    private static final int CELL = 27;
    private static final int THUMB = 25;

    // 输入框行
    private static final int LABEL_Y = 132;
    private static final int INPUT_Y = 142;
    private static final int INPUT_W = 60;
    private static final int INPUT_H = 16;
    private static final int INPUT_X0 = 10;
    private static final int INPUT_X1 = 76;
    private static final int INPUT_X2 = 142;

    // 应用按钮
    private static final int APPLY_X = 71;
    private static final int APPLY_Y = 164;
    private static final int APPLY_W = 70;
    private static final int APPLY_H = 18;

    /** 当前选中的图片索引（点击缩略图改变，点「应用」时提交）。 */
    private int selected;

    private EditBox scaleBox;
    private EditBox dyBox;
    private EditBox dzBox;

    public PosterScreen(PosterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.translatable("gui.cc_rc.poster.title"));
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
        this.selected = menu.image;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.leftPos;
        int cy = this.topPos;

        // 缩略图按钮：全部图片按 7 列排布
        for (int i = 0; i < PosterImages.size(); i++) {
            int x = cx + GRID_X + (i % COLS) * CELL + (CELL - THUMB) / 2;
            int y = cy + GRID_Y + (i / COLS) * CELL + (CELL - THUMB) / 2;
            ImageButton button = new ImageButton(x, y, i);
            // 提示文字 = 原始图片名称（「无材质」为索引 0 的默认材质）
            button.setTooltip(Tooltip.create(Component.literal(PosterImages.get(i).name())));
            this.addRenderableWidget(button);
        }

        // 三个数值输入框（初值取方块实体当前配置）
        this.scaleBox = makeBox(cx + INPUT_X0, cy + INPUT_Y, format(menu.scale));
        this.dyBox = makeBox(cx + INPUT_X1, cy + INPUT_Y, format(menu.dy));
        this.dzBox = makeBox(cx + INPUT_X2, cy + INPUT_Y, format(menu.dz));
        this.setInitialFocus(this.scaleBox);

        // 应用按钮
        Button apply = Button.builder(Component.translatable("gui.cc_rc.poster.apply"), b -> this.apply())
                .bounds(cx + APPLY_X, cy + APPLY_Y, APPLY_W, APPLY_H)
                .build();
        this.addRenderableWidget(apply);
    }

    private EditBox makeBox(int x, int y, String value) {
        EditBox box = new EditBox(this.font, x, y, INPUT_W, INPUT_H, Component.empty());
        box.setMaxLength(10);
        box.setValue(value);
        this.addWidget(box);
        return box;
    }

    /** 浮点数格式化：去掉多余的 .0，便于阅读与再次编辑。 */
    private static String format(float value) {
        if (Math.abs(value - Math.round(value)) < 1.0E-4F) {
            return String.valueOf(Math.round(value));
        }
        return String.valueOf(value);
    }

    /** 解析输入框数值（允许逗号小数点；非法输入回退为原值）。 */
    private static float parse(String text, float fallback) {
        try {
            return Float.parseFloat(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void apply() {
        ModNetwork.CHANNEL.sendToServer(new PosterConfigPacket(
                menu.pos, this.selected,
                parse(this.scaleBox.getValue(), menu.scale),
                parse(this.dyBox.getValue(), menu.dy),
                parse(this.dzBox.getValue(), menu.dz)));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // 自绘深色面板 + 浅灰边框
        int x0 = this.leftPos;
        int y0 = this.topPos;
        int x1 = x0 + BG_WIDTH;
        int y1 = y0 + BG_HEIGHT;
        graphics.fill(x0, y0, x1, y1, 0xFF1E1E1E);
        graphics.fill(x0, y0, x1, y0 + 1, 0xFF808080);
        graphics.fill(x0, y1 - 1, x1, y1, 0xFF808080);
        graphics.fill(x0, y0, x0 + 1, y1, 0xFF808080);
        graphics.fill(x1 - 1, y0, x1, y1, 0xFF808080);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFF, false);
        Component hint = Component.translatable("gui.cc_rc.poster.hint");
        graphics.drawString(this.font, hint, BG_WIDTH - 8 - this.font.width(hint),
                this.titleLabelY, 0x9A9A9A, false);

        graphics.drawString(this.font, Component.translatable("gui.cc_rc.poster.scale"),
                INPUT_X0, LABEL_Y, 0xE0E0E0, false);
        graphics.drawString(this.font, Component.translatable("gui.cc_rc.poster.dy"),
                INPUT_X1, LABEL_Y, 0xE0E0E0, false);
        graphics.drawString(this.font, Component.translatable("gui.cc_rc.poster.dz"),
                INPUT_X2, LABEL_Y, 0xE0E0E0, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        // 输入框需要手动渲染（AbstractContainerScreen 不会自动渲染 addWidget 的 EditBox）
        this.scaleBox.render(graphics, mouseX, mouseY, partialTick);
        this.dyBox.render(graphics, mouseX, mouseY, partialTick);
        this.dzBox.render(graphics, mouseX, mouseY, partialTick);
    }

    /** 图片缩略图按钮：等比缩放绘制原图，选中项白色边框。 */
    private class ImageButton extends Button {

        private final int index;

        ImageButton(int x, int y, int index) {
            super(x, y, THUMB, THUMB, Component.empty(), b -> selected = index, DEFAULT_NARRATION);
            this.index = index;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // 底部格背板（深灰）
            graphics.fill(getX(), getY(), getX() + this.width, getY() + this.height, 0xFF101010);

            // 缩略图：按内容比例等比缩放至按钮内部（跳过画布顶部透明留白）
            PosterImages.Image image = PosterImages.get(this.index);
            float factor = Math.min((float) (this.width - 2) / image.width(),
                    (float) (this.height - 2) / image.contentHeight());
            int tw = Math.max(1, (int) (image.width() * factor));
            int th = Math.max(1, (int) (image.contentHeight() * factor));
            int ox = getX() + (this.width - tw) / 2;
            int oy = getY() + (this.height - th) / 2;
            // 11 参重载：采样内容区域 (0, top, 原宽, 内容高) 后拉伸绘制到 tw x th
            // （9 参重载不支持缩放，只会截取左上角同尺寸区域，导致缩略图几乎空白）
            graphics.blit(image.texture(), ox, oy, tw, th, 0.0F, (float) image.top(),
                    image.width(), image.contentHeight(), image.width(), image.height());

            // 边框：选中 = 白色；悬停 = 浅灰；普通 = 深灰
            int border = selected == this.index ? 0xFFFFFFFF
                    : (isHoveredOrFocused() ? 0xFFD0D0D0 : 0xFF555555);
            graphics.fill(getX(), getY(), getX() + this.width, getY() + 1, border);
            graphics.fill(getX(), getY() + this.height - 1, getX() + this.width, getY() + this.height, border);
            graphics.fill(getX(), getY(), getX() + 1, getY() + this.height, border);
            graphics.fill(getX() + this.width - 1, getY(), getX() + this.width, getY() + this.height, border);
        }
    }
}
