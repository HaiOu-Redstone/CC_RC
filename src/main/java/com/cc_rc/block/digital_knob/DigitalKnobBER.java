package com.cc_rc.block.digital_knob;

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

import java.util.Locale;

/**
 * 数字调节器方块实体渲染器
 * 在正面屏幕渲染两行文字（与数码显示器一致的显示逻辑）：
 *   - 第一行：命名文字（白色，来自放置时物品的自定义名称，默认空不显示）
 *   - 第二行：当前整数值（橙色，与数码显示器状态色一致）
 *
 * 可调参数：
 *   - SCALE：文字缩放
 *   - TEXT_Z：文字所在屏幕面Z（相对块中心）
 *   - LINE1_Y：第一行（白字）相对块中心的上下偏移，正数向上
 *   - LINE2_Y：第二行（橙字）相对块中心的上下偏移，正数向上
 */
public class DigitalKnobBER implements BlockEntityRenderer<DigitalKnobBlockEntity> {

    // ====== 可调参数区 ======
    private static final float SCALE  = 0.015F;
    private static final float TEXT_Z = -0.57F;
    // 第一行（白字）在下方；第二行（橙字）在上方
    private static final float LINE1_Y = 0.07F;
    private static final float LINE2_Y = 0.36F;

    private static final int COLOR_WHITE  = 0xFFFFFFFF;
    private static final int COLOR_VALUE  = 0xFFF06020; // 与数码显示器状态色一致（橙）
    private static final int PACKED_LIGHT = 0xF000F0;
    // ========================

    public DigitalKnobBER(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DigitalKnobBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        Direction facing = state.getValue(DigitalKnobBlock.FACING);

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

        // 第二行：当前值（橙色）
        // 百分比模式开启时仅改变显示方式：value/10 + 百分号（123 -> "12.3%"，5 -> "0.5%"），实际存储值不变
        Component line2 = be.isPercentMode()
                ? Component.literal(String.format(Locale.ROOT, "%.1f%%", be.getValue() / 10.0))
                : Component.literal(Integer.toString(be.getValue()));
        drawLine(poseStack, font, line2, LINE2_Y, COLOR_VALUE, bufferSource);

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
}
