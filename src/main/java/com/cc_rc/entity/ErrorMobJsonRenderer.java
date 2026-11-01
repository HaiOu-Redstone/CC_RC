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
 * 错误生物渲染器（支持三个变种：ERROR / NULL / WARN）：**直接加载 Blockbench JSON 模型渲染**。
 *
 * 与邪恶盖金（EvilGajinRenderer）同一方案：不再把 Blockbench 几何转成 Java ModelPart，
 * 而是用 {@link BlockbenchJsonModel} 运行时解析 json 逐元素逐面绘制（每面独立 UV +
 * 旋转元素绕 origin 旋转），无模型转换、无动画帧。错误生物是静态字牌实体，无部件动画。
 *
 * 三个变种共用本渲染器类，由实体注册侧传入各自的模型 json / 贴图 / 缩放因子：
 *  - ERROR（主变种）：assets/cc_rc/models/entity/error_mob.json + error_mob.png，缩放 0.45
 *  - NULL：assets/cc_rc/models/entity/error_mob_null.json + error_mob_null.png，缩放 0.75
 *  - WARN：assets/cc_rc/models/entity/error_mob_warn.json + error_mob_warn.png，缩放 0.45
 *
 * 朝向：实体的 yRot（yaw）即模型朝向（Blockbench 前向 +z，与 MC yaw=0 的 +z 方向一致），
 * render 内叠加 -yRot 旋转即可正面对向移动方向；模型整体放大使用注册侧 scaleFactor。
 */
public class ErrorMobJsonRenderer extends EntityRenderer<ErrorMob> {

    private final ResourceLocation texture;
    private final float scaleFactor;
    private final BlockbenchJsonModel model;

    public ErrorMobJsonRenderer(EntityRendererProvider.Context context,
                                String modelJsonPath, String texturePath, float scaleFactor) {
        super(context);
        this.texture = new ResourceLocation(CcRc.MODID, texturePath);
        this.scaleFactor = scaleFactor;
        try {
            // 渲染线程读取资源包中的 Blockbench json（一次性构建几何数据）
            this.model = BlockbenchJsonModel.load(Minecraft.getInstance().getResourceManager(),
                    new ResourceLocation(CcRc.MODID, modelJsonPath));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Blockbench model " + modelJsonPath, e);
        }
    }

    @Override
    public void render(ErrorMob entity, float entityYaw, float partialTick,
                       PoseStack pose, MultiBufferSource source, int packedLight) {
        super.render(entity, entityYaw, partialTick, pose, source, packedLight);
        pose.pushPose();
        // 模型休整：按变种缩放（以原点为中心），实体朝向沿 yaw 旋转（模型前向 +z 与 yaw=0 的 +z 一致）
        pose.scale(scaleFactor, scaleFactor, scaleFactor);
        pose.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        // 透明镂空渲染（cutout，α<0.5 像素丢弃；无背面剔除，双面可见）
        VertexConsumer buffer = source.getBuffer(RenderType.entityCutoutNoCull(texture));
        this.model.render(pose, buffer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(ErrorMob entity) {
        return texture;
    }
}