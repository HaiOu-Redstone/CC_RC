package com.cc_rc.block.console;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * 控制台「绘制坐标系」：把表面像素坐标 (u, v) 映射到世界坐标。
 *
 * 普通控制柜表面与斜面控制台的斜面共用同一套渲染代码：
 *  - 普通表面：origin = 该表面矩形中心，u / v / n 取 {@link ConsoleSurface} 的轴；
 *  - 斜面表面：origin = 「宿主表面像素 (0,0)」在斜面平面上的位置，
 *    u = 宿主表面 U 轴，v = 沿坡向上的单位向量，n = 斜面外法线。
 *
 * toWorld 返回世界坐标，toLocal 返回相对渲染方块（PoseStack 已平移）的坐标。
 * 1 像素 = 1/16 格。
 */
public final class ConsoleFrame {

    /** 渲染所相对的方块坐标（方块实体渲染时 PoseStack 已平移到该方块）。 */
    private final BlockPos basePos;
    /** 像素坐标 (0,0) 对应的世界坐标。 */
    private final Vec3 origin;
    private final Vec3 u;
    private final Vec3 v;
    private final Vec3 n;

    public ConsoleFrame(BlockPos basePos, Vec3 origin, Vec3 u, Vec3 v, Vec3 n) {
        this.basePos = basePos;
        this.origin = origin;
        this.u = u;
        this.v = v;
        this.n = n;
    }

    /** 普通结构表面：以该表面矩形中心为像素原点。 */
    public static ConsoleFrame ofFace(BlockPos renderPos, BlockPos origin,
                                      int sizeX, int sizeY, int sizeZ,
                                      Direction face, Direction busFacing) {
        return new ConsoleFrame(renderPos,
                ConsoleSurface.faceCenter(origin, sizeX, sizeY, sizeZ, face),
                ConsoleSurface.u(face, busFacing),
                ConsoleSurface.v(face, busFacing),
                ConsoleSurface.normal(face));
    }

    public BlockPos basePos() {
        return this.basePos;
    }

    public Vec3 u() {
        return this.u;
    }

    public Vec3 v() {
        return this.v;
    }

    public Vec3 n() {
        return this.n;
    }

    /** 像素坐标 → 世界坐标（outset 为沿外法线外推的距离，单位格）。 */
    public Vec3 toWorld(double uPx, double vPx, double outset) {
        return this.origin
                .add(this.u.scale(uPx / ConsoleSurface.PIXELS_PER_BLOCK))
                .add(this.v.scale(vPx / ConsoleSurface.PIXELS_PER_BLOCK))
                .add(this.n.scale(outset));
    }

    /** 像素坐标 → 相对渲染方块的坐标（可直接写入顶点矩阵）。 */
    public Vec3 toLocal(double uPx, double vPx, double outset) {
        Vec3 world = toWorld(uPx, vPx, outset);
        return world.subtract(this.basePos.getX(), this.basePos.getY(), this.basePos.getZ());
    }
}
