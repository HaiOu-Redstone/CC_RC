package com.cc_rc.block.Plotter;

import com.cc_rc.block.console_panel.ConsolePanelBER;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 圆盘记录仪渲染器
 * 在文字坐标系内渲染 24 个红色小方块，显示红石信号趋势
 *
 * 渲染策略：使用 Font.drawInBatch 绘制 "█" (FULL BLOCK) 字符
 *   - NORMAL 模式：不透明渲染，颜色鲜艳（避免 lightning() 的 alpha 混合导致颜色变淡）
 *   - 通过 PoseStack 缩放 1/4，让 8 像素字符变为 2 像素方块
 *   - 复用文字渲染管线（已知稳定）
 *
 * 微调方法（修改下方常量后重新编译即可）：
 *   - START_X        ：左右位置。增大 → 向右移；减小 → 向左移
 *   - START_Y        ：上下位置。增大 → 向下移；减小 → 向上移
 *   - STEP_PIXEL     ：数据点 X 方向间距
 *   - VALUE_PIXEL    ：数据值 Y 方向间距
 *   - SCALE          ：缩放比例。1/SCALE = 字符原始大小（8像素）渲染后的实际像素数
 *                       SCALE=0.25 → 字符 2 像素；SCALE=0.5 → 字符 4 像素
 *   - COLOR_ARGB     ：颜色（ARGB 格式）。0xFFB40000 = 不透明深红色 (R=180, G=0, B=0)
 */
public class PlotterBER implements BlockEntityRenderer<PlotterBlockEntity> {

    // ====== 起点坐标（文字像素） ======
    private static final float START_X = 22.0F;
    private static final float START_Y = 48.0F;

    // ====== 间距 / 大小（文字像素） ======
    private static final float STEP_PIXEL = 2.0F;
    private static final float VALUE_PIXEL = 2.0F;

    // ====== 字符缩放 ======
    // 字符原始大小 8 像素，SCALE=0.25 让字符变为 2 像素方块
    private static final float SCALE = 0.25F;
    private static final float INV_SCALE = 1.0F / SCALE;  // 坐标补偿系数

    // ====== 颜色（ARGB 格式，0xAARRGGBB） ======
    // 0xFFB40000 = 不透明深红色（A=255, R=180, G=0, B=0）
    // 0xFFFF0000 = 不透明纯红色（A=255, R=255, G=0, B=0）
    private static final int COLOR_ARGB = 0xFFFF0000;

    // ====== 光照（0xAABBGGRR 格式） ======
    // 0xF000F0 = 全亮光照（确保颜色鲜艳，不受光照影响）
    private static final int PACKED_LIGHT = 0xF000F0;

    public PlotterBER(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PlotterBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        Component text = be.getText();

        ConsolePanelBER.pushTextTransform(state, poseStack,
                ConsolePanelBER.DEFAULT_TEXT_X, ConsolePanelBER.DEFAULT_TEXT_Y, ConsolePanelBER.DEFAULT_TEXT_Z);

        // 渲染文字
        if (text != null && !text.getString().isEmpty()) {
            Font font = Minecraft.getInstance().font;
            float width = font.width(text);
            font.drawInBatch(text, -width / 2.0F, 0, 0xFFFFFF, false,
                    poseStack.last().pose(), bufferSource, Font.DisplayMode.POLYGON_OFFSET,
                    0x0F0000, packedLight);
        }

        // 渲染趋势图
        renderPlot(be.getData(), poseStack, bufferSource);

        poseStack.popPose();
    }

    private void renderPlot(int[] data, PoseStack poseStack, MultiBufferSource bufferSource) {
        Font font = Minecraft.getInstance().font;
        Component block = Component.literal("\u2588");  // █ FULL BLOCK 字符

        poseStack.pushPose();
        poseStack.scale(SCALE, SCALE, SCALE);  // 缩放让字符变为小方块

        for (int i = 0; i < data.length; i++) {
            int value = Math.max(0, Math.min(15, data[i]));
            if (value <= 0) continue;  // 跳过 0 值

            // 坐标补偿：原坐标 * (1/scale) = * INV_SCALE
            float fx = (START_X - i * STEP_PIXEL) * INV_SCALE;       // 玩家视角向左偏移
            float fy = (START_Y - value * VALUE_PIXEL) * INV_SCALE;  // 玩家视角向上偏移

            // NORMAL 模式：不透明渲染，颜色鲜艳
            // drawInBatch(text, x, y, color, dropShadow, matrix, buffer, mode, backgroundColor, packedLight)
            // backgroundColor = 0（透明背景，不影响正文字色）；packedLight = 全亮，避免文字变黑
            font.drawInBatch(block, fx, fy, COLOR_ARGB, false,
                    poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL,
                    0, PACKED_LIGHT);
        }

        poseStack.popPose();
    }
}
