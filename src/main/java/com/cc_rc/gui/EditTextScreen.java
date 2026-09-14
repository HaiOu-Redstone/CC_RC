package com.cc_rc.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.cc_rc.CcRc;
import com.cc_rc.network.EditTextPacket;
import com.cc_rc.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 编辑工具文字编辑屏幕（自定义 GUI 背景 edit_tool_gui.png）。
 *
 * 布局保持与原版铁砧 GUI 相同的位置规格（imageWidth=176 / imageHeight=166，
 * 仅替换背景贴图，不移动任何控件）：
 *  - 背景：assets/<modid>/textures/gui/edit_tool_gui.png（256x256 画布，
 *    左上 176x166 即铁砧样式的编辑界面区域，来源「模型/其他物品/edit_tool_gui.png」）；
 *  - 标题（"编辑文字"）在左上角原位；"当前：<原文字>"显示在其右侧同行；
 *  - 输入框：铁砧文字输入栏原位（leftPos+12, topPos+34，宽 152）；
 *  - 「完成」按钮：输入框下方（topPos+68），Enter 亦可提交。
 *
 * 按键：Esc 关闭；输入框聚焦时 Enter 提交；**E（背包键）被拦截**——
 * 编辑工具界面为纯输入界面，按 E 不应触发原版背包键关闭行为。
 */
public class EditTextScreen extends AbstractContainerScreen<EditTextMenu> {

    private static final ResourceLocation GUI_BG =
            new ResourceLocation(CcRc.MODID, "textures/gui/edit_tool_gui.png");
    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT = 166;

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

        // 输入框：铁砧文字输入栏原位（左上 12,34，宽 152，高 16）
        this.input = new EditBox(this.font, cx + 12, cy + 34, 152, 16,
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
                .bounds(cx + 12, cy + 68, 152, 20)
                .build();
        this.addRenderableWidget(this.doneButton);
    }

    /** 提交输入并关闭：发送 C2S 包把文字写入目标方块。 */
    private void submitAndClose() {
        String text = this.input.getValue();
        ModNetwork.CHANNEL.sendToServer(new EditTextPacket(menu.pos, text));
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
}