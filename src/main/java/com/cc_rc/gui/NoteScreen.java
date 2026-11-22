package com.cc_rc.gui;

import com.cc_rc.CcRc;
import com.cc_rc.entity.NoteEntity;
import com.cc_rc.network.ModNetwork;
import com.cc_rc.network.NoteConfigPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;

/**
 * 便签编辑界面：四个输入框对应便签的四行文字。
 *
 *  - 点击某一行输入框即选中该行，随后点颜色色块 / B I U S 按钮只改这一行；
 *  - 每行颜色不同时，输入框文字颜色同步显示该行颜色；
 *  - 「完成」把四行文字、颜色与格式打包成 CompoundTag 发送 NoteConfigPacket。
 *
 * 复用控制台文字界面的 192x234 面板贴图，整体风格与涂鸦编辑界面一致。
 */
public class NoteScreen extends AbstractContainerScreen<NoteMenu> {

    private static final ResourceLocation GUI_BG =
            new ResourceLocation(CcRc.MODID, "textures/gui/console_text_gui.png");
    private static final int BG_WIDTH = 192;
    private static final int BG_HEIGHT = 234;

    // 四行输入框
    private static final int INPUT_X = 12;
    private static final int INPUT_W = 168;
    private static final int INPUT_H = 16;
    private static final int LINE_Y = 26;
    private static final int LINE_STEP = 18;

    // 完成
    private static final int DONE_Y = 104;
    private static final int DONE_H = 20;

    // B / I / U / S
    private static final int FORMAT_Y = 130;
    private static final int FORMAT_W = 34;
    private static final int FORMAT_H = 16;
    private static final int FORMAT_GAP = 6;

    // 16 个染料色块
    private static final int COLOR_Y = 152;
    private static final int COLOR_W = 16;
    private static final int COLOR_H = 10;
    private static final int COLOR_GAP_X = 2;
    private static final int COLOR_GAP_Y = 2;
    private static final int COLOR_COLS = 8;

    private final ShadowlessTextField[] lineBoxes = new ShadowlessTextField[NoteEntity.LINES];
    private final int[] colors = new int[NoteEntity.LINES];
    private final int[] flags = new int[NoteEntity.LINES];
    private final Button[] formatButtons = new Button[4];
    /** 当前正在设置颜色 / 格式的行号。 */
    private int activeLine;

    public NoteScreen(NoteMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.translatable("entity.cc_rc.note"));
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.leftPos;
        int cy = this.topPos;

        // 从菜单数据带出四行文字 / 颜色 / 格式
        for (int i = 0; i < NoteEntity.LINES; i++) {
            this.colors[i] = menu.data.contains(NoteEntity.colorKey(i))
                    ? menu.data.getInt(NoteEntity.colorKey(i)) & 0xFFFFFF
                    : NoteEntity.DEFAULT_COLOR;
            this.flags[i] = menu.data.getInt(NoteEntity.flagKey(i));
        }

        // 四个输入框（自绘无阴影文本，避免米黄底上出现重影 / 文字贴顶）
        for (int i = 0; i < NoteEntity.LINES; i++) {
            ShadowlessTextField box = new ShadowlessTextField(this.font,
                    cx + INPUT_X, cy + LINE_Y + i * LINE_STEP,
                    INPUT_W, INPUT_H, Component.literal("第 " + (i + 1) + " 行"));
            box.setMaxLength(NoteEntity.MAX_CHARS);
            box.setValue(menu.data.getString(NoteEntity.lineKey(i)));
            box.setTooltip(Tooltip.create(Component.literal(
                    "第 " + (i + 1) + " 行（点击选中后可设置颜色 / 格式）")));
            // 加入 renderables：由 super.render 统一在背景之后绘制，无需手动 render
            this.addRenderableWidget(box);
            this.lineBoxes[i] = box;
        }
        this.setInitialFocus(this.lineBoxes[0]);

        // 完成
        Button done = Button.builder(Component.literal("完成"), b -> this.submitAndClose())
                .bounds(cx + INPUT_X, cy + DONE_Y, INPUT_W, DONE_H)
                .build();
        this.addRenderableWidget(done);

        // 字体格式 B / I / U / S（作用于当前选中行）
        this.formatButtons[0] = addFormatButton(cx + INPUT_X + 0 * (FORMAT_W + FORMAT_GAP),
                cy + FORMAT_Y, "B", "粗体", NoteEntity.FLAG_BOLD);
        this.formatButtons[1] = addFormatButton(cx + INPUT_X + 1 * (FORMAT_W + FORMAT_GAP),
                cy + FORMAT_Y, "I", "斜体", NoteEntity.FLAG_ITALIC);
        this.formatButtons[2] = addFormatButton(cx + INPUT_X + 2 * (FORMAT_W + FORMAT_GAP),
                cy + FORMAT_Y, "U", "下划线", NoteEntity.FLAG_UNDERLINE);
        this.formatButtons[3] = addFormatButton(cx + INPUT_X + 3 * (FORMAT_W + FORMAT_GAP),
                cy + FORMAT_Y, "S", "删除线", NoteEntity.FLAG_STRIKE);

        // 染料色块（作用于当前选中行）
        DyeColor[] dyes = DyeColor.values();
        for (int i = 0; i < dyes.length; i++) {
            int rgb = dyes[i].getTextColor();
            int x = cx + INPUT_X + (i % COLOR_COLS) * (COLOR_W + COLOR_GAP_X);
            int y = cy + COLOR_Y + (i / COLOR_COLS) * (COLOR_H + COLOR_GAP_Y);
            ColorSwatch swatch = new ColorSwatch(x, y, rgb);
            swatch.setTooltip(Tooltip.create(Component.translatable(
                    "color.minecraft." + dyes[i].getName())));
            this.addRenderableWidget(swatch);
        }

        refreshLineStyles();
    }

    /** 创建格式切换按钮：对当前选中行翻转对应标志位。 */
    private Button addFormatButton(int x, int y, String tag, String name, int flagBit) {
        Button btn = Button.builder(Component.empty(), b -> {
                    this.flags[this.activeLine] ^= flagBit;
                    refreshLineStyles();
                })
                .bounds(x, y, FORMAT_W, FORMAT_H)
                .build();
        btn.setTooltip(Tooltip.create(Component.literal(name + "（作用于选中行）")));
        this.addRenderableWidget(btn);
        return btn;
    }

    /** 把每行颜色同步到输入框文字，并刷新格式按钮的加粗状态。 */
    private void refreshLineStyles() {
        for (int i = 0; i < NoteEntity.LINES; i++) {
            this.lineBoxes[i].setTextColor(0xFF000000 | this.colors[i]);
        }
        String[] tags = {"B", "I", "U", "S"};
        int[] bits = {NoteEntity.FLAG_BOLD, NoteEntity.FLAG_ITALIC,
                NoteEntity.FLAG_UNDERLINE, NoteEntity.FLAG_STRIKE};
        for (int i = 0; i < this.formatButtons.length; i++) {
            boolean active = (this.flags[this.activeLine] & bits[i]) != 0;
            this.formatButtons[i].setMessage(Component.literal(tags[i]).withStyle(
                    Style.EMPTY.withBold(active).withColor(0xFFFFFF)));
        }
    }

    /** 若某个输入框获得焦点，则把它设为当前编辑行。 */
    private void updateActiveFromFocus() {
        for (int i = 0; i < NoteEntity.LINES; i++) {
            if (this.lineBoxes[i].isFocused()) {
                if (this.activeLine != i) {
                    this.activeLine = i;
                    refreshLineStyles();
                }
                return;
            }
        }
    }

    /** 提交并关闭：把四行内容打包发给服务端。 */
    private void submitAndClose() {
        CompoundTag tag = new CompoundTag();
        for (int i = 0; i < NoteEntity.LINES; i++) {
            tag.putString(NoteEntity.lineKey(i), this.lineBoxes[i].getValue());
            tag.putInt(NoteEntity.colorKey(i), this.colors[i]);
            tag.putInt(NoteEntity.flagKey(i), this.flags[i]);
        }
        ModNetwork.CHANNEL.sendToServer(new NoteConfigPacket(menu.entityId, tag));
        this.onClose();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(GUI_BG, this.leftPos, this.topPos, BG_WIDTH, BG_HEIGHT,
                0.0F, 0.0F, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);
        // 输入框自绘米黄色底（与便签纸面中间部分同色 F7EF97），并描边
        int cx = this.leftPos;
        int cy = this.topPos;
        for (int i = 0; i < NoteEntity.LINES; i++) {
            int x = cx + INPUT_X;
            int y = cy + LINE_Y + i * LINE_STEP;
            graphics.fill(x, y, x + INPUT_W, y + INPUT_H, 0xFFF7EF97);
            int border = (i == this.activeLine) ? 0xFF6B5D12 : 0xFFB3A64F;
            graphics.fill(x, y, x + INPUT_W, y + 1, border);
            graphics.fill(x, y + INPUT_H - 1, x + INPUT_W, y + INPUT_H, border);
            graphics.fill(x, y, x + 1, y + INPUT_H, border);
            graphics.fill(x + INPUT_W - 1, y, x + INPUT_W, y + INPUT_H, border);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        // 行提示移到标题栏右侧，避免与「完成」按钮 / B I U S 行重叠
        Component lineLabel = Component.literal("正在编辑第 " + (this.activeLine + 1) + " 行");
        graphics.drawString(this.font, lineLabel,
                BG_WIDTH - 8 - this.font.width(lineLabel), this.titleLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 焦点在点击后才会更新，渲染前同步一次当前编辑行
        this.updateActiveFromFocus();
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        this.updateActiveFromFocus();
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean enter = keyCode == 257 || keyCode == 335;
        if (enter) {
            this.submitAndClose();
            return true;
        }
        // 拦截背包键：编辑界面不应被背包键关闭
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        this.updateActiveFromFocus();
        return handled;
    }

    /** 染料色块按钮：自绘纯色方块 + 边框，白色边框 = 当前行选中的颜色。 */
    private class ColorSwatch extends Button {

        private final int rgb;

        ColorSwatch(int x, int y, int rgb) {
            super(x, y, COLOR_W, COLOR_H, Component.empty(), b -> {
                colors[activeLine] = rgb;
                refreshLineStyles();
            }, DEFAULT_NARRATION);
            this.rgb = rgb;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int argb = 0xFF000000 | this.rgb;
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width,
                    this.getY() + this.height, argb);
            int border = colors[activeLine] == this.rgb ? 0xFFFFFFFF : 0xFF303030;
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
