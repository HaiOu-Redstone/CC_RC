package com.cc_rc.gui;

import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * 无阴影单行输入框，用来替代原版 EditBox。
 *
 * 原版 EditBox 内部用带阴影的 GuiGraphics.drawString 画文本，且没有关闭阴影的开关，
 * 放在米黄底色上就会出现「重影」；同时它的光标 / 文本定位逻辑与自绘背景不易对齐。
 * 这里完全自绘：
 *  - 文本不带阴影，垂直居中（与输入框背景对齐）；
 *  - 超宽时横向滚动，并按控件范围裁剪，不会溢出到背景其它位置；
 *  - 聚焦时显示闪烁光标，点击可按像素位置定位光标。
 */
public class ShadowlessTextField extends AbstractWidget {

    /** 文本相对控件左侧的内缩（像素）。 */
    private static final int TEXT_INSET = 4;
    /** 光标闪烁的半周期（毫秒）：亮 500ms、灭 500ms，按真实时间计算，不受帧率影响。 */
    private static final long BLINK_HALF_PERIOD_MS = 500L;

    private final Font font;
    private String value = "";
    private int maxLength = 32;
    /** 光标所在字符下标。 */
    private int cursorPos;
    /** 横向滚动起点（显示的第一个字符下标）。 */
    private int displayPos;
    /** 文本颜色（含 alpha）。 */
    private int textColor = 0xFF000000;

    public ShadowlessTextField(Font font, int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
        this.font = font;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = Math.max(1, maxLength);
        if (this.value.length() > this.maxLength) {
            this.value = this.value.substring(0, this.maxLength);
            this.cursorPos = Math.min(this.cursorPos, this.value.length());
        }
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String text) {
        String safe = text == null ? "" : text;
        if (safe.length() > this.maxLength) {
            safe = safe.substring(0, this.maxLength);
        }
        this.value = safe;
        this.cursorPos = safe.length();
        this.displayPos = 0;
        this.ensureCursorVisible();
    }

    public void setTextColor(int color) {
        this.textColor = color;
    }

    /** 保证光标在可见区域内：按需推进 displayPos。 */
    private void ensureCursorVisible() {
        if (this.cursorPos < this.displayPos) {
            this.displayPos = this.cursorPos;
        }
        int inner = this.width - TEXT_INSET * 2;
        while (this.displayPos < this.cursorPos
                && this.font.width(this.value.substring(this.displayPos, this.cursorPos)) > inner) {
            this.displayPos++;
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.ensureCursorVisible();
        int textX = this.getX() + TEXT_INSET;
        int textY = this.getY() + (this.height - 8) / 2;

        // 裁剪到输入框内部（留 1 像素给自绘描边），避免长文本溢出
        graphics.enableScissor(this.getX() + 1, this.getY() + 1,
                this.getX() + this.width - 1, this.getY() + this.height - 1);
        // 无阴影绘制：drawString(..., false)
        graphics.drawString(this.font, this.value.substring(this.displayPos),
                textX, textY, this.textColor, false);
        if (this.isFocused() && Util.getMillis() / BLINK_HALF_PERIOD_MS % 2L == 0L) {
            int cursorX = textX + this.font.width(
                    this.value.substring(this.displayPos, this.cursorPos));
            graphics.fill(cursorX, textY - 1, cursorX + 1, textY + 9, this.textColor);
        }
        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !this.active || !this.visible || !this.clicked(mouseX, mouseY)) {
            return false;
        }
        // 按点击位置找最近的字符边界作为光标位置
        int rel = (int) Math.round(mouseX - this.getX() - TEXT_INSET);
        int bestIndex = this.displayPos;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = this.displayPos; i <= this.value.length(); i++) {
            int width = this.font.width(this.value.substring(this.displayPos, i));
            int distance = Math.abs(width - rel);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        this.cursorPos = bestIndex;
        this.ensureCursorVisible();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.isFocused()) {
            return false;
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (this.cursorPos > 0) {
                    this.value = this.value.substring(0, this.cursorPos - 1)
                            + this.value.substring(this.cursorPos);
                    this.cursorPos--;
                    this.ensureCursorVisible();
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (this.cursorPos < this.value.length()) {
                    this.value = this.value.substring(0, this.cursorPos)
                            + this.value.substring(this.cursorPos + 1);
                    this.ensureCursorVisible();
                }
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (this.cursorPos > 0) {
                    this.cursorPos--;
                    this.ensureCursorVisible();
                }
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (this.cursorPos < this.value.length()) {
                    this.cursorPos++;
                    this.ensureCursorVisible();
                }
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                this.cursorPos = 0;
                this.displayPos = 0;
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                this.cursorPos = this.value.length();
                this.ensureCursorVisible();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!this.isFocused() || !this.active) {
            return false;
        }
        if (!SharedConstants.isAllowedChatCharacter(codePoint) || this.value.length() >= this.maxLength) {
            return false;
        }
        this.value = this.value.substring(0, this.cursorPos) + codePoint
                + this.value.substring(this.cursorPos);
        this.cursorPos++;
        this.ensureCursorVisible();
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE,
                Component.translatable("gui.narrate.editBox", this.getMessage(), this.value));
    }
}
