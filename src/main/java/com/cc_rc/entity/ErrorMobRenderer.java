package com.cc_rc.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 错误生物渲染器（支持三个变种：ERROR / NULL / WARN）。
 *
 * 渲染器本身与字牌内容无关：构造时通过 {@link ModelCreator} 注入指定变种的
 * 模型实例（注册侧 lambda 用 context.bakeLayer(对应层) 构造），并指定贴图与
 * 缩放因子。三个变种共用本渲染器类，由实体注册侧的工厂 lambda 区分。
 *
 * 字牌模型坐标横向排列（ERROR 宽约 2.1 方块 / NULL 宽约 0.8 / WARN 宽约 2.0），
 * 渲染时按变种缩放（scaleFactor）使其与各自碰撞箱相称；实体朝向往 yaw 由
 * 模型基类（ErrorMobModelBase.setupAnim）处理。
 */
public class ErrorMobRenderer extends MobRenderer<ErrorMob, ErrorMobModelBase> {

    /** 模型工厂：在实体注册侧用 EntityRendererProvider.Context bake 对应变种模型层 */
    @FunctionalInterface
    public interface ModelCreator {
        ErrorMobModelBase create(EntityRendererProvider.Context context);
    }

    private final ResourceLocation texture;
    private final float scaleFactor;

    public ErrorMobRenderer(EntityRendererProvider.Context context, ModelCreator modelCreator,
                            ResourceLocation texture, float scaleFactor) {
        super(context, modelCreator.create(context), 0.5F);
        this.texture = texture;
        this.scaleFactor = scaleFactor;
    }

    @Override
    public ResourceLocation getTextureLocation(ErrorMob entity) {
        return texture;
    }

    @Override
    protected void scale(ErrorMob entity, PoseStack poseStack, float partialTick) {
        // 补偿原版 LivingEntityRenderer 的 pose.scale(-1,-1,1)（绕 Z 180°，模型 y 向下约定）：
        // 新模型几何是 Blockbench 直译（Y 向上、底部 y=0），这里再乘 -1 抵消翻转，使字牌
        // 不再上下/左右颠倒；并反向补偿原版 translate(0,-1.501,0)，让模型底部贴地显示。
        poseStack.scale(-scaleFactor, -scaleFactor, scaleFactor);
        poseStack.translate(0.0F, 1.501F, 0.0F);
    }
}