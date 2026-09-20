package com.cc_rc.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.cc_rc.CcRc;
import com.cc_rc.network.EditTextPacket;
import com.cc_rc.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;

/**
 * 编辑工具文字编辑屏幕（自定义 GUI 背景 edit_tool_gui.png）。
 *
 * 布局保持与原版铁砧 GUI 相同的位置规格（imageWidth=176 / imageHeight=166，
 * 仅替换背景贴图，不移动任何控件）：
 *  - 背景：assets/<modid>/textures/gui/edit_tool_gui.png（256x256 画布，
 *    左上 176x166 即铁砧样式的编辑界面区域，来源「模型/其他物品/edit_tool_gui.png」）；
 *  - 标题（"编辑文字"）在左上角原位；"当前：<原文字>"显示在其右侧同行；
 *  - 输入框：铁砧文字输入栏原位（leftPos+12, topPos+34，宽 152）；
 *  - 「完成」按钮：输入框下方（topPos+68），Enter 亦可提交；
 *  - 文字格式：topPos+96 一排 B(粗体)/I(斜体)/U(下划线)/S(删除线) 切换按钮，
 *    激活时白色粗体、未激活白色（仅以粗细区分），topPos+118 起 16 个染料色块按钮（8 列 x 2 行），
 *    点击把文字染成对应染料颜色，白色外框 = 当前选中颜色；
 *  - 提交时把文字与完整样式（颜色 + 四种格式）经 EditTextPacket 写回目标方块。
 *
 * 按键：Esc 关闭；输入框聚焦时 Enter 提交；**E（背包键）被拦截**——
 * 编辑工具界面为纯输入界面，按 E 不应触发原版背包键关闭行为。
 */
public class EditTextScreen extends AbstractContainerScreen<EditTextMenu> {

    private static final ResourceLocation GUI_BG =
            new ResourceLocation(CcRc.MODID, "textures/gui/edit_tool_gui.png");
    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT = 166;

    // 控件布局（相对 GUI 左上角）
    private static final int INPUT_X = 12;
    private static final int INPUT_Y = 34;
    private static final int INPUT_W = 152;
    private static final int INPUT_H = 16;
    private static final int DONE_X = 12;
    private static final int DONE_Y = 68;
    private static final int DONE_W = 152;
    private static final int DONE_H = 20;
    private static final int FORMAT_Y = 96;        // 格式按钮行
    private static final int FORMAT_W = 34;
    private static final int FORMAT_H = 16;
    private static final int FORMAT_GAP = 6;
    private static final int COLOR_Y = 118;        // 色块按钮起始行
    private static final int COLOR_W = 16;
    private static final int COLOR_H = 10;
    private static final int COLOR_GAP_X = 2;
    private static final int COLOR_GAP_Y = 2;
    private static final int COLOR_COLS = 8;

    // 文字样式状态（初值取自已显示文字，提交时全量写回）
    private int color = -1;          // -1 = 不设置颜色（恢复默认白）
    private boolean bold;
    private boolean italic;
    private boolean underline;
    private boolean strikethrough;

    private EditBox input;
    private Button doneButton;

    public EditTextScreen(EditTextMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.translatable("item.cc_rc.edit_tool"));
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.leftPos;
        int cy = this.topPos;

        // 从当前显示文字读取已有样式，作为按钮/色块的初始状态
        Style style = menu.currentText.getStyle();
        this.bold = style.isBold();
        this.italic = style.isItalic();
        this.underline = style.isUnderlined();
        this.strikethrough = style.isStrikethrough();
        TextColor textColor = style.getColor();
        this.color = textColor == null ? -1 : textColor.getValue();

        // 输入框：铁砧文字输入栏原位（左上 12,34，宽 152，高 16）
        this.input = new EditBox(this.font, cx + INPUT_X, cy + INPUT_Y, INPUT_W, INPUT_H,
                Component.literal("输入显示文字"));
        this.input.setMaxLength(32);
        this.input.setValue(menu.currentText.getString());
        this.input.setResponder(s -> {
            // 空文本 = 清除显示（允许提交），纯空格提交时会由服务端 trim
            doneButton.active = true;
        });
        this.addWidget(this.input);
        this.setInitialFocus(this.input);

        // 完成按钮：输入框下方（原位，铁砧"利用"按钮附近）
        this.doneButton = Button.builder(Component.literal("完成"),
                        (btn) -> this.submitAndClose())
                .bounds(cx + DONE_X, cy + DONE_Y, DONE_W, DONE_H)
                .build();
        this.addRenderableWidget(this.doneButton);

        // 格式按钮行：B / I / U / S（粗体 / 斜体 / 下划线 / 删除线）
        addFormatButton(cx + INPUT_X + 0 * (FORMAT_W + FORMAT_GAP), cy + FORMAT_Y,
                "B", "粗体", () -> this.bold, v -> this.bold = v);
        addFormatButton(cx + INPUT_X + 1 * (FORMAT_W + FORMAT_GAP), cy + FORMAT_Y,
                "I", "斜体", () -> this.italic, v -> this.italic = v);
        addFormatButton(cx + INPUT_X + 2 * (FORMAT_W + FORMAT_GAP), cy + FORMAT_Y,
                "U", "下划线", () -> this.underline, v -> this.underline = v);
        addFormatButton(cx + INPUT_X + 3 * (FORMAT_W + FORMAT_GAP), cy + FORMAT_Y,
                "S", "删除线", () -> this.strikethrough, v -> this.strikethrough = v);

        // 染色色块：16 种染料颜色（8 列 x 2 行）
        DyeColor[] dyes = DyeColor.values();
        for (int i = 0; i < dyes.length; i++) {
            int rgb = dyes[i].getTextColor();
            int x = cx + INPUT_X + (i % COLOR_COLS) * (COLOR_W + COLOR_GAP_X);
            int y = cy + COLOR_Y + (i / COLOR_COLS) * (COLOR_H + COLOR_GAP_Y);
            ColorSwatch swatch = new ColorSwatch(x, y, rgb,
                    Component.literal("颜色：").append(Component.translatable(
                            "color.minecraft." + dyes[i].getName())));
            swatch.setTooltip(Tooltip.create(Component.translatable(
                    "color.minecraft." + dyes[i].getName())));
            this.addRenderableWidget(swatch);
        }
    }

    /** 创建格式切换按钮：白字粗体=激活，灰字=未激活，点击翻转状态。 */
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

    /** 刷新格式按钮外观：激活 = 白色粗体，未激活 = 白色（仅以粗细区分，保证暗背景下可读性）。 */
    private void refreshFormatButton(Button btn, String tag, boolean active) {
        btn.setMessage(Component.literal(tag).withStyle(
                Style.EMPTY.withBold(active).withColor(0xFFFFFF)));
    }

    /** 提交输入并关闭：发送 C2S 包把文字与样式写入目标方块。 */
    private void submitAndClose() {
        String text = this.input.getValue();
        ModNetwork.CHANNEL.sendToServer(new EditTextPacket(
                menu.pos, text, color, bold, italic, underline, strikethrough));
        this.onClose();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        // 绘制自定义编辑界面背景（取画布左上 176x166 区域，即铁砧风格编辑界面）
        graphics.blit(GUI_BG, this.leftPos, this.topPos, 0, 0, BG_WIDTH, BG_HEIGHT);
    }

    /**
     * 自定义标签渲染：
     *  - 只画界面标题（"编辑文字"）在左上角原位；
     *  - 不调用 super.renderLabels —— 该界面无物品槽位，避免出现"物品栏"字样；
     *  - 原文字（"当前：xxx"）显示在标题右侧同一行。
     */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        // 原文字显示在标题右边（"编辑文字"右侧）
        String current = menu.currentText.getString();
        if (!current.isEmpty()) {
            graphics.drawString(this.font,
                    "当前：" + current,
                    this.titleLabelX + this.font.width(this.title) + 10,
                    this.titleLabelY,
                    0x404040, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        // 输入框需要手动渲染（AbstractContainerScreen 不会自动渲染 addWidget 的 EditBox）
        this.input.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter 提交
        if (this.input.isFocused() && (keyCode == 257 || keyCode == 335)) {
            this.submitAndClose();
            return true;
        }
        // 拦截背包键（默认 E）：编辑工具界面不应被背包键关闭
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * 染料色块按钮：自绘纯色方块 + 边框，白色边框 = 当前选中颜色。
     * 点击把文字颜色设为该染料颜色（提交时随 EditTextPacket 写回）。
     */
    private class ColorSwatch extends Button {

        private final int rgb;

        ColorSwatch(int x, int y, int rgb, Component message) {
            super(x, y, COLOR_W, COLOR_H, message, b -> {
                color = rgb;
            }, DEFAULT_NARRATION);
            this.rgb = rgb;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // 填充色块本身
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000 | rgb);
            // 边框：白色 = 当前选中；浅色 = 悬停；深色 = 普通
            boolean selected = color == rgb || (color < 0 && rgb == 0xFFFFFF);
            int border = selected ? 0xFFFFFFFF
                    : (isHoveredOrFocused() ? 0xFFE0E0E0 : 0xFF555555);
            graphics.fill(getX(), getY(), getX() + width, getY() + 1, border);
            graphics.fill(getX(), getY(), getX() + 1, getY() + height, border);
            graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, border);
            graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, border);
        }
    }
}