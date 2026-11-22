package com.cc_rc.block.console;

import com.cc_rc.CcRc;
import com.cc_rc.block.console.draw.ConsoleDrawPresets;
import com.cc_rc.block.console.draw.ConsoleShape;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * 控制柜表面绘图图形的渲染器。
 *
 * 所有图形都以「表面像素」为单位描述（1 像素 = 1/16 格），这里按图层从低到高
 * 逐层构造表面矩阵并绘制：
 *  - 实心图形（矩形 / 线段 / 箭头 / 空心边框）使用 1×1 纯白贴图 + 顶点色，
 *    任意颜色都不需要额外生成贴图；
 *  - 预设图案使用纯白 PNG 蒙版 + 顶点色染色，支持 0/90/180/270 度旋转与 1~3 倍放大。
 *
 * 外推距离小于组件模型的 0.002 格，因此图形始终位于组件下方，
 * 不会遮挡仪表 / 按钮等表面组件。
 */
public final class ConsoleDrawRenderer {

    /** 1×1 纯白贴图：实心图形通过顶点色显示为任意颜色。 */
    private static final ResourceLocation WHITE_TEXTURE =
            new ResourceLocation(CcRc.MODID, "textures/block/console_parts/white.png");

    /** 全亮光照：绘图通常位于面板表面，避免无光处颜色变黑。 */
    private static final int PACKED_LIGHT = 0xF000F0;

    /**
     * 平面图案使用的固定法线（视空间 +Y）。
     *
     * 实体类着色器（entitySolid / entityCutoutNoCull）会用顶点法线做一次漫反射
     * （minecraft_mix_light）：平面图案的法线正对玩家时漫反射系数只有约 0.74，
     * 白色会被压成浅灰色、其它颜色整体发灰。这里统一传入「视空间向上」的法线，
     * 漫反射系数恒为 1，图案始终显示为所选颜色（平面图案本就应像发光涂料一样）。
     */
    private static final Matrix3f FLAT_NORMAL = new Matrix3f();

    /** 最底层图形相对方块表面的外推距离（格）。 */
    private static final double OUTSET_BASE = 0.001D;
    /** 每增加一个图层向外的距离（格），保证图层之间不深度冲突。 */
    private static final double OUTSET_STEP = 0.0001D;

    private ConsoleDrawRenderer() {
    }

    /** 绘制整个结构表面上的全部图形（在组件之前调用）。 */
    public static void render(ConsoleBusBlockEntity be, PoseStack poseStack,
                              MultiBufferSource bufferSource) {
        BlockPos origin = be.getOrigin();
        if (origin == null) {
            return;
        }
        int sizeX = be.getSizeX();
        int sizeY = be.getSizeY();
        int sizeZ = be.getSizeZ();
        Direction busFacing = be.getBusFacing();
        BlockPos busPos = be.getBlockPos();

        for (Direction face : Direction.values()) {
            // 该表面没有任何图形时跳过矩阵构造
            boolean hasAny = false;
            for (ConsoleShape shape : be.getDrawShapes(face)) {
                hasAny = true;
                break;
            }
            if (!hasAny) {
                continue;
            }
            Vec3 uAxis = ConsoleSurface.u(face, busFacing);
            Vec3 vAxis = ConsoleSurface.v(face, busFacing);
            Vec3 nAxis = ConsoleSurface.normal(face);

            for (int layer = 0; layer < ConsoleShape.LAYERS; layer++) {
                double outset = OUTSET_BASE + layer * OUTSET_STEP;
                // 表面中心（相对方块坐标）作为局部原点
                Vec3 center = ConsoleSurface.toWorld(origin, sizeX, sizeY, sizeZ,
                        face, busFacing, 0.0D, 0.0D, outset)
                        .subtract(busPos.getX(), busPos.getY(), busPos.getZ());

                // 局部矩阵：顶点 (u, v, 0) → 表面像素坐标；1 像素 = 1/16 格
                Matrix4f local = new Matrix4f(
                        (float) uAxis.x() / 16.0F, (float) uAxis.y() / 16.0F,
                        (float) uAxis.z() / 16.0F, 0.0F,
                        (float) vAxis.x() / 16.0F, (float) vAxis.y() / 16.0F,
                        (float) vAxis.z() / 16.0F, 0.0F,
                        (float) nAxis.x() / 16.0F, (float) nAxis.y() / 16.0F,
                        (float) nAxis.z() / 16.0F, 0.0F,
                        (float) center.x, (float) center.y, (float) center.z, 1.0F);
                Matrix4f matrix = new Matrix4f(poseStack.last().pose()).mul(local);

                for (ConsoleShape shape : be.getDrawShapes(face)) {
                    if (shape.layer != layer) {
                        continue;
                    }
                    if (shape.type == com.cc_rc.block.console.draw.ConsoleShapeType.PRESET) {
                        renderPreset(shape, matrix, bufferSource);
                    } else {
                        VertexConsumer consumer = bufferSource.getBuffer(
                                RenderType.entitySolid(WHITE_TEXTURE));
                        renderSolid(shape, matrix, consumer);
                    }
                }
            }
        }
    }

    /**
     * 在指定绘制坐标系里画一组图形（斜面控制台使用：只画宿主格子内的图形）。
     *
     * 顶点仍使用结构像素坐标，坐标系的原点 / 轴由 {@link ConsoleFrame} 提供，
     * 因此图形会随坡面一起倾斜。
     */
    public static void renderInFrame(ConsoleFrame frame, java.util.List<ConsoleShape> shapes,
                                     PoseStack poseStack, MultiBufferSource bufferSource) {
        if (shapes == null || shapes.isEmpty()) {
            return;
        }
        Vec3 uAxis = frame.u();
        Vec3 vAxis = frame.v();
        Vec3 nAxis = frame.n();
        for (int layer = 0; layer < ConsoleShape.LAYERS; layer++) {
            double outset = OUTSET_BASE + layer * OUTSET_STEP;
            // 坐标系像素原点（相对渲染方块），顶点 (u, v, 0) 即结构像素坐标
            Vec3 center = frame.toLocal(0.0D, 0.0D, outset);
            Matrix4f local = new Matrix4f(
                    (float) uAxis.x() / 16.0F, (float) uAxis.y() / 16.0F,
                    (float) uAxis.z() / 16.0F, 0.0F,
                    (float) vAxis.x() / 16.0F, (float) vAxis.y() / 16.0F,
                    (float) vAxis.z() / 16.0F, 0.0F,
                    (float) nAxis.x() / 16.0F, (float) nAxis.y() / 16.0F,
                    (float) nAxis.z() / 16.0F, 0.0F,
                    (float) center.x, (float) center.y, (float) center.z, 1.0F);
            Matrix4f matrix = new Matrix4f(poseStack.last().pose()).mul(local);
            for (ConsoleShape shape : shapes) {
                if (shape.layer != layer) {
                    continue;
                }
                if (shape.type == com.cc_rc.block.console.draw.ConsoleShapeType.PRESET) {
                    renderPreset(shape, matrix, bufferSource);
                } else {
                    VertexConsumer consumer = bufferSource.getBuffer(
                            RenderType.entitySolid(WHITE_TEXTURE));
                    renderSolid(shape, matrix, consumer);
                }
            }
        }
    }

    /**
     * 在调用方给定的像素坐标系里绘制单个图形（涂鸦形状方块使用）。
     *
     * 坐标系需满足：顶点 (u, v, 0) 以「表面像素」为单位（1 像素 = 1/16 格），
     * 图形中心位于 (0, 0)。图层 / 外推由调用方自行处理。
     */
    public static void renderShape(ConsoleShape shape, Matrix4f matrix,
                                   MultiBufferSource bufferSource) {
        if (shape == null) {
            return;
        }
        if (shape.type == com.cc_rc.block.console.draw.ConsoleShapeType.PRESET) {
            renderPreset(shape, matrix, bufferSource);
        } else {
            renderSolid(shape, matrix,
                    bufferSource.getBuffer(RenderType.entitySolid(WHITE_TEXTURE)));
        }
    }

    /** 绘制实心图形（矩形 / 空心矩形 / 线段 / 箭头）。 */
    private static void renderSolid(ConsoleShape shape, Matrix4f matrix,
                                    VertexConsumer consumer) {
        int color = shape.color;
        switch (shape.type) {
            case RECT -> {
                float x0 = Math.min(shape.u1, shape.u2);
                float x1 = Math.max(shape.u1, shape.u2);
                float y0 = Math.min(shape.v1, shape.v2);
                float y1 = Math.max(shape.v1, shape.v2);
                if (x1 - x0 > 0.0F && y1 - y0 > 0.0F) {
                    quad(consumer, matrix, color, x0, y0, x1, y1);
                }
            }
            case RECT_OUTLINE -> {
                float x0 = Math.min(shape.u1, shape.u2);
                float x1 = Math.max(shape.u1, shape.u2);
                float y0 = Math.min(shape.v1, shape.v2);
                float y1 = Math.max(shape.v1, shape.v2);
                float w = Math.min(shape.width, Math.min((x1 - x0) / 2.0F, (y1 - y0) / 2.0F));
                if (w <= 0.0F) {
                    return;
                }
                // 四条边框；内侧矩形过小时退化为实心矩形
                if (x1 - x0 <= 2.0F * w || y1 - y0 <= 2.0F * w) {
                    quad(consumer, matrix, color, x0, y0, x1, y1);
                    return;
                }
                quad(consumer, matrix, color, x0, y0, x1, y0 + w);
                quad(consumer, matrix, color, x0, y1 - w, x1, y1);
                quad(consumer, matrix, color, x0, y0 + w, x0 + w, y1 - w);
                quad(consumer, matrix, color, x1 - w, y0 + w, x1, y1 - w);
            }
            case LINE -> line(consumer, matrix, color,
                    shape.u1, shape.v1, shape.u2, shape.v2, shape.width);
            case ARROW -> arrow(consumer, matrix, color, shape);
            default -> {
            }
        }
    }

    /** 线段：以两端点为中心、按线宽向两侧扩展的四边形。 */
    private static void line(VertexConsumer consumer, Matrix4f matrix,
                             int color, float x1, float y1, float x2, float y2, float width) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001F) {
            return;
        }
        float half = width / 2.0F;
        float px = -dy / len * half;
        float py = dx / len * half;
        quad4(consumer, matrix, color,
                x1 - px, y1 - py, x2 - px, y2 - py, x2 + px, y2 + py, x1 + px, y1 + py);
    }

    /**
     * 箭头：尾部到尖端画杆，尖端画三角头。
     * 头部尺寸与线宽成正比（头长 = 4×线宽，头半宽 = 1.5×线宽），
     * 线宽越大箭头越大，杆部长度随两端点变化。
     */
    private static void arrow(VertexConsumer consumer, Matrix4f matrix,
                              int color, ConsoleShape shape) {
        float x1 = shape.u1;
        float y1 = shape.v1;
        float x2 = shape.u2;
        float y2 = shape.v2;
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001F) {
            return;
        }
        float ux = dx / len;
        float uy = dy / len;
        float headLen = Math.min(len, shape.width * 4.0F);
        float headHalf = shape.width * 1.5F;
        // 杆部：尾部 → 头部底面
        float baseX = x2 - ux * headLen;
        float baseY = y2 - uy * headLen;
        if (len > headLen) {
            line(consumer, matrix, color, x1, y1, baseX, baseY, shape.width);
        }
        // 头部三角形：尖端 + 底面两侧（用重复末顶点补成四边形图元）
        float px = -uy * headHalf;
        float py = ux * headHalf;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        vertex(consumer, matrix, x2, y2, r, g, b);
        vertex(consumer, matrix, baseX + px, baseY + py, r, g, b);
        vertex(consumer, matrix, baseX - px, baseY - py, r, g, b);
        vertex(consumer, matrix, baseX - px, baseY - py, r, g, b);
    }

    /** 绘制预设图案：纯白蒙版 + 顶点色，按角度旋转四边形。 */
    private static void renderPreset(ConsoleShape shape, Matrix4f matrix,
                                     MultiBufferSource bufferSource) {
        ConsoleDrawPresets.Preset preset = ConsoleDrawPresets.byId(shape.preset);
        if (preset == null) {
            return;
        }
        float half = preset.size() * ConsoleDrawPresets.DISPLAY_SCALE
                * Math.max(ConsoleShape.MIN_SCALE, shape.scale) / 2.0F;
        // 绕图案中心按任意角度旋转；UV 固定绑定在四个角上（左下/右下/右上/左上）
        float radians = (float) Math.toRadians(shape.rotation);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        float cu = shape.u1;
        float cv = shape.v1;
        float[][] corner = {
                rotateOffset(-half, -half, cos, sin, cu, cv),
                rotateOffset(half, -half, cos, sin, cu, cv),
                rotateOffset(half, half, cos, sin, cu, cv),
                rotateOffset(-half, half, cos, sin, cu, cv)
        };
        float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityCutoutNoCull(preset.texture()));
        int r = (shape.color >> 16) & 0xFF;
        int g = (shape.color >> 8) & 0xFF;
        int b = shape.color & 0xFF;
        for (int i = 0; i < 4; i++) {
            vertexUV(consumer, matrix, corner[i][0], corner[i][1], r, g, b, uv[i][0], uv[i][1]);
        }
    }

    /** 把局部偏移 (ox, oy) 按 (cos, sin) 旋转后平移到中心 (cu, cv)。 */
    private static float[] rotateOffset(float ox, float oy, float cos, float sin,
                                        float cu, float cv) {
        return new float[]{cu + ox * cos - oy * sin, cv + ox * sin + oy * cos};
    }

    /** 轴对齐矩形（左下 → 右下 → 右上 → 左上，外面可见）。 */
    private static void quad(VertexConsumer consumer, Matrix4f matrix,
                             int color, float x0, float y0, float x1, float y1) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        vertex(consumer, matrix, x0, y0, r, g, b);
        vertex(consumer, matrix, x1, y0, r, g, b);
        vertex(consumer, matrix, x1, y1, r, g, b);
        vertex(consumer, matrix, x0, y1, r, g, b);
    }

    /** 任意四边形（调用方保证顶点顺序为外侧看逆时针）。 */
    private static void quad4(VertexConsumer consumer, Matrix4f matrix,
                              int color, float x1, float y1, float x2, float y2,
                              float x3, float y3, float x4, float y4) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        vertex(consumer, matrix, x1, y1, r, g, b);
        vertex(consumer, matrix, x2, y2, r, g, b);
        vertex(consumer, matrix, x3, y3, r, g, b);
        vertex(consumer, matrix, x4, y4, r, g, b);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
                               float x, float y, int r, int g, int b) {
        vertexUV(consumer, matrix, x, y, r, g, b, 0.0F, 0.0F);
    }

    private static void vertexUV(VertexConsumer consumer, Matrix4f matrix,
                                 float x, float y, int r, int g, int b, float u, float v) {
        consumer.vertex(matrix, x, y, 0.0F)
                .color(r, g, b, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(PACKED_LIGHT)
                .normal(FLAT_NORMAL, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
