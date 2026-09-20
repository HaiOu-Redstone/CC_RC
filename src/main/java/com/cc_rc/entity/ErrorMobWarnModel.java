package com.cc_rc.entity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import com.cc_rc.CcRc;

/**
 * 错误生物变种模型：WARN 像素字牌（warn）。
 *
 * 新模型来源「模型/生物/错误生物/新模型/WARN.zip」（Blockbench 导出 json，17 个元素），
 * 由转换脚本离线生成 Java 几何：2px 宽薄板按像素字形拼出 "WARN"，z 厚 2px；
 * 两个斜腿为独立 part 绕各自 origin 旋转 -22.5°（z 轴）。
 * 模型以脚底中心为原点并居中（x 居中、z 中心化），整体宽约 46/16 方块、高 20/16 方块。
 * 贴图 32x32（源 texture.png）。
 */
public class ErrorMobWarnModel extends ErrorMobModelBase {

    /** 模型层位置（独立于主 error_mob，供渲染器按实体类型 bake） */
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(CcRc.MODID, "error_mob_warn"), "main");

    public ErrorMobWarnModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition part = mesh.getRoot();

        CubeListBuilder main = CubeListBuilder.create()
                .addBox(13f, 2f, -1f, 2f, 18f, 2f, new CubeDeformation(0.0F))
                .addBox(21f, 2f, -1f, 2f, 18f, 2f, new CubeDeformation(0.0F))
                .addBox(17f, 2f, -1f, 2f, 17f, 2f, new CubeDeformation(0.0F))
                .addBox(14f, 0f, -1f, 3f, 2f, 2f, new CubeDeformation(0.0F))
                .addBox(19f, 0f, -1f, 3f, 2f, 2f, new CubeDeformation(0.0F))
                .addBox(9f, 0f, -1f, 2f, 18f, 2f, new CubeDeformation(0.0F))
                .addBox(1f, 0f, -1f, 2f, 18f, 2f, new CubeDeformation(0.0F))
                .addBox(3f, 18f, -1f, 6f, 2f, 2f, new CubeDeformation(0.0F))
                .addBox(3f, 9f, -1f, 6f, 2f, 2f, new CubeDeformation(0.0F))
                .addBox(-3f, 0f, -1f, 2f, 20f, 2f, new CubeDeformation(0.0F))
                .addBox(-11f, 11f, -1f, 2f, 7f, 2f, new CubeDeformation(0.0F))
                .addBox(-9f, 18f, -1f, 6f, 2f, 2f, new CubeDeformation(0.0F))
                .addBox(-9f, 9f, -1f, 6f, 2f, 2f, new CubeDeformation(0.0F))
                .addBox(-15f, 0f, -1f, 2f, 20f, 2f, new CubeDeformation(0.0F))
                .addBox(-23f, 0f, -1f, 2f, 20f, 2f, new CubeDeformation(0.0F))
        ;
        part.addOrReplaceChild("letters", main, PartPose.ZERO);
        part.addOrReplaceChild("rot0", CubeListBuilder.create()
                .addBox(-1f, -6.5f, -1f, 2f, 10f, 2f, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-9f, 6.5f, 0f, 0.0F, 0.0F, -0.39270f)); // 绕(-2,6.5,8) 轴 z 旋转 -22.5°
        part.addOrReplaceChild("rot1", CubeListBuilder.create()
                .addBox(-1f, -10f, -1f, 2f, 20f, 2f, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-18f, 10f, 0f, 0.0F, 0.0F, -0.39270f)); // 绕(-11,10,8) 轴 z 旋转 -22.5°

        // 贴图 32x32
        return LayerDefinition.create(mesh, 32, 32);
    }
}