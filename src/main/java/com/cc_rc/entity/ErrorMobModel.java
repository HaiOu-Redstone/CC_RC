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
 * 错误生物模型（立体化 ERROR 字牌）。
 *
 * 由「模型/错误生物/error.obj」的 5 个扁平字块立体化而来：原模型 z 厚度仅 0.05
 * 方块，此处加厚为 0.35 方块（约 5.6 像素），使平贴字牌变成有体积的立体字块，
 * 横向排列组成 "ERROR" 字样；模型以脚底中心为原点，整体宽约 2.1 方块、高约 1.04 方块。
 *
 * 5 个字块（x/y/z 单位为像素，16px=1 方块）：
 *  - E：x[-16.54, -12.19] 宽 4.35   y[0.19, 16.43] 高 16.24  z[-2.8, 2.8] 厚 5.6
 *  - R：x[-10.74,  -5.45] 宽 5.29   y[0.19, 16.43] 高 16.24
 *  - R：x[ -4.09,   1.20] 宽 5.29   y[0.19, 16.43]
 *  - O：x[  2.13,   9.66] 宽 7.53   y[0.00, 16.62] 高 16.62
 *  - R：x[ 11.25,  16.54] 宽 5.29   y[0.19, 16.43]
 *
 * 模型只定义一组静态字牌（无动画部件），渲染时随实体移动/旋转，攻击动画
 * 不需要腿臂（字牌悬空浮动，向敌人倾斜由实体朝向实现）。
 */
public class ErrorMobModel extends ErrorMobModelBase {

    /** 模型层位置：ERROR 字牌（主变种） */
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(CcRc.MODID, "error_mob"), "main");

    public ErrorMobModel(ModelPart root) {
        super(root);
    }

    /** 构造模型定义（5 个立体 ERROR 字块）。 */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition part = mesh.getRoot();

        // 5 个字块（对应 error.obj 的 object_1~object_5，坐标换算见类注释）
        // UV 修复：vanilla 盒自动 UV 中，正面（+z/SOUTH 面）采样区为
        //   U∈[u+sz+sx+sz, u+sz+sx+sz+sx]、V∈[v+sz, v+sz+sy]
        // 此前五块共用默认 texOffs(0,0)，正面全部叠加采样纹理角落同一区域导致文字错乱。
        // 现按贴图上五个字母（左下角 x=2/15/28/41/54，y=18，字高 y∈12..18）逐块设置
        //   texOffs：u = 字母x − sz − sx − sz，v 保持 0（y 方向正确，不动垂直采样）
        CubeListBuilder letters = CubeListBuilder.create()
                .texOffs(-14, 0).addBox(-16.54f, 0.19f, -2.80f, 4.35f, 16.24f, 5.60f, new CubeDeformation(0.0F)) // E（采样 x≈2..6）
                .texOffs(-1, 0).addBox(-10.74f, 0.19f, -2.80f, 5.29f, 16.24f, 5.60f, new CubeDeformation(0.0F))  // R（采样 x≈15..19）
                .texOffs(12, 0).addBox(-4.09f, 0.19f, -2.80f, 5.29f, 16.24f, 5.60f, new CubeDeformation(0.0F))   // R（采样 x≈28..32）
                .texOffs(22, 0).addBox(2.13f, 0.00f, -2.80f, 7.53f, 16.62f, 5.60f, new CubeDeformation(0.0F))    // O（采样 x≈41..45）
                .texOffs(38, 0).addBox(11.25f, 0.19f, -2.80f, 5.29f, 16.24f, 5.60f, new CubeDeformation(0.0F));  // R（采样 x≈54..58）
        part.addOrReplaceChild("letters", letters, PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }
}