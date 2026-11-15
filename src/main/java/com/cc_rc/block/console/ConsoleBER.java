package com.cc_rc.block.console;

import com.cc_rc.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

/**
 * 控制台结构方块渲染器。
 *
 * 仅在「部分成型」状态下，给结构的每个外露表面（邻居不是控制台方块的面）
 * 绘制一层「一像素一格」的白色网格，方便玩家按 1/16 格精度摆放组件与装饰；
 * 未成型/完全成型时不绘制任何内容（不产生额外渲染开销）。
 */
public class ConsoleBER implements BlockEntityRenderer<ConsoleBlockEntity> {

    /** 网格线颜色（白色，低透明度，避免遮挡方块本身材质）。 */
    private static final float GRID_R = 1.0F;
    private static final float GRID_G = 1.0F;
    private static final float GRID_B = 1.0F;
    private static final float GRID_A = 0x38 / 255.0F;
    /** 外轮廓线略亮，用于区分相邻方块的表面。 */
    private static final float BORDER_A = 0x60 / 255.0F;
    /** 网格离方块表面向外的距离（格），防止与方块表面 z-fighting。 */
    private static final float OUTSET = 0.002F;

    public ConsoleBER(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ConsoleBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // 只有部分成型时才需要辅助网格
        if (be.getStructureState() != 1) {
            return;
        }
        Level level = be.getLevel();
        if (level == null) {
            return;
        }

        BlockPos pos = be.getBlockPos();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();

        for (Direction dir : Direction.values()) {
            // 邻居也是控制台方块 → 该面在结构内部，不绘制
            if (ConsoleStructureHelper.isConsole(level.getBlockState(pos.relative(dir)))) {
                continue;
            }
            drawFaceGrid(consumer, matrix, dir);
        }
    }

    /** 在指定朝向的方块面上绘制 16x16 像素网格 + 外轮廓。 */
    private static void drawFaceGrid(VertexConsumer consumer, Matrix4f matrix, Direction dir) {
        Direction.Axis axis = dir.getAxis();
        // 面内的两个坐标轴（与法线轴共同构成三个互不相同的轴）
        Direction.Axis uAxis = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        Direction.Axis vAxis = axis == Direction.Axis.Y ? Direction.Axis.Z : Direction.Axis.Y;
        // 面所在平面坐标：正方向的面在 1 之外，负方向的面在 0 之外
        float plane = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? 1.0F + OUTSET : -OUTSET;

        // 15 条内部网格线，间距 1/16 格（= 1 像素）
        for (int i = 1; i <= 15; i++) {
            float t = i / 16.0F;
            // 平行于 u 轴
            drawLine(consumer, matrix, dir, axis, uAxis, vAxis, plane,
                    0.0F, t, 1.0F, t, GRID_A);
            // 平行于 v 轴
            drawLine(consumer, matrix, dir, axis, uAxis, vAxis, plane,
                    t, 0.0F, t, 1.0F, GRID_A);
        }
        // 四条边（0 与 1 处），稍亮
        drawLine(consumer, matrix, dir, axis, uAxis, vAxis, plane, 0.0F, 0.0F, 1.0F, 0.0F, BORDER_A);
        drawLine(consumer, matrix, dir, axis, uAxis, vAxis, plane, 0.0F, 1.0F, 1.0F, 1.0F, BORDER_A);
        drawLine(consumer, matrix, dir, axis, uAxis, vAxis, plane, 0.0F, 0.0F, 0.0F, 1.0F, BORDER_A);
        drawLine(consumer, matrix, dir, axis, uAxis, vAxis, plane, 1.0F, 0.0F, 1.0F, 1.0F, BORDER_A);
    }

    /** 绘制一条面内线段（u/v 为面内局部坐标 0~1）。 */
    private static void drawLine(VertexConsumer consumer, Matrix4f matrix, Direction dir,
                                 Direction.Axis axis, Direction.Axis uAxis, Direction.Axis vAxis,
                                 float plane, float u0, float v0, float u1, float v1, float alpha) {
        int nx = dir.getStepX();
        int ny = dir.getStepY();
        int nz = dir.getStepZ();
        emit(consumer, matrix, axis, uAxis, vAxis, plane, u0, v0, nx, ny, nz, alpha);
        emit(consumer, matrix, axis, uAxis, vAxis, plane, u1, v1, nx, ny, nz, alpha);
    }

    /** 写入一个顶点：把面内局部坐标按三个轴映射回 x/y/z。 */
    private static void emit(VertexConsumer consumer, Matrix4f matrix,
                             Direction.Axis axis, Direction.Axis uAxis, Direction.Axis vAxis,
                             float plane, float u, float v,
                             int nx, int ny, int nz, float alpha) {
        float x = component(Direction.Axis.X, axis, uAxis, vAxis, plane, u, v);
        float y = component(Direction.Axis.Y, axis, uAxis, vAxis, plane, u, v);
        float z = component(Direction.Axis.Z, axis, uAxis, vAxis, plane, u, v);
        consumer.vertex(matrix, x, y, z)
                .color(GRID_R, GRID_G, GRID_B, alpha)
                .normal(nx, ny, nz)
                .endVertex();
    }

    private static float component(Direction.Axis target, Direction.Axis axis,
                                   Direction.Axis uAxis, Direction.Axis vAxis,
                                   float plane, float u, float v) {
        if (target == axis) {
            return plane;
        }
        return target == uAxis ? u : v;
    }
}
