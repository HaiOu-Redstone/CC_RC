package com.cc_rc.client;

import com.cc_rc.block.console.ConsoleBusBlockEntity;
import com.cc_rc.network.ConsoleNumpadSetPacket;
import com.cc_rc.network.ModNetwork;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * 数字键盘（numpad）的使用界面：空手右键数字键盘时打开。
 *
 * 上方一个数字输入框，下方「确认 / 放弃」两个按钮：
 *  - 确认：解析输入值并发包给服务端写入组件（同时回写绑定的浮点变量）；
 *  - 放弃 / Esc：清空输入框并关闭界面，组件原本的数值保持不变。
 *
 * 界面完全在客户端打开（不占用容器菜单），数值校验与写入以服务端为准。
 */
public class ConsoleNumpadScreen extends Screen {

    private static final int W = 200;
    private static final int H = 104;
    /** 输入框尺寸与位置（相对面板左上角）。 */
    private static final int BOX_X = 20;
    private static final int BOX_Y = 34;
    private static final int BOX_W = 160;
    private static final int BOX_H = 18;
    /** 底部按钮。 */
    private static final int BTN_Y = 70;
    private static final int BTN_W = 72;
    private static final int BTN_H = 20;

    /** 总线方块坐标与组件 id（确认时随包提交给服务端）。 */
    private final BlockPos busPos;
    private final int compId;

    private int leftPos;
    private int topPos;
    private EditBox valueBox;
    /** 校验失败提示（空 = 不显示）。 */
    private String error = "";

    private ConsoleNumpadScreen(BlockPos busPos, int compId) {
        super(Component.translatable("gui.cc_rc.numpad.title"));
        this.busPos = busPos;
        this.compId = compId;
    }

    /** 客户端打开数字键盘的使用界面。 */
    public static void open(BlockPos busPos, int compId) {
        Minecraft.getInstance().setScreen(new ConsoleNumpadScreen(busPos, compId));
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - W) / 2;
        this.topPos = (this.height - H) / 2;

        // 数字输入框：只允许「-」「.」与数字，最长 14 位，避免输入无效内容
        this.valueBox = new EditBox(this.font, this.leftPos + BOX_X, this.topPos + BOX_Y,
                BOX_W, BOX_H, Component.translatable("gui.cc_rc.numpad.value"));
        this.valueBox.setMaxLength(14);
        this.valueBox.setFilter(text -> text.matches("-?\\d*(\\.\\d*)?"));
        this.valueBox.setValue(formatCurrentValue());
        this.addRenderableWidget(this.valueBox);

        // 确认 / 放弃
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cc_rc.numpad.confirm"),
                        b -> confirm())
                .bounds(this.leftPos + W / 2 - BTN_W - 6, this.topPos + BTN_Y, BTN_W, BTN_H)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cc_rc.numpad.cancel"),
                        b -> onClose())
                .bounds(this.leftPos + W / 2 + 6, this.topPos + BTN_Y, BTN_W, BTN_H)
                .build());
    }

    /** 读取客户端方块实体里组件的当前值，作为输入框初始内容。 */
    private String formatCurrentValue() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return "0";
        }
        ConsoleBusBlockEntity be = this.minecraft.level.getBlockEntity(this.busPos)
                instanceof ConsoleBusBlockEntity found ? found : null;
        if (be == null) {
            return "0";
        }
        ConsoleBusBlockEntity.CompEntry comp = be.findComp(this.compId);
        if (comp == null || !Float.isFinite(comp.value)) {
            return "0";
        }
        // 去掉多余的末尾 0（整数显示为整数），保留最多 3 位小数用于编辑
        String text = String.format(Locale.ROOT, "%.3f", comp.value);
        text = text.replaceAll("0+$", "");
        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }

    /** 确认：解析输入值并发包；非法输入只显示提示，不关闭界面。 */
    private void confirm() {
        String text = this.valueBox.getValue().trim();
        float value;
        try {
            value = Float.parseFloat(text);
        } catch (NumberFormatException e) {
            this.error = Component.translatable("gui.cc_rc.numpad.invalid").getString();
            return;
        }
        if (!Float.isFinite(value)) {
            this.error = Component.translatable("gui.cc_rc.numpad.invalid").getString();
            return;
        }
        ModNetwork.CHANNEL.sendToServer(new ConsoleNumpadSetPacket(this.busPos, this.compId, value));
        // 提交后关闭界面（输入值已发送，无需再清空）
        super.onClose();
    }

    /** 放弃 / Esc：先清空输入值，再关闭界面（不发包，组件数值不变）。 */
    @Override
    public void onClose() {
        if (this.valueBox != null) {
            this.valueBox.setValue("");
        }
        super.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 回车等价于点「确认」，方便键盘输入
        if (keyCode == 257 || keyCode == 335) {
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int x = this.leftPos;
        int y = this.topPos;
        // 深色面板 + 灰色边框
        graphics.fill(x, y, x + W, y + H, 0xE0101010);
        border(graphics, x, y, x + W, y + H, 0xFF808080);
        graphics.drawString(this.font, this.title, x + 8, y + 8, 0xFFFFFFFF, false);
        graphics.drawString(this.font, Component.translatable("gui.cc_rc.numpad.value"),
                x + BOX_X, y + 22, 0xFFA0A0A0, false);
        if (!this.error.isEmpty()) {
            graphics.drawString(this.font, this.error, x + BOX_X, y + 56, 0xFFFF5555, false);
        }
        // 控件（输入框 / 按钮）最后绘制，保证位于面板之上
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static void border(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        graphics.fill(x0, y0, x1, y0 + 1, color);
        graphics.fill(x0, y1 - 1, x1, y1, color);
        graphics.fill(x0, y0, x0 + 1, y1, color);
        graphics.fill(x1 - 1, y0, x1, y1, color);
    }
}
