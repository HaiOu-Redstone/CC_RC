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
 * 错误生物变种模型：NULL 字牌（null）。
 *
 * 由「模型/错误生物/null/null.obj」立体化而来：原模型为扁平 "null" 字样
 * （2 竖线 + 2 横骨架，4 部件），此处 z 厚度加厚为 0.35 方块（5.6 像素）成
 * 立体字牌，以脚底中心为原点，整体宽约 0.8 方块、高约 0.41 方块。
 *
 * 4 部件（x/y/z 单位为像素）：
 *  - object_1/2：竖线（宽 0.56）
 *  - object_3/4：骨架（高 4.68）
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
        // UV 修复：与主变种同根因——各块共用 texOffs(0,0) 会叠加采样纹理角落同一区域致文字错乱。
        // 贴图上 "null" 四字母左下角 x=8/21/34/47、y=18（字高 y∈12..18），按模型自左至右
        // （object_1..object_4）逐块映射该字槽：
        //   u = 字母x − sz − sx − sz；v = 12 − sz − (sy−6)/2（把字母行 [12,18] 置于面内居中，
        //   因 null 方块高度(4.68/6.55)小于字母宽度纵向带，须上移采样窗避免整行落在面外）
        part.addOrReplaceChild("letters", CubeListBuilder.create()
                .texOffs(-4, 6).addBox(-6.38f, 0.07f, -2.80f, 0.56f, 6.55f, 5.60f, new CubeDeformation(0.0F)) // object_1 → n（采样 x≈8）
                .texOffs(9, 6).addBox(-4.32f, 0.07f, -2.80f, 0.56f, 6.55f, 5.60f, new CubeDeformation(0.0F))  // object_2 → u（采样 x≈21）
                .texOffs(19, 7).addBox(-2.30f, 0.00f, -2.80f, 3.63f, 4.68f, 5.60f, new CubeDeformation(0.0F)) // object_3 → l（采样 x≈34）
                .texOffs(32, 7).addBox(2.71f, 0.07f, -2.80f, 3.67f, 4.68f, 5.60f, new CubeDeformation(0.0F)), // object_4 → l（采样 x≈47）
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // 贴图 64x32
        return LayerDefinition.create(mesh, 64, 32);
    }
}