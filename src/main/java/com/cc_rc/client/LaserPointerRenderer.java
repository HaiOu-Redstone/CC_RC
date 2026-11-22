package com.cc_rc.client;

import com.cc_rc.CcRc;
import com.cc_rc.Config;
import com.cc_rc.ModItems;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * 激光笔渲染器（纯客户端，无网络包）。
 *
 * 行为：
 *  - 对场景内每个手持激光笔的玩家（含本地玩家与其他玩家）绘制一条红色激光；
 *  - 起点取玩家手部附近的近似位置（主手在右、副手在左，左手玩家整体镜像）；
 *  - 终点 = 玩家视线（眼睛出发、沿视线方向）与方块**碰撞箱**表面的交点，最远 {@link Config#getLaserPointerMaxDistance()} 格；
 *  - 该距离内没有命中方块时，终点 = 眼睛 + 视线方向 * 最大距离，即激光与视线平行。
 *
 * 绘制方式：
 *  - 用无贴图的 POSITION_COLOR 四边形（{@link RenderType#debugQuads()}）画 3 片绕光束轴互成 60° 的细长条；
 *    不使用单片朝向相机的四边形，是因为第一人称下光束几乎与视线重合，单片会退化到看不见；
 *  - 顶点坐标统一减去相机位置，深度测试保持开启，激光会被墙体正常遮挡。
 */
@Mod.EventBusSubscriber(modid = CcRc.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LaserPointerRenderer {

    /** 光束横截面的细带数量（互成 180°/3 = 60°）。 */
    private static final int BEAM_SLICES = 3;

    /** 激光颜色（红色，完全不透明）。 */
    private static final int COLOR_R = 255;
    private static final int COLOR_G = 0;
    private static final int COLOR_B = 0;
    private static final int COLOR_A = 255;

    // 手部起点相对眼睛的偏移（格）：沿视线前移、向右/左、向下
    // 站立时眼睛高约 1.62 格、持物手约在 1.0 格高，故向下偏移取 0.62（原先 0.22 会落在肩部）
    private static final double HAND_FORWARD = 0.30D;
    private static final double HAND_SIDE = 0.30D;
    private static final double HAND_DOWN = 0.62D;

    private LaserPointerRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // 在方块渲染完成后绘制，保证被不透明方块正确遮挡
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        Matrix4f mat = event.getPoseStack().last().pose();
        Vec3 camera = event.getCamera().getPosition();
        float partialTick = event.getPartialTick();
        float halfWidth = (float) Config.getLaserPointerLineWidth() * 0.5F;
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        for (Player player : mc.level.players()) {
            boolean mainHand = player.getMainHandItem().is(ModItems.LASER_POINTER.get());
            boolean offHand = player.getOffhandItem().is(ModItems.LASER_POINTER.get());
            if (!mainHand && !offHand) {
                continue;
            }
            // 视线命中点两只手共用（激光都指向准星所指位置）
            Aim aim = computeAim(player, partialTick);
            if (mainHand) {
                drawBeam(buffers, mat, camera, handOrigin(player, aim, true), aim.end(), halfWidth);
            }
            if (offHand) {
                drawBeam(buffers, mat, camera, handOrigin(player, aim, false), aim.end(), halfWidth);
            }
        }
        buffers.endBatch(RenderType.debugQuads());
    }

    /** 一次视线求交的结果：眼睛位置、单位视线方向、激光终点。 */
    private record Aim(Vec3 eye, Vec3 dir, Vec3 end) {
    }

    /**
     * 计算激光终点：从眼睛沿视线方向做碰撞箱射线检测，
     * 命中方块则取命中点（碰撞箱表面），否则取最大距离处的点（与视线平行）。
     */
    private static Aim computeAim(Player player, float partialTick) {
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 dir = player.getViewVector(partialTick).normalize();
        Vec3 far = eye.add(dir.scale(Config.getLaserPointerMaxDistance()));
        // ClipContext.Block.COLLIDER = 按碰撞箱形状求交
        BlockHitResult hit = player.level().clip(new ClipContext(eye, far,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = hit.getType() == HitResult.Type.MISS ? far : hit.getLocation();
        return new Aim(eye, dir, end);
    }

    /** 手部近似起点：视线前方 + 左右侧 + 略向下；右手玩家主手在右侧，左手玩家镜像。 */
    private static Vec3 handOrigin(Player player, Aim aim, boolean mainHand) {
        Vec3 forward = aim.dir();
        // 视线右向量 = 视线 × 世界向上；视线竖直时退化为 +X
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        right = right.normalize();
        Vec3 up = right.cross(forward).normalize();
        boolean onRight = (player.getMainArm() == HumanoidArm.RIGHT) == mainHand;
        double side = onRight ? 1.0D : -1.0D;
        return aim.eye()
                .add(forward.scale(HAND_FORWARD))
                .add(right.scale(HAND_SIDE * side))
                .add(up.scale(-HAND_DOWN));
    }

    /**
     * 绘制一条激光：沿光束轴取 3 个互相垂直方向，各画一条细长四边形。
     * 顶点坐标转换为相对相机的局部坐标（世界渲染阶段相机已平移）。
     */
    private static void drawBeam(MultiBufferSource buffers, Matrix4f mat, Vec3 camera,
                                 Vec3 start, Vec3 end, float halfWidth) {
        Vec3 axis = end.subtract(start);
        double length = axis.length();
        if (length < 1.0E-4D) {
            return;
        }
        Vec3 dir = axis.scale(1.0D / length);
        // 取一个与光束不平行的参考向量，构造横截面正交基
        Vec3 ref = Math.abs(dir.y) > 0.99D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 u = dir.cross(ref).normalize();
        Vec3 v = dir.cross(u).normalize();

        Vec3 localStart = start.subtract(camera);
        Vec3 localEnd = end.subtract(camera);
        VertexConsumer vc = buffers.getBuffer(RenderType.debugQuads());
        for (int i = 0; i < BEAM_SLICES; i++) {
            double angle = Math.PI * i / BEAM_SLICES;
            Vec3 offset = u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle))).scale(halfWidth);
            // 四边形：起点两侧 → 终点两侧（无剔除，正反两面都可见）
            vertex(vc, mat, localStart.add(offset));
            vertex(vc, mat, localStart.subtract(offset));
            vertex(vc, mat, localEnd.subtract(offset));
            vertex(vc, mat, localEnd.add(offset));
        }
    }

    /** 写入一个红色顶点（POSITION_COLOR 格式：位置 + 颜色）。 */
    private static void vertex(VertexConsumer vc, Matrix4f mat, Vec3 pos) {
        vc.vertex(mat, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(COLOR_R, COLOR_G, COLOR_B, COLOR_A)
                .endVertex();
    }
}
