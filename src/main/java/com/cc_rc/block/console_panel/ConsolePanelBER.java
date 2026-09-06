package com.cc_rc.block.console_panel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

/**
 * 控制面板方块实体渲染器
 * 包含静态方法 renderText，供所有方块类（拉杆、指示灯、仪表、控制面板）共用
 * 每个方块类可通过 textX/textY/textZ 参数调整文字位置
 */
public class ConsolePanelBER implements BlockEntityRenderer<ConsolePanelBlockEntity> {
    /** 默认文字位置 */
    public static final float DEFAULT_TEXT_X = 0.0F;
    public static final float DEFAULT_TEXT_Y = 0.3F;
    public static final float DEFAULT_TEXT_Z = 0.35F;

    public ConsolePanelBER(BlockEntityRendererProvider.Context context) {
    }

    /**
     * 应用文字坐标系变换（translate + rotate + scale），供 BER 在同一坐标系内渲染额外内容
     * 调用后需自行 popPose
     */
    public static void pushTextTransform(BlockState state, PoseStack poseStack,
                                         float textX, float textY, float textZ) {
        Direction facing = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING);
        AttachFace face = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        if (face == AttachFace.WALL) {
            float angle = -facing.getOpposite().toYRot();
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            poseStack.translate(textX, textY, textZ);
        } else if (face == AttachFace.CEILING) {
            float angle = -facing.toYRot();
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            poseStack.translate(textX, textY, textZ);
        } else { // FLOOR
            float angle = -(facing.getOpposite().toYRot() + 180) % 360;
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            poseStack.translate(textX, textY, textZ);
        }

        poseStack.scale(-0.015F, -0.015F, 0.015F);
    }

    /**
     * 通用文字渲染方法，供所有 BER 调用（默认字号 0.015、不纵向居中，保持原行为）
     * @param state       方块状态（需含 FACING 和 FACE 属性）
     * @param text        要显示的文本
     * @param poseStack   PoseStack
     * @param bufferSource 缓冲源
     * @param packedLight 光照
     * @param textX       文字 X 偏移（默认 0）
     * @param textY       文字 Y 偏移（默认 0.3）
     * @param textZ       文字 Z 偏移（默认 0.35）
     */
    public static void renderText(BlockState state, Component text, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int packedLight,
                                  float textX, float textY, float textZ) {
        renderText(state, text, poseStack, bufferSource, packedLight,
                textX, textY, textZ, 0.015F, false);
    }

    /**
     * 通用文字渲染方法（可指定字号与纵向居中）
     * @param state        方块状态（需含 FACING 和 FACE 属性）
     * @param text         要显示的文本
     * @param poseStack    PoseStack
     * @param bufferSource 缓冲源
     * @param packedLight  光照
     * @param textX        文字 X 偏移
     * @param textY        文字 Y 偏移
     * @param textZ        文字 Z 偏移
     * @param scale        文字缩放系数（字号倍率，基础 0.015）
     * @param centerY      是否在 y 方向纵向居中（绕文字锚点居中）
     */
    public static void renderText(BlockState state, Component text, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int packedLight,
                                  float textX, float textY, float textZ,
                                  float scale, boolean centerY) {
        if (text == null || text.getString().isEmpty()) return;

        Direction facing = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING);
        AttachFace face = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        if (face == AttachFace.WALL) {
            float angle = -facing.getOpposite().toYRot();
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            poseStack.translate(textX, textY, textZ);
        } else if (face == AttachFace.CEILING) {
            float angle = -facing.toYRot();
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            poseStack.translate(textX, textY, textZ);
        } else { // FLOOR
            float angle = -(facing.getOpposite().toYRot() + 180) % 360;
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            poseStack.translate(textX, textY, textZ);
        }

        poseStack.scale(-scale, -scale, scale);

        Font font = Minecraft.getInstance().font;
        float width = font.width(text);
        float y = centerY ? -font.lineHeight / 2.0F : 0;
        font.drawInBatch(text, -width / 2.0F, y, 0xFFFFFF, false,
                poseStack.last().pose(), bufferSource, Font.DisplayMode.POLYGON_OFFSET,
                0x0F0000, packedLight);

        poseStack.popPose();
    }

    /** 使用默认文字位置渲染 */
    public static void renderText(BlockState state, Component text, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int packedLight) {
        renderText(state, text, poseStack, bufferSource, packedLight,
                DEFAULT_TEXT_X, DEFAULT_TEXT_Y, DEFAULT_TEXT_Z);
    }

    @Override
    public void render(ConsolePanelBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // 大号控制面板：字号为普通面板的 3 倍，且文字在 x、y 方向居中
        if (be.getBlockState().getBlock() instanceof ConsolePanelLargeBlock) {
            renderText(be.getBlockState(), be.getText(), poseStack, bufferSource, packedLight,
                    0.0F, 0.0F, DEFAULT_TEXT_Z, 0.045F, true);
        } else {
            renderText(be.getBlockState(), be.getText(), poseStack, bufferSource, packedLight);
        }
    }
}
