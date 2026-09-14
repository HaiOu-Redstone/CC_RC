package com.cc_rc.entity;

import com.cc_rc.CcRc;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * 盖金蜗牛（gajin）模型——由「模型/生物/蜗牛/gaijin.bbmodel」转换。
 *
 * 坐标语义（关键）：**Minecraft 实体模型像素坐标中 y=24 对应脚底（贴地），
 * y 越小越高**（LivingEntityRenderer 渲染时 scale(-1,-1,1) + translate(0,-1.501,0)
 * 组合后 y_world = 1.501 - y_px/16）。bbmodel 是 java_block 格式（Y 轴向上），
 * 转换时已将「bb_y 越小=越低」翻转为「实体 y 越大=越低」。
 *
 * 4 个 box（16px=1 方块，模型整体约 0.5×0.5 方块）：
 *  - body  身体：宽 8(x-4..4)、高 3(y 21..24，底贴地)、长 12(z-6..6)
 *  - shell 外壳：宽 6(x-3..3)、高 6(y 15..21，body 之上)、长 8(z-2..6，偏后)
 *  - tentR 右触角：宽 2(x 1..3)、高 4(y 17..21)、长 1(z-7..-6，前上方)
 *  - tentL 左触角：宽 2(x-3..-1)、高 4(y 17..21)、长 1(z-7..-6)
 *
 * 贴图 textures/entity/gajin.png（64×64）为重排后的 Mojang 标准布局：
 *  - shell texOffs(0,0)；tentR texOffs(28,0)；tentL texOffs(34,0)；
 *    body texOffs(0,16)。
 *
 * 蜗牛行走缓慢无腿部动画，仅随实体朝向转身（由 setupAnim 设置 yRot）。
 */
public class GajinModel extends HierarchicalModel<GajinSnail> {

    /** 模型层位置：盖金蜗牛 */
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(CcRc.MODID, "gajin"), "main");

    private final ModelPart root;

    public GajinModel(ModelPart root) {
        this.root = root;
    }

    @Override
    public ModelPart root() {
        return root;
    }

    /** 构造模型定义：4 个 box（坐标语义见类注释，y 越大越靠下）。 */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition part = mesh.getRoot();

        // 身体：脚底贴地的 8×3×12 长条（x[-4,4] y[21,24] z[-6,6]）
        part.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, 21.0F, -6.0F, 8.0F, 3.0F, 12.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // 外壳：身体上方偏后的 6×6×8 大壳（x[-3,3] y[15,21] z[-2,6]）
        part.addOrReplaceChild("shell",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, 15.0F, -2.0F, 6.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // 右触角：前方右侧 2×4×1（x[1,3] y[17,21] z[-7,-6]）
        part.addOrReplaceChild("tentR",
                CubeListBuilder.create().texOffs(28, 0).addBox(1.0F, 17.0F, -7.0F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // 左触角：前方左侧 2×4×1（x[-3,-1] y[17,21] z[-7,-6]）
        part.addOrReplaceChild("tentL",
                CubeListBuilder.create().texOffs(34, 0).addBox(-3.0F, 17.0F, -7.0F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    /**
     * 动画：蜗牛无腿部，仅随实体朝向转身（netHeadYaw 已含实体 yaw），
     * 静止时保持默认姿态；摇头/摆尾动画省略。
     */
    @Override
    public void setupAnim(GajinSnail entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // 随实体水平朝向转身（弧度 = 角度 * PI/180，0.017453292F 即 DEG_TO_RAD）
        this.root().yRot = netHeadYaw * 0.017453292F;
    }
}