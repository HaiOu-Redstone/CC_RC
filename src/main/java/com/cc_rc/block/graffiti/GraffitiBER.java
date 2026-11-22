package com.cc_rc.block.graffiti;

import com.cc_rc.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/**
 * 涂鸦文字渲染器：把方块实体里的文字直接绘制到墙面上。
 *
 * 锚点与海报完全一致（观者视角左上角、离墙 1 像素）：
 *  - FACING=north：锚点 (1.0, 1.0, 15/16)，观者向右 = -X；
 *  - FACING=south：锚点 (0.0, 1.0,  1/16)，观者向右 = +X；
 *  - FACING=east ：锚点 ( 1/16, 1.0, 1.0)，观者向右 = -Z；
 *  - FACING=west ：锚点 (15/16, 1.0, 0.0)，观者向右 = +Z。
 *
 * 文字自锚点向右、向下排列：
 *  - 每字体像素对应的世界长度 = scale / font.lineHeight（scale = 行高，单位格）；
 *  - dx 沿观者向右偏移、dy 沿竖直向上偏移（单位格，可负）。
 *
 * 手持编辑工具时，在未偏移的锚点绘制 1x1 半透明物品图标，
 * 标记这个透明方块本体的位置（与海报手持物品时的幽灵标记一致）。
 */
public class GraffitiBER implements BlockEntityRenderer<GraffitiBlockEntity> {

    /** 文字 / 幽灵标记离墙面的距离（格）：1 像素，避免与墙方块表面 z-fighting。 */
    private static final float WALL_GAP = 1.0F / 16.0F;
    /** 文字沿外法线的额外外推（格），避免与幽灵标记共面。 */
    private static final float TEXT_OUTSET = 0.01F;
    /** 全亮光照：墙面暗处文字也应保持所选颜色（与海报/控制台做法一致）。 */
    private static final int PACKED_LIGHT = 0xF000F0;
    /** 幽灵标记贴图：本方块物品图标。 */
    private static final ResourceLocation GHOST_TEXTURE =
            new ResourceLocation("cc_rc", "textures/item/graffiti.png");

    public GraffitiBER(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(GraffitiBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof GraffitiBlock)) {
            return;
        }
        Direction facing = state.getValue(GraffitiBlock.FACING);

        // 观者视角锚点（左上角）与「观者向右」单位向量（竖直方向恒为 +Y）
        float ax;
        float az;
        float rx;
        float rz;
        switch (facing) {
            case NORTH -> {
                ax = 1.0F;
                az = 1.0F - WALL_GAP;
                rx = -1.0F;
                rz = 0.0F;
            }
            case SOUTH -> {
                ax = 0.0F;
                az = WALL_GAP;
                rx = 1.0F;
                rz = 0.0F;
            }
            case EAST -> {
                ax = WALL_GAP;
                az = 1.0F;
                rx = 0.0F;
                rz = -1.0F;
            }
            default -> {   // WEST
                ax = 1.0F - WALL_GAP;
                az = 0.0F;
                rx = 0.0F;
                rz = 1.0F;
            }
        }
        // 面法线 = 向右向量 × 向上向量 = (-rz, 0, rx)，指向观者
        float nx = -rz;
        float nz = rx;

        // 未偏移的锚点（幽灵标记用，标记的是方块本体）
        float baseAx = ax;
        float baseAz = az;

        Component text = be.toComponent();
        if (!text.getString().isEmpty()) {
            drawText(be, text, poseStack, bufferSource, ax, az, rx, rz, nx, nz);
        }

        // 手持编辑工具时，用半透明物品图标标记方块本体所在的 1x1 面
        if (isHoldingEditTool()) {
            drawGhost(bufferSource.getBuffer(RenderType.entityTranslucent(GHOST_TEXTURE)),
                    poseStack.last().pose(), baseAx, baseAz, rx, rz, nx, nz);
        }
    }

    /**
     * 在墙面上绘制文字：用仿射矩阵把「字体像素」映射到世界（与 ConsoleBusBER 的方式一致）。
     * 字体坐标 y 轴向下，因此竖直基向量取负。
     */
    private static void drawText(GraffitiBlockEntity be, Component text, PoseStack poseStack,
                                 MultiBufferSource bufferSource,
                                 float ax, float az, float rx, float rz, float nx, float nz) {
        Font font = Minecraft.getInstance().font;
        // 每字体像素对应的世界长度：行高（格）/ 字体行高（像素）
        float perFontPixel = be.getScale() / Math.max(1, font.lineHeight);

        // 起点 = 锚点 + dx（沿观者向右）+ dy（向上），再沿法线外推避免深度冲突
        float startX = ax + rx * be.getDx() + nx * TEXT_OUTSET;
        float startZ = az + rz * be.getDx() + nz * TEXT_OUTSET;
        float startY = 1.0F + be.getDy();

        Matrix4f local = new Matrix4f(
                rx * perFontPixel, 0.0F, rz * perFontPixel, 0.0F,          // 第 1 列：向右
                0.0F, -perFontPixel, 0.0F, 0.0F,                          // 第 2 列：向下
                nx * perFontPixel, 0.0F, nz * perFontPixel, 0.0F,          // 第 3 列：法线
                startX, startY, startZ, 1.0F);                            // 第 4 列：平移
        Matrix4f matrix = new Matrix4f(poseStack.last().pose()).mul(local);

        // 颜色补上不透明 alpha（Font 顶点色为 ARGB）
        int color = 0xFF000000 | (be.getColor() & 0xFFFFFF);
        font.drawInBatch(text, 0.0F, 0.0F, color, false, matrix, bufferSource,
                Font.DisplayMode.POLYGON_OFFSET, 0, PACKED_LIGHT);
    }

    /** 本地玩家主手/副手是否拿着编辑工具（决定是否绘制方块本体标记）。 */
    private static boolean isHoldingEditTool() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        return player.getMainHandItem().is(ModItems.EDIT_TOOL.get())
                || player.getOffhandItem().is(ModItems.EDIT_TOOL.get());
    }

    /** 在贴墙面画一个 1x1 的半透明物品图标，标记透明方块本体的位置（全亮，暗处也可见）。 */
    private static void drawGhost(VertexConsumer vc, Matrix4f mat, float ax, float az,
                                  float rx, float rz, float nx, float nz) {
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
