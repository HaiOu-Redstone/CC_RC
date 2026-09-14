package com.cc_rc.entity;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * 错误生物模型基类。
 *
 * 三个变种（error / null / warn）模型都只是"立体字牌"——无动画部件，
 * 仅负责整块字牌的朝向（跟随实体 yaw 旋转）。基类承载：
 *  - root 模型部件持有（构造注入）
 *  - setupAnim：字牌随实体 yaw 转身（netHeadYaw 旋转）
 *
 * 各自的 LayerDefinition（不同字块坐标）在子类实现（ErrorMobModel /
 * ErrorMobNullModel / ErrorMobWarnModel），渲染器按实体类型选择对应模型层。
 */
public abstract class ErrorMobModelBase extends HierarchicalModel<ErrorMob> {

    private final ModelPart root;

    public ErrorMobModelBase(ModelPart root) {
        this.root = root;
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(ErrorMob entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // 静态字牌模型：无活动部件，无需绑定动画；整体朝向随实体 yaw 转身
        this.root().yRot = netHeadYaw * 0.017453292F;
    }
}