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
 * 错误生物模型（主变种）：ERROR 像素字牌。
 *
 * 新模型来源「模型/生物/错误生物/新模型/error (1).zip」（Blockbench 导出 json，23 个元素），
 * 由转换脚本离线生成 Java 几何：2px 宽薄板按像素字形拼出 "ERROR"，z 厚 2px；
 * 三个 R 斜腿为独立 part 绕各自 origin 旋转 -22.5°（z 轴）。
 * 模型以脚底中心为原点并居中（x 居中、z 中心化），整体宽约 46/16 方块、高 20/16 方块。
 * 贴图 32x32（源 texture.png）。
 */
public class ErrorMobModel extends ErrorMobModelBase {

    /** 模型层位置：ERROR 字牌（主变种） */
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(CcRc.MODID, "error_mob"), "main");

    public ErrorMobModel(ModelPart root) {
        super(root);
    }

    /** 构造模型定义（Blockbench 新模型几何，23 元素：20 直板 + 3 旋转斜腿）。 */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition part = mesh.getRoot();

        CubeListBuilder main = CubeListBuilder.create()
                .addBox(21f, 0f, -1f, 2f, 20f, 2f, new CubeDeformation(0.0F))
                .addBox(15f, 0f, -1f, 6f, 2f, 2f, new CubeDeformation(0.0F))
                .addBox(15f, 18f, -1f, 6f, 2f, 2f, new CubeDeformation(0.0F))
                .addBox(15f, 9f, -1f, 6f, 2f, 2f, new CubeDeformation(0.0F))
                .addBox(12f, 0f, -1f, 2f, 20f, 2f, new CubeDeformation(0.0F))
                .addBox(6f, 11f, -1f, 2f, 7f, 2f, new CubeDeformation(0.0F))
                .addBox(8f, 18f, -1f, 6f, 2f, 2f, new CubeDeformation(0.0F))
                .addBox(8f, 9f, -1f, 6f, 2f, 2f, new CubeDeformation(0.0F))
                .addBox(2f, 0f, -1f, 2f, 20f, 2f, new CubeDeformation(0.0F))
                .addBox(-4f, 11f, -1f, 2f, 7f, 2f, new CubeDeformation(0.0F))
                .addBox(-2f, 18f, -1f, 6f, 2f, 2f, new CubeDeformation(0.0F))
                .addBox(-2f, 9f, -1f, 6f, 2f, 2f, new CubeDeformation(0.0F))
                .addBox(-8f, 2f, -1f, 2f, 16f, 2f, new CubeDeformation(0.0F))
                .addBox(-14f, 2f, -1f, 2f, 16f, 2f, new CubeDeformation(0.0F))
                .addBox(-12f, 0f, -1f, 4f, 2f, 2f, new CubeDeformation(0.0F))
                .addBox(-12f, 18f, -1f, 4f, 2f, 2f, new CubeDeformation(0.0F))
                .addBox(-17f, 0f, -1f, 2f, 20f, 2f, new CubeDeformation(0.0F))
                .addBox(-23f, 11f, -1f, 2f, 7f, 2f, new CubeDeformation(0.0F))
                .addBox(-21f, 18f, -1f, 6f, 2f, 2f, new CubeDeformation(0.0F))
                .addBox(-21f, 9f, -1f, 6f, 2f, 2f, new CubeDeformation(0.0F))
        ;
        part.addOrReplaceChild("letters", main, PartPose.ZERO);
        part.addOrReplaceChild("rot0", CubeListBuilder.create()
                .addBox(-1f, -6.5f, -1f, 2f, 10f, 2f, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(8f, 6.5f, 0f, 0.0F, 0.0F, -0.39270f)); // 绕(17,6.5,8) 轴 z 旋转 -22.5°
        part.addOrReplaceChild("rot1", CubeListBuilder.create()
                .addBox(-1f, -6.5f, -1f, 2f, 10f, 2f, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-2f, 6.5f, 0f, 0.0F, 0.0F, -0.39270f)); // 绕(7,6.5,8) 轴 z 旋转 -22.5°
        part.addOrReplaceChild("rot2", CubeListBuilder.create()
                .addBox(-1f, -6.5f, -1f, 2f, 10f, 2f, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-21f, 6.5f, 0f, 0.0F, 0.0F, -0.39270f)); // 绕(-12,6.5,8) 轴 z 旋转 -22.5°

        // 贴图 32x32
        return LayerDefinition.create(mesh, 32, 32);
    }
}