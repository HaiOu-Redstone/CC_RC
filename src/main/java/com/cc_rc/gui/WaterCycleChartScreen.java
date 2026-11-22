package com.cc_rc.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 水循环图高清查看界面（只负责显示与关闭，没有其它交互）。
 *
 * 显存策略：高清贴图（5000x3536，约 67MB 显存）只在界面打开时通过 TextureManager 加载，
 * 关闭界面时 release 释放；墙面渲染只用 512x362 低清贴图，所以平时不占大显存。
 * 代价是每次打开需要解码一次大 PNG（短暂卡顿）。
 */
public class WaterCycleChartScreen extends Screen {

    /** 高清贴图（与低清墙面图分开存放，正常游戏过程中不会被其它渲染引用）。 */
    private static final ResourceLocation HD_TEXTURE =
            new ResourceLocation("cc_rc", "textures/gui/water_cycle_diagram_hd.png");
    /** 原图像素尺寸（等比适配窗口时使用）。 */
    private static final int TEX_WIDTH = 5000;
    private static final int TEX_HEIGHT = 3536;

    public WaterCycleChartScreen() {
        super(Component.translatable("gui.cc_rc.water_cycle_chart.title"));
    }

    @Override
    protected void init() {
        // 打开界面时才加载高清图（TextureManager 会在关闭时由 removed() 释放）
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(HD_TEXTURE);
        // 线性过滤：大图缩小显示时更平滑（不开 mipmap，避免额外显存）
        texture.setFilter(true, false);

        // 「完成」按钮缩到 44×14 并放右上角，尽量减少对图片的遮挡
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(this.width - 48, 4, 44, 14)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 原版背景（窗口比图片宽时，左右两侧显示暗化背景）
        this.renderBackground(graphics);

        // 上下贴满窗口：按窗口高度等比缩放，水平居中；窗口过窄时左右自然裁掉
        float scale = this.height / (float) TEX_HEIGHT;
        int drawWidth = Math.max(1, Math.round(TEX_WIDTH * scale));
        int x = (this.width - drawWidth) / 2;
        graphics.blit(HD_TEXTURE, x, 0, drawWidth, this.height,
                0.0F, 0.0F, TEX_WIDTH, TEX_HEIGHT, TEX_WIDTH, TEX_HEIGHT);

        super.render(graphics, mouseX, mouseY, partialTick);

        // 标题加深色半透明底，避免压在浅色图画上看不清
        int titleWidth = this.font.width(this.title);
        graphics.fill(4, 4, 12 + titleWidth, 10 + this.font.lineHeight, 0x99000000);
        graphics.drawString(this.font, this.title, 8, 8, 0xFFFFFF, false);
    }

    @Override
    public void removed() {
        super.removed();
        // 关闭界面即释放高清贴图的显存，下次打开重新加载
        Minecraft.getInstance().getTextureManager().release(HD_TEXTURE);
    }

    /** 不暂停游戏（只是查看图片）。 */
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
