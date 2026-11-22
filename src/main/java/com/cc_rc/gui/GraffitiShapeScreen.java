package com.cc_rc.gui;

import com.cc_rc.CcRc;
import com.cc_rc.block.console.draw.ConsoleDrawPresets;
import com.cc_rc.block.console.draw.ConsoleShape;
import com.cc_rc.block.console.draw.ConsoleShapeType;
import com.cc_rc.block.graffiti.GraffitiShapeBlockEntity;
import com.cc_rc.network.GraffitiShapeConfigPacket;
import com.cc_rc.network.ModNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;

/**
 * 涂鸦形状设置界面。
 *
 * 布局（复用控制台文字界面的 192x234 面板贴图）：
 *  - 图形类型切换按钮（预设图案 / 实心矩形 / 空心矩形 / 线段 / 箭头）；
 *  - 预设图案网格（24 个缩略图，点选后自动切回「预设图案」类型）；
 *  - 16 个染料色块（颜色）；
 *  - dx / dy 偏移（单位格，向右 / 向上为正）；
 *  - 大小（图案边长，单位格）与线宽（表面像素，仅空心矩形 / 线段 / 箭头）；
 *  - 旋转（度，绕图案中心）；
 *  - 完成按钮：经 GraffitiShapeConfigPacket 把全部参数写回方块实体。
 */
public class GraffitiShapeScreen extends AbstractContainerScreen<GraffitiShapeMenu> {

    /** 背景沿用控制台文字编辑界面的面板贴图（192x234）。 */
    private static final ResourceLocation GUI_BG =
            new ResourceLocation(CcRc.MODID, "textures/gui/console_text_gui.png");

    private static final int BG_WIDTH = 192;
    private static final int BG_HEIGHT = 234;

    // 图形类型
    private static final int TYPE_Y = 24;
    private static final int TYPE_H = 16;

    // 预设图案网格：8 列 × 3 行，每格 18×18（缩略图 16×16 + 1 像素内边）
    private static final int PRESET_Y = 44;
    private static final int PRESET_CELL = 21;
    private static final int PRESET_ICON = 16;
    private static final int PRESET_COLS = 8;

    // 染料色块
    private static final int COLOR_Y = 108;
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

    // 大小 / 线宽
    private static final int SIZE_Y = 156;
    private static final int SIZE_BOX_X = 40;
    private static final int SIZE_BOX_W = 50;
    private static final int WIDTH_LABEL_X = 96;
    private static final int WIDTH_BOX_X = 122;
    private static final int WIDTH_BOX_W = 46;

    // 旋转
    private static final int ROT_Y = 176;
    private static final int ROT_BOX_X = 40;
    private static final int ROT_BOX_W = 60;

    // 完成
    private static final int DONE_Y = 198;
    private static final int DONE_H = 20;

    private static final int LEFT = 12;
    private static final int WIDTH = 168;

    /**
     * 类型按钮上显示的中文名。
     * 必须按枚举值映射，不能按下标取数组：枚举顺序为 RECT / RECT_OUTLINE / LINE / ARROW / PRESET，
     * 与展示顺序（预设图案在最前）不同，用下标会整体错位（选空心矩形却画出线段）。
     */
    private static String typeName(ConsoleShapeType type) {
        return switch (type) {
            case PRESET -> "预设图案";
            case RECT -> "实心矩形";
            case RECT_OUTLINE -> "空心矩形";
            case LINE -> "线段";
            case ARROW -> "箭头";
        };
    }

    private ConsoleShapeType type;
    private String preset;
    private int color;

    private Button typeButton;
    private EditBox dxBox;
    private EditBox dyBox;
    private EditBox sizeBox;
    private EditBox widthBox;
    private EditBox rotationBox;

    public GraffitiShapeScreen(GraffitiShapeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.translatable("block.cc_rc.graffiti_shape"));
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.leftPos;
        int cy = this.topPos;

        // 初值来自菜单（已有涂鸦带出原参数）
        this.type = ConsoleShapeType.byOrdinal(menu.type);
        this.preset = ConsoleDrawPresets.byId(menu.preset) != null
                ? menu.preset : ConsoleDrawPresets.DEFAULT_ID;
        this.color = menu.color & 0xFFFFFF;

        // 图形类型切换按钮
        this.typeButton = Button.builder(Component.empty(),
                        b -> {
                            this.type = stepType(this.type, 1);
                            refreshTypeButton();
                        })
                .bounds(cx + LEFT, cy + TYPE_Y, WIDTH, TYPE_H)
                .build();
        this.typeButton.setTooltip(Tooltip.create(
                Component.literal("图形类型：左键切换，滚轮翻页")));
        refreshTypeButton();
        this.addRenderableWidget(this.typeButton);

        // 预设图案网格
        List<ConsoleDrawPresets.Preset> presets = ConsoleDrawPresets.all();
        for (int i = 0; i < presets.size(); i++) {
            ConsoleDrawPresets.Preset p = presets.get(i);
            int x = cx + LEFT + (i % PRESET_COLS) * PRESET_CELL;
            int y = cy + PRESET_Y + (i / PRESET_COLS) * PRESET_CELL;
            PresetButton btn = new PresetButton(x, y, p);
            btn.setTooltip(Tooltip.create(Component.literal(p.id())));
            this.addRenderableWidget(btn);
        }

        // 染料色块
        DyeColor[] dyes = DyeColor.values();
        for (int i = 0; i < dyes.length; i++) {
            int rgb = dyes[i].getTextColor();
            int x = cx + LEFT + (i % COLOR_COLS) * (COLOR_W + COLOR_GAP_X);
            int y = cy + COLOR_Y + (i / COLOR_COLS) * (COLOR_H + COLOR_GAP_Y);
            ColorSwatch swatch = new ColorSwatch(x, y, rgb, Component.translatable(
                    "color.minecraft." + dyes[i].getName()));
            swatch.setTooltip(Tooltip.create(Component.translatable(
                    "color.minecraft." + dyes[i].getName())));
            this.addRenderableWidget(swatch);
        }

        // dx / dy 偏移
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

        // 大小 / 线宽
        this.sizeBox = new EditBox(this.font, cx + SIZE_BOX_X, cy + SIZE_Y, SIZE_BOX_W, POS_H,
                Component.literal("大小"));
        this.sizeBox.setMaxLength(6);
        this.sizeBox.setValue(formatFloat(menu.size));
        this.sizeBox.setTooltip(Tooltip.create(Component.literal(
                "图案大小：边长（格），1 = 一格，0.1~16")));
        this.addWidget(this.sizeBox);

        this.widthBox = new EditBox(this.font, cx + WIDTH_BOX_X, cy + SIZE_Y,
                WIDTH_BOX_W, POS_H, Component.literal("线宽"));
        this.widthBox.setMaxLength(6);
        this.widthBox.setValue(formatFloat(menu.width));
        this.widthBox.setTooltip(Tooltip.create(Component.literal(
                "线宽（表面像素，1 像素 = 1/16 格；仅空心矩形 / 线段 / 箭头使用）")));
        this.addWidget(this.widthBox);

        // 旋转
        this.rotationBox = new EditBox(this.font, cx + ROT_BOX_X, cy + ROT_Y,
                ROT_BOX_W, POS_H, Component.literal("旋转"));
        this.rotationBox.setMaxLength(6);
        this.rotationBox.setValue(formatFloat(menu.rotation));
        this.rotationBox.setTooltip(Tooltip.create(Component.literal("旋转角度（度，绕图案中心）")));
        this.addWidget(this.rotationBox);

        // 完成按钮
        Button done = Button.builder(Component.literal("完成"), b -> this.submitAndClose())
                .bounds(cx + LEFT, cy + DONE_Y, WIDTH, DONE_H)
                .build();
        this.addRenderableWidget(done);
    }

    /** 按枚举顺序循环切换图形类型（step 为 +1 向后、-1 向前）。 */
    private static ConsoleShapeType stepType(ConsoleShapeType current, int step) {
        ConsoleShapeType[] values = ConsoleShapeType.values();
        int index = (current.ordinal() + step) % values.length;
        if (index < 0) {
            index += values.length;
        }
        return values[index];
    }

    private void refreshTypeButton() {
        this.typeButton.setMessage(Component.literal("类型：" + typeName(this.type)));
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
        float size = parseFloat(this.sizeBox, menu.size);
        float width = parseFloat(this.widthBox, menu.width);
        float rotation = parseFloat(this.rotationBox, menu.rotation);

        dx = Math.max(-GraffitiShapeBlockEntity.MAX_OFFSET,
                Math.min(GraffitiShapeBlockEntity.MAX_OFFSET, dx));
        dy = Math.max(-GraffitiShapeBlockEntity.MAX_OFFSET,
                Math.min(GraffitiShapeBlockEntity.MAX_OFFSET, dy));
        size = Math.max(GraffitiShapeBlockEntity.MIN_SIZE,
                Math.min(GraffitiShapeBlockEntity.MAX_SIZE, size));
        width = ConsoleShape.quantize(Math.max(ConsoleShape.MIN_WIDTH,
                Math.min(ConsoleShape.MAX_WIDTH, width)));
        rotation = ConsoleShape.quantize((rotation % 360.0F + 360.0F) % 360.0F);

        ModNetwork.CHANNEL.sendToServer(new GraffitiShapeConfigPacket(
                menu.pos, this.type.ordinal(), this.preset, this.color,
                size, width, rotation, dx, dy));
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
        graphics.drawString(this.font, "dx", LEFT - 2, POS_Y + 4, labelColor, false);
        graphics.drawString(this.font, "dy", DY_LABEL_X, POS_Y + 4, labelColor, false);
        graphics.drawString(this.font, "大小", LEFT - 2, SIZE_Y + 4, labelColor, false);
        graphics.drawString(this.font, "线宽", WIDTH_LABEL_X, SIZE_Y + 4, labelColor, false);
        graphics.drawString(this.font, "旋转", LEFT - 2, ROT_Y + 4, labelColor, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        // EditBox 不会被 AbstractContainerScreen 自动渲染
        this.dxBox.render(graphics, mouseX, mouseY, partialTick);
        this.dyBox.render(graphics, mouseX, mouseY, partialTick);
        this.sizeBox.render(graphics, mouseX, mouseY, partialTick);
        this.widthBox.render(graphics, mouseX, mouseY, partialTick);
        this.rotationBox.render(graphics, mouseX, mouseY, partialTick);
    }

    /** 鼠标滚轮悬停在类型按钮上时切换图形类型（上滚向前、下滚向后）。 */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.typeButton != null && this.typeButton.isMouseOver(mouseX, mouseY)) {
            this.type = stepType(this.type, delta > 0.0D ? 1 : -1);
            refreshTypeButton();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean enter = keyCode == 257 || keyCode == 335;
        if (enter && (this.dxBox.isFocused() || this.dyBox.isFocused()
                || this.sizeBox.isFocused() || this.widthBox.isFocused()
                || this.rotationBox.isFocused())) {
            this.submitAndClose();
            return true;
        }
        // 拦截背包键：设置界面不应被背包键关闭
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** 预设图案缩略图按钮：选中时画白色边框，点击后自动切到「预设图案」类型。 */
    private class PresetButton extends Button {

        private final ConsoleDrawPresets.Preset preset;

        PresetButton(int x, int y, ConsoleDrawPresets.Preset preset) {
            super(x, y, PRESET_CELL - 2, PRESET_CELL - 2, Component.literal(preset.id()),
                    b -> {
                        GraffitiShapeScreen.this.preset = preset.id();
                        GraffitiShapeScreen.this.type = ConsoleShapeType.PRESET;
                        GraffitiShapeScreen.this.refreshTypeButton();
                    }, DEFAULT_NARRATION);
            this.preset = preset;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int size = this.preset.size();
            graphics.blit(this.preset.texture(), this.getX(), this.getY(),
                    PRESET_ICON, PRESET_ICON, 0.0F, 0.0F, size, size, size, size);
            int border = this.preset.id().equals(GraffitiShapeScreen.this.preset)
                    ? 0xFFFFFFFF : 0xFF303030;
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
