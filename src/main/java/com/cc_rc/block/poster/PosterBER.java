package com.cc_rc.block.poster;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.cc_rc.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/**
 * 海报方块实体渲染器：按方块实体参数直接绘制一张贴图四边形。
 *
 * 走方块实体渲染而不是原版方块模型，原因：原版方块模型的元素坐标被硬限制在每轴 [-16, 32]
 * （像素），超过即解析失败变紫黑块；海报需要 2 倍宽、九格宽、向下超出格子等大尺寸，
 * 只能用自定义渲染。
 *
 * 几何约定（局部坐标，单位「格」）：
 *  - 以「观者视角」为基准：锚点 = 方块贴墙面的左上角（离墙 1 像素），图片自锚点向右、向下延伸；
 *    u=0/v=0 对应图片左上角，因此无论朝向如何，图片都不会左右镜像；
 *  - 宽 = 缩放倍率（1.0 = 1 格）；高 = 宽 * 内容高 / 原图宽（保持原始比例）；
 *    贴图 v 从内容顶部（跳过画布顶部透明留白）采样，因此默认「图片顶端对齐方块顶端」；
 *  - dz 沿「观者向右」偏移，dy 沿竖直向上偏移；单位均为「格」（可精确到 0.5 格）；
 *  - 手持海报物品时额外在方块贴墙面绘制半透明物品图标，标记透明方块本体的位置。
 *
 * 四个朝向的锚点与「观者向右」单位向量：
 *  - FACING=north（墙在 z+ 侧，面朝北，观者自北向南看）：锚点 (1.0, 1.0, 15/16)，向右 = -X
 *  - FACING=south（墙在 z- 侧）：锚点 (0.0, 1.0, 1/16)，向右 = +X
 *  - FACING=east （墙在 x- 侧）：锚点 ( 1/16, 1.0, 1.0)，向右 = -Z
 *  - FACING=west （墙在 x+ 侧）：锚点 (15/16, 1.0, 0.0)，向右 = +Z
 *  （与 PosterBlock 的碰撞箱、原版四个方向面的 UV 约定一致）
 */
public class PosterBER implements BlockEntityRenderer<PosterBlockEntity> {

    /** 贴图离墙面的距离（格）：1 像素，避免与墙方块表面 z-fighting。 */
    private static final float WALL_GAP = 1.0F / 16.0F;

    /** 幽灵标记用的贴图：海报物品图标（手持海报时用来标记透明方块本体的位置）。 */
    private static final net.minecraft.resources.ResourceLocation GHOST_TEXTURE =
            new net.minecraft.resources.ResourceLocation("cc_rc", "textures/item/poster.png");

    public PosterBER(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PosterBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof PosterBlock)) {
            return;
        }
        Direction facing = state.getValue(PosterBlock.FACING);
        PosterImages.Image image = PosterImages.get(be.getImage());

        // 图片尺寸（格）：宽 = 缩放倍率，高按原图比例
        float width = be.getScale();
        float height = width * image.aspect();
        // 偏移量（单位：格）
        float dz = be.getDz();
        float dy = be.getDy();

        // 观者视角锚点（左上角）与「观者向右」单位向量（rx/rz；竖直方向恒为 +Y）
        float ax;
        float az;
        float rx;
        float rz;
        switch (facing) {
            case NORTH -> {
                ax = 1.0F;
                az = 1.0F - WALL_GAP;   // 15/16
                rx = -1.0F;
                rz = 0.0F;
            }
            case SOUTH -> {
                ax = 0.0F;
                az = WALL_GAP;          // 1/16
                rx = 1.0F;
                rz = 0.0F;
            }
            case EAST -> {
                ax = WALL_GAP;          // 1/16
                az = 1.0F;
                rx = 0.0F;
                rz = -1.0F;
            }
            default -> {                // WEST
                ax = 1.0F - WALL_GAP;   // 15/16
                az = 0.0F;
                rx = 0.0F;
                rz = 1.0F;
            }
        }
        // 水平偏移沿「观者向右」，垂直偏移沿 +Y（正向为上）
        // 记录未偏移的锚点（幽灵标记用，标记的是方块本体而不是图片）
        float baseAx = ax;
        float baseAz = az;
        ax += rx * dz;
        az += rz * dz;
        float top = 1.0F + dy;
        float bottom = top - height;
        float rightX = ax + rx * width;
        float rightZ = az + rz * width;

        // 面法线 = 向右向量 × 向上向量 = (-rz, 0, rx)，指向观者
        float nx = -rz;
        float nz = rx;

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(image.texture()));
        Matrix4f mat = poseStack.last().pose();
        // 图片顶边 = 内容顶部（贴图 v0，跳过画布顶部透明留白），因此默认「图片顶端对齐方块顶端」
        float vTop = image.v0();
        // 四个顶点（逆时针，自观者看）：左上 → 左下 → 右下 → 右上；uv 与顶点一一对应（u 向右、v 向下）
        addVertex(vc, mat, ax, top, az, 0.0F, vTop, nx, nz, packedLight);
        addVertex(vc, mat, ax, bottom, az, 0.0F, 1.0F, nx, nz, packedLight);
        addVertex(vc, mat, rightX, bottom, rightZ, 1.0F, 1.0F, nx, nz, packedLight);
        addVertex(vc, mat, rightX, top, rightZ, 1.0F, vTop, nx, nz, packedLight);

        // 手持海报物品时，用半透明物品图标标记方块本体所在的 1x1 面（方块本身不渲染，否则"看不见"）
        if (isHoldingPoster()) {
            drawGhost(bufferSource.getBuffer(RenderType.entityTranslucent(GHOST_TEXTURE)),
                    mat, baseAx, baseAz, rx, rz, nx, nz);
        }
    }

    /** 本地玩家主手/副手是否拿着海报物品（决定是否绘制方块本体标记）。 */
    private static boolean isHoldingPoster() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        return player.getMainHandItem().is(ModItems.POSTER_ITEM.get())
                || player.getOffhandItem().is(ModItems.POSTER_ITEM.get());
    }

    /** 在贴墙面画一个 1x1 的半透明物品图标，标记透明方块本体的位置（全亮，暗处也可见）。 */
    private static void drawGhost(VertexConsumer vc, Matrix4f mat, float ax, float az,
                                  float rx, float rz, float nx, float nz) {
        float rightX = ax + rx;
        float rightZ = az + rz;
        int light = 0xF000F0;
        int alpha = 110;
        vc.vertex(mat, ax, 1.0F, az).color(255, 255, 255, alpha).uv(0.0F, 0.0F)
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(nx, 0.0F, nz).endVertex();
        vc.vertex(mat, ax, 0.0F, az).color(255, 255, 255, alpha).uv(0.0F, 1.0F)
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(nx, 0.0F, nz).endVertex();
        vc.vertex(mat, rightX, 0.0F, rightZ).color(255, 255, 255, alpha).uv(1.0F, 1.0F)
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(nx, 0.0F, nz).endVertex();
        vc.vertex(mat, rightX, 1.0F, rightZ).color(255, 255, 255, alpha).uv(1.0F, 0.0F)
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(nx, 0.0F, nz).endVertex();
    }

    /** 写入一个顶点（白色，无叠加层，光照取方块光照）。 */
    private static void addVertex(VertexConsumer vc, Matrix4f mat, float x, float y, float z,
                                  float u, float v, float nx, float nz, int light) {
        vc.vertex(mat, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(nx, 0.0F, nz)
                .endVertex();
    }
}
