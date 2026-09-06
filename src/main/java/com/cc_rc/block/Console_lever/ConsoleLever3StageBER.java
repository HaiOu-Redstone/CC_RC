package com.cc_rc.block.Console_lever;

import com.cc_rc.block.console_panel.ConsolePanelBER;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * 3挡位控制台拉杆方块实体渲染器
 * 调用 ConsolePanelBER.renderText 静态方法渲染文字
 * 用于 console_lever_6 和 console_lever_7
 */
public class ConsoleLever3StageBER implements BlockEntityRenderer<ConsoleLever3StageBlockEntity> {
    public ConsoleLever3StageBER(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ConsoleLever3StageBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ConsolePanelBER.renderText(be.getBlockState(), be.getText(), poseStack, bufferSource, packedLight);
    }
}
