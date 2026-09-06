package com.cc_rc.block.digital_plotter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 数字圆盘记录仪方块实体渲染器
 * 完整方块（变换同数码显示器），正面屏幕渲染：
 *   - 命名文字（白色，一行，位于绘图区下方）
 *   - 红色趋势线图（50 个点，值 0~100，点大小为圆盘记录仪的一半）
 *
 * 线条渲染方式同圆盘记录仪：使用 Font.drawInBatch 绘制 "█" (FULL BLOCK) 字符，
 * NORMAL 模式不透明渲染。
 *
 * 微调方法（修改下方常量后重新编译即可）：
 *   - PLOT_SCALE    ：点缩放。圆盘记录仪 SCALE=0.25（字符2像素），本机 0.125（字符1像素，一半大小）
 *   - START_X       ：绘图区左右中心（文字像素），50点以该点为中心向两侧各25点分布
 *   - START_Y       ：值0所在位置（文字像素），值越大越向上
 *   - STEP_PIXEL    ：数据点X方向间距（与点大小同为1像素，保证连续）
 *   - VALUE_PIXEL   ：数据值Y方向间距（值0~100共100级）
 *   - PLOT_Y        ：绘图区原点Y位置（块空间，相对块中心）
 *   - NAME_Y        ：命名文字（白）的Y位置（块空间，相对块中心）
 */
public class DigitalPlotterBER implements BlockEntityRenderer<DigitalPlotterBlockEntity> {

    // ====== 文字 / 屏幕参数区 ======
    private static final float SCALE  = 0.015F;  // 文字整体缩放（与数码显示器一致）
    private static final float TEXT_Z = -0.57F;  // 屏幕面Z（相对块中心），命名文字所在平面
    private static final float NAME_Y = 0.36F;  // 命名文字（白）相对块中心的上下偏移，正数向上
    private static final float PLOT_Y = -0.09F;   // 绘图区原点Y（相对块中心，正数向上）
    private static final float PLOT_Z = TEXT_Z + 1.0F / 16.0F;  // 划线平面比文字向中心收 1 个模型像素（1/16 格），前后分层
    // =================================

    // ====== 点缩放（同圆盘记录仪管线） ======
    // 字符原始大小8像素：圆盘记录仪 SCALE=0.25（2像素），本机 0.125（1像素，一半大小）
    private static final float PLOT_SCALE = 0.15F;
    private static final float INV_SCALE  = 1.0F / PLOT_SCALE;  // 坐标补偿系数

    // ====== 绘图区坐标（文字像素） ======
    private static final float START_X = 27.5F;   // 左右中心：50点以该点为中心，向两侧各25点
    private static final float START_Y = 23.0F;   // 值0所在Y（值越大越向上，对应向上偏移）
    private static final float STEP_PIXEL = 1.15F; // 数据点X方向间距（1像素，与点大小一致保证连续）
    private static final float VALUE_PIXEL = 0.32F; // 数据值Y方向间距（0~100共100级，100*0.5px=50px≈0.75格）
    // ====================================

    // ====== 颜色 / 光照 ======
    private static final int COLOR_WHITE  = 0xFFFFFFFF;
    private static final int COLOR_ARGB   = 0xFFFF0000;  // 不透明纯红（同圆盘记录仪）
    private static final int PACKED_LIGHT = 0xF000F0;    // 全亮光照
    // ========================

    public DigitalPlotterBER(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DigitalPlotterBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        Direction facing = state.getValue(DigitalPlotterBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        float angle = -facing.toYRot();
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));

        Font font = Minecraft.getInstance().font;

        // 命名文字（白色，仅一行）
        Component text = be.getText();
        if (text != null && !text.getString().isEmpty()) {
            drawLine(poseStack, font, text, NAME_Y, COLOR_WHITE, bufferSource);
        }

        // 趋势线图（红色）
        drawPlot(be.getData(), poseStack, font, bufferSource);

        poseStack.popPose();
    }

    private void drawLine(PoseStack poseStack, Font font, Component text, float y,
                          int color, MultiBufferSource bufferSource) {
        poseStack.pushPose();
        poseStack.translate(0, y, TEXT_Z);
        poseStack.scale(-SCALE, -SCALE, SCALE);

        float width = font.width(text);
        font.drawInBatch(text, -width / 2.0F, 0, color, false,
                poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL,
                0, PACKED_LIGHT);

        poseStack.popPose();
    }

    private void drawPlot(int[] data, PoseStack poseStack, Font font, MultiBufferSource bufferSource) {
        Component block = Component.literal("\u2588");  // █ FULL BLOCK 字符

        poseStack.pushPose();
        poseStack.translate(0, PLOT_Y, PLOT_Z);
        poseStack.scale(-SCALE, -SCALE, SCALE);
        poseStack.scale(PLOT_SCALE, PLOT_SCALE, PLOT_SCALE);

        for (int i = 0; i < data.length; i++) {
            int value = Math.max(DigitalPlotterBlockEntity.MIN_VALUE,
                    Math.min(DigitalPlotterBlockEntity.MAX_VALUE, data[i]));
            if (value <= 0) continue;  // 跳过 0 值

            // 坐标补偿：原坐标 * (1/PLOT_SCALE) = * INV_SCALE
            float fx = (START_X - i * STEP_PIXEL) * INV_SCALE;
            float fy = (START_Y - value * VALUE_PIXEL) * INV_SCALE;

            // NORMAL 模式：不透明渲染，颜色鲜艳
            font.drawInBatch(block, fx, fy, COLOR_ARGB, false,
                    poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL,
                    0, PACKED_LIGHT);
        }

        poseStack.popPose();
    }
}
