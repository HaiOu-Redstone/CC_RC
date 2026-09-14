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
 * 错误生物变种模型：WARN 字牌（warn）。
 *
 * 由「模型/错误生物/WARN/warn.obj」立体化而来：原模型为扁平 "WARN" 字样
 * （4 个字母部件横排），此处 z 厚度加厚为 0.35 方块（5.6 像素）成立体字牌，
 * 以脚底中心为原点，整体宽约 2.0 方块、高约 0.5 方块。
 *
 * 4 部件（x/y/z 单位为像素）：
 *  - object_1：W（宽 6.32）
 *  - object_2：A（宽 5.29）
 *  - object_3：R（宽 7.06）
 *  - object_4：N（宽 10.15）
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
        // UV 修复：与主变种同根因——各块共用 texOffs(0,0) 会叠加采样纹理角落同一区域致文字错乱。
        // 贴图上 "WARN" 四字母左下角 x=8/21/34/47、y=18（字高 y∈12..18），按模型自左至右
        // （object_1 W → object_2 A → object_3 R → object_4 N）逐块映射：
        //   u = 字母x − sz − sx − sz；v = 12 − sz − (sy−6)/2（把字母行 [12,18] 置于面内居中，
        //   使 7px 字母尽量铺满 7.95px 高的正面采样窗，避免整行落在面外）
        part.addOrReplaceChild("letters", CubeListBuilder.create()
                .texOffs(-10, 5).addBox(-15.81f, 0.00f, -2.80f, 6.32f, 7.95f, 5.60f, new CubeDeformation(0.0F)) // object_1 W（采样 x≈8）
                .texOffs(5, 5).addBox(-8.14f, 0.00f, -2.80f, 5.29f, 7.95f, 5.60f, new CubeDeformation(0.0F))   // object_2 A（采样 x≈21）
                .texOffs(16, 5).addBox(-1.73f, 0.00f, -2.80f, 7.06f, 7.95f, 5.60f, new CubeDeformation(0.0F))  // object_3 R（采样 x≈34）
                .texOffs(26, 5).addBox(5.66f, 0.00f, -2.80f, 10.15f, 7.95f, 5.60f, new CubeDeformation(0.0F)), // object_4 N（采样 x≈47）
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // 贴图 64x32
        return LayerDefinition.create(mesh, 64, 32);
    }
}