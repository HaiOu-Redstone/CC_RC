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
 * 错误生物变种模型：NULL 像素字牌（null）。
 *
 * 新模型来源「模型/生物/错误生物/新模型/null.zip」（Blockbench 导出 json，10 个元素），
 * 由转换脚本离线生成 Java 几何：2px 宽薄板按像素字形拼出 "NULL"，z 厚 2px；
 * 中部竖线为独立 part 绕 origin 旋转 -22.5°（z 轴）。
 * 模型以脚底中心为原点并居中（x 居中、z 中心化），整体宽约 42/16 方块、高 20/16 方块。
 * 贴图 16x16（源 texture.png）。
 */
public class ErrorMobNullModel extends ErrorMobModelBase {

    /** 模型层位置（独立于主 error_mob，供渲染器按实体类型 bake） */
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(CcRc.MODID, "error_mob_null"), "main");

    public ErrorMobNullModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition part = mesh.getRoot();

        CubeListBuilder main = CubeListBuilder.create()
                .addBox(-1f, 2f, -1f, 2f, 18f, 2f, new CubeDeformation(0.0F))
                .addBox(7f, 2f, -1f, 2f, 18f, 2f, new CubeDeformation(0.0F))
                .addBox(0f, 0f, -1f, 8f, 2f, 2f, new CubeDeformation(0.0F))
                .addBox(-5f, 0f, -1f, 2f, 20f, 2f, new CubeDeformation(0.0F))
                .addBox(-11f, 0f, -1f, 6f, 2f, 2f, new CubeDeformation(0.0F))
                .addBox(19f, 0f, -1f, 2f, 20f, 2f, new CubeDeformation(0.0F))
                .addBox(11f, 0f, -1f, 2f, 20f, 2f, new CubeDeformation(0.0F))
                .addBox(-15f, 0f, -1f, 2f, 20f, 2f, new CubeDeformation(0.0F))
                .addBox(-21f, 0f, -1f, 6f, 2f, 2f, new CubeDeformation(0.0F))
        ;
        part.addOrReplaceChild("letters", main, PartPose.ZERO);
        part.addOrReplaceChild("rot0", CubeListBuilder.create()
                .addBox(-1f, -10f, -1f, 2f, 20f, 2f, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(16f, 10f, 0f, 0.0F, 0.0F, -0.39270f)); // 绕(24,10,8) 轴 z 旋转 -22.5°

        // 贴图 16x16
        return LayerDefinition.create(mesh, 16, 16);
    }
}