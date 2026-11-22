package com.cc_rc.block.graffiti;

import com.cc_rc.ModItems;
import com.cc_rc.block.console.ConsoleDrawRenderer;
import com.cc_rc.block.console.draw.ConsoleShape;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/**
 * 涂鸦形状渲染器：把方块实体里的单个图案直接绘制到墙面上。
 *
 * 锚点取贴墙面中心（例如 FACING=north 时为 (0.5, 0.5, 15/16)），
 * 像素坐标系 1 像素 = 1/16 格：
 *  - dx 沿观者向右偏移、dy 沿竖直向上偏移（单位格）；
 *  - rotation 在像素平面内绕图案中心旋转（单位度），对所有图形类型生效。
 *
 * 图形本身的绘制直接复用控制柜表面画图的 {@link ConsoleDrawRenderer}，
 * 因此预设图案的染色 / 旋转以及矩形 / 线段的顶点生成逻辑与画图工具完全一致。
 *
 * 手持画图工具时，在方块本体所在的 1x1 面上绘制半透明物品图标作为幽灵标记。
 */
public class GraffitiShapeBER implements BlockEntityRenderer<GraffitiShapeBlockEntity> {

    /** 图案离墙面的距离（格）：1 像素，避免与墙方块表面 z-fighting。 */
    private static final float WALL_GAP = 1.0F / 16.0F;
    /** 图案沿外法线的额外外推（格），避免与幽灵标记共面。 */
    private static final float SHAPE_OUTSET = 0.01F;
    /** 全亮光照：墙面暗处图案也应保持所选颜色。 */
    private static final int PACKED_LIGHT = 0xF000F0;
    /** 幽灵标记贴图：本方块物品图标。 */
    private static final ResourceLocation GHOST_TEXTURE =
            new ResourceLocation("cc_rc", "textures/item/graffiti_shape.png");

    public GraffitiShapeBER(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(GraffitiShapeBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof GraffitiShapeBlock)) {
            return;
        }
        Direction facing = state.getValue(GraffitiShapeBlock.FACING);

        // 贴墙面中心（像素坐标原点）与「观者向右」单位向量（竖直方向恒为 +Y）
        float cx;
        float cz;
        float rx;
        float rz;
        switch (facing) {
            case NORTH -> {
                cx = 0.5F;
                cz = 1.0F - WALL_GAP;
                rx = -1.0F;
                rz = 0.0F;
            }
            case SOUTH -> {
                cx = 0.5F;
                cz = WALL_GAP;
                rx = 1.0F;
                rz = 0.0F;
            }
            case EAST -> {
                cx = WALL_GAP;
                cz = 0.5F;
                rx = 0.0F;
                rz = -1.0F;
            }
            default -> {   // WEST
                cx = 1.0F - WALL_GAP;
                cz = 0.5F;
                rx = 0.0F;
                rz = 1.0F;
            }
        }
        // 面法线 = 向右向量 × 向上向量 = (-rz, 0, rx)，指向观者
        float nx = -rz;
        float nz = rx;

        ConsoleShape shape = be.toShape();
        drawShape(be, shape, poseStack, bufferSource, cx, cz, rx, rz, nx, nz);

        // 手持画图工具时，用半透明物品图标标记方块本体所在的 1x1 面
        if (isHoldingDrawTool()) {
            drawGhost(bufferSource.getBuffer(RenderType.entityTranslucent(GHOST_TEXTURE)),
                    poseStack.last().pose(), facing, rx, rz, nx, nz);
        }
    }

    /** 构造像素坐标系并绘制单个图形。 */
    private static void drawShape(GraffitiShapeBlockEntity be, ConsoleShape shape,
                                  PoseStack poseStack, MultiBufferSource bufferSource,
                                  float cx, float cz, float rx, float rz, float nx, float nz) {
        float radians = (float) Math.toRadians(be.getRotation());
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);

        // 像素原点 = 贴墙面中心 + dx（向右）+ dy（向上），再沿法线外推
        float ox = cx + rx * be.getDx() + nx * SHAPE_OUTSET;
        float oz = cz + rz * be.getDx() + nz * SHAPE_OUTSET;
        float oy = 0.5F + be.getDy();

        // 第 1 列：像素 U 轴（向右旋转 rotation 度）；第 2 列：像素 V 轴（向上旋转后再取反方向由顶点决定）
        Matrix4f local = new Matrix4f(
                rx * cos / 16.0F, sin / 16.0F, rz * cos / 16.0F, 0.0F,
                -rx * sin / 16.0F, cos / 16.0F, -rz * sin / 16.0F, 0.0F,
                nx / 16.0F, 0.0F, nz / 16.0F, 0.0F,
                ox, oy, oz, 1.0F);
        Matrix4f matrix = new Matrix4f(poseStack.last().pose()).mul(local);
        ConsoleDrawRenderer.renderShape(shape, matrix, bufferSource);
    }

    /** 本地玩家主手/副手是否拿着画图工具（决定是否绘制方块本体标记）。 */
    private static boolean isHoldingDrawTool() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        return player.getMainHandItem().is(ModItems.DRAW_TOOL.get())
                || player.getOffhandItem().is(ModItems.DRAW_TOOL.get());
    }

    /** 在贴墙面画一个 1x1 的半透明物品图标，标记透明方块本体的位置（全亮，暗处也可见）。 */
    private static void drawGhost(VertexConsumer vc, Matrix4f mat, Direction facing,
                                  float rx, float rz, float nx, float nz) {
        float ax;
        float az;
        switch (facing) {
            case NORTH -> {
                ax = 1.0F;
                az = 1.0F - WALL_GAP;
            }
            case SOUTH -> {
                ax = 0.0F;
                az = WALL_GAP;
            }
            case EAST -> {
                ax = WALL_GAP;
                az = 1.0F;
            }
            default -> {   // WEST
                ax = 1.0F - WALL_GAP;
                az = 0.0F;
            }
        }
        float rightX = ax + rx;
        float rightZ = az + rz;
        int alpha = 110;
        vc.vertex(mat, ax, 1.0F, az).color(255, 255, 255, alpha).uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(PACKED_LIGHT)
                .normal(nx, 0.0F, nz).endVertex();
        vc.vertex(mat, ax, 0.0F, az).color(255, 255, 255, alpha).uv(0.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(PACKED_LIGHT)
                .normal(nx, 0.0F, nz).endVertex();
        vc.vertex(mat, rightX, 0.0F, rightZ).color(255, 255, 255, alpha).uv(1.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(PACKED_LIGHT)
                .normal(nx, 0.0F, nz).endVertex();
        vc.vertex(mat, rightX, 1.0F, rightZ).color(255, 255, 255, alpha).uv(1.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(PACKED_LIGHT)
                .normal(nx, 0.0F, nz).endVertex();
    }
}
