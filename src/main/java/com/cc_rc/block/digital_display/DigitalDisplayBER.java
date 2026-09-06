package com.cc_rc.block.digital_display;

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
 * 数码显示器方块实体渲染器
 * 在屏幕面上渲染两行文字：
 *   - 第一行：命名文字（白色）
 *   - 第二行：固定状态文字（橙色 0xf06020，默认 "----"）
 *
 * 微调方法（修改下方常量后重新编译即可）：
 *   - SCALE     ：整体文字缩放。默认 0.015（接近 1 像素/格的比例，8px 字符约 0.12 格高）
 *   - TEXT_Z    ：文字所在屏幕面的 Z（块中心为 0.5，屏幕面凸出约 0.19，故默认 0.70）
 *   - LINE1_Y   ：第一行（白字）相对块中心的上下偏移，正数向上
 *   - LINE2_Y   ：第二行（橙字）相对块中心的上下偏移，正数向上
 */
public class DigitalDisplayBER implements BlockEntityRenderer<DigitalDisplayBlockEntity> {

    // ====== 尺寸 / 位置 / 颜色 可调参数区 ======
    private static final float SCALE  = 0.015F;
    private static final float TEXT_Z = -0.57F;
    // 第一行（白字）在下方屏幕区；第二行（橙字）在上方屏幕区
    private static final float LINE1_Y = -0.2F;
    private static final float LINE2_Y = 0.25F;

    private static final int COLOR_WHITE   = 0xFFFFFFFF; // AARRGGBB，纯白不透明
    private static final int COLOR_STATUS  = 0xFFF06020;  // 0xAARRGGBB，与 0xf06020 对应（不透明橙色）
    private static final int PACKED_LIGHT  = 0xF000F0;    // 全亮光照（不受环境光照影响）
    // ============================================

    public DigitalDisplayBER(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DigitalDisplayBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        Direction facing = state.getValue(DigitalDisplayBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        float angle = -facing.toYRot();
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));

        Font font = Minecraft.getInstance().font;

        // 第一行：命名文字（白色）
        Component line1 = be.getText();
        if (line1 != null && !line1.getString().isEmpty()) {
            drawLine(poseStack, font, line1, LINE1_Y, COLOR_WHITE, bufferSource);
        }

        // 第二行：固定状态文字（橙色）
        Component line2 = be.getStatus();
        if (line2 != null && !line2.getString().isEmpty()) {
            drawLine(poseStack, font, line2, LINE2_Y, COLOR_STATUS, bufferSource);
        }

        poseStack.popPose();
    }

    private void drawLine(PoseStack poseStack, Font font, Component text, float y,
                          int color, MultiBufferSource bufferSource) {
        poseStack.pushPose();
        poseStack.translate(0, y, TEXT_Z);
        poseStack.scale(-SCALE, -SCALE, SCALE);

        float width = font.width(text);
        // drawInBatch(text, x, y, color, dropShadow, matrix, buffer, mode, backgroundColor, packedLight)
        // backgroundColor = 0（透明背景，不影响正文字色）；packedLight = 全亮，避免文字由于无光照变黑
        font.drawInBatch(text, -width / 2.0F, 0, color, false,
                poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL,
                0, PACKED_LIGHT);

        poseStack.popPose();
    }
}