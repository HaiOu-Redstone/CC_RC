package com.cc_rc.block.Console_lever;

import com.cc_rc.block.console_panel.ConsolePanelBER;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * 控制台拉杆方块实体渲染器
 * 调用 ConsolePanelBER.renderText 静态方法渲染文字
 */
public class ConsoleLeverBER implements BlockEntityRenderer<ConsoleLeverBlockEntity> {
    public ConsoleLeverBER(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ConsoleLeverBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ConsolePanelBER.renderText(be.getBlockState(), be.getText(), poseStack, bufferSource, packedLight);
    }
}
