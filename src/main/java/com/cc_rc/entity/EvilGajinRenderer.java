package com.cc_rc.entity;

import com.cc_rc.CcRc;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;

/**
 * 邪恶盖金（evil_gajin）渲染器：**直接加载 Blockbench JSON 模型渲染**（生物无动画，无需 EntityModel/部件）。
 *
 * 模型资源：assets/cc_rc/models/entity/evil_gajin.json（gaijin_t58.zip 解包原样使用）+ 贴图
 * assets/cc_rc/textures/entity/evil_gajin/evil_gajin.png。渲染由 BlockbenchJsonModel 逐元素
 * 逐面绘制（每面独立 UV + 旋转元素绕 origin 旋转），无模型转换、无动画帧。
 *
 * 朝向：实体的 yRot（yaw）即模型朝向（Blockbench 前向 +z，与 MC yaw=0 的 +z 方向一致），
 * render 内叠加 -yRot 旋转即可正面对向移动方向。
 */
public class EvilGajinRenderer extends EntityRenderer<EvilGajin> {

    private static final ResourceLocation MODEL =
            new ResourceLocation(CcRc.MODID, "models/entity/evil_gajin.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(CcRc.MODID, "textures/entity/evil_gajin/evil_gajin.png");

    private final BlockbenchJsonModel model;

    public EvilGajinRenderer(EntityRendererProvider.Context context) {
        super(context);
        try {
            // 渲染线程读取资源包中的 Blockbench json（一次性构建几何数据）
            this.model = BlockbenchJsonModel.load(Minecraft.getInstance().getResourceManager(), MODEL);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Blockbench model " + MODEL, e);
        }
    }

    @Override
    public void render(EvilGajin entity, float entityYaw, float partialTick,
                       PoseStack pose, MultiBufferSource source, int packedLight) {
        super.render(entity, entityYaw, partialTick, pose, source, packedLight);
        pose.pushPose();
        // 模型整体放大 1.5 倍（以原点为中心：先于居中平移作用、后于朝向旋转）
        pose.scale(1.5F, 1.5F, 1.5F);
        // 实体朝向：yaw 旋转（模型前向 +z 与 yaw=0 的 +z 一致，直接应用 -yRot）
        pose.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        // 透明镂空渲染（cutout，α<0.5 像素丢弃；无背面剔除，双面可见）
        VertexConsumer buffer = source.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.model.render(pose, buffer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EvilGajin entity) {
        return TEXTURE;
    }
}