package com.cc_rc.block.console;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * 多方块控制台的「表面坐标系」工具。
 *
 * 结构成型后是一个实心长方体，六个外表面各有一套二维坐标系：
 *  - U 轴：从外侧看向该表面时的「向右」方向；
 *  - V 轴：从外侧看向该表面时的「向上」方向；
 *  - N 轴：该表面的外法线（即表面朝外的方向）。
 *
 * 顶面/底面的朝向由总线水平朝向决定（总线背面方向 = FACING）：
 *  - 顶面：文字上方 = 总线正面方向（即 -FACING），U = FACING 逆时针 90°；
 *  - 底面：与顶面共用同一组朝向，U 取反（从下方仰视时左右相反）。
 *
 * 坐标单位是「像素」（1 格 = 16 像素）：表面文字的坐标 (u, v) 是相对于
 * 该表面矩形中心的有符号像素偏移，可直接跨格，与结构原点无关。
 */
public final class ConsoleSurface {

    /** 一格等于多少像素（与原版方块贴图分辨率一致）。 */
    public static final double PIXELS_PER_BLOCK = 16.0;

    /** 表面文字相对方块表面的外推距离（避免与方块面深度冲突）。 */
    public static final double TEXT_OUTSET = 0.005;

    private ConsoleSurface() {
    }

    /** 归一化总线朝向：只允许水平四向，异常时退回 NORTH。 */
    public static Direction busFacing(Direction facing) {
        if (facing == null || facing.getAxis() == Direction.Axis.Y) {
            return Direction.NORTH;
        }
        return facing;
    }

    /** 表面 U 轴（从外侧看的「向右」）。 */
    public static Vec3 u(Direction face, Direction busFacing) {
        Direction f = busFacing(busFacing);
        return switch (face) {
            // 四个侧面：从外侧看，南面右手指西、北面右手指东，依此类推
            case NORTH -> new Vec3(-1.0D, 0.0D, 0.0D);
            case SOUTH -> new Vec3(1.0D, 0.0D, 0.0D);
            case WEST -> new Vec3(0.0D, 0.0D, 1.0D);
            case EAST -> new Vec3(0.0D, 0.0D, -1.0D);
            // 顶面：U = FACING 绕 Y 轴顺时针 90°（即 (f.z, 0, -f.x)）
            case UP -> new Vec3(f.getStepZ(), 0.0D, -f.getStepX());
            // 底面：仰视时左右与顶面相反
            case DOWN -> new Vec3(-f.getStepZ(), 0.0D, f.getStepX());
        };
    }

    /** 表面 V 轴（从外侧看的「向上」）。 */
    public static Vec3 v(Direction face, Direction busFacing) {
        Direction f = busFacing(busFacing);
        if (face == Direction.UP || face == Direction.DOWN) {
            // 顶/底面：文字上方 = 总线正面方向（总线背面的反方向）
            return new Vec3(-f.getStepX(), 0.0D, -f.getStepZ());
        }
        return new Vec3(0.0D, 1.0D, 0.0D);
    }

    /** 表面外法线。 */
    public static Vec3 normal(Direction face) {
        return new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
    }

    /** 结构长方体的几何中心（世界坐标）。 */
    public static Vec3 center(BlockPos origin, int sizeX, int sizeY, int sizeZ) {
        return new Vec3(origin.getX() + sizeX / 2.0D,
                origin.getY() + sizeY / 2.0D,
                origin.getZ() + sizeZ / 2.0D);
    }

    /** 某个表面的矩形中心（世界坐标，位于方块表面平面上）。 */
    public static Vec3 faceCenter(BlockPos origin, int sizeX, int sizeY, int sizeZ, Direction face) {
        double x = origin.getX() + sizeX / 2.0D;
        double y = origin.getY() + sizeY / 2.0D;
        double z = origin.getZ() + sizeZ / 2.0D;
        switch (face) {
            case NORTH -> z = origin.getZ();
            case SOUTH -> z = origin.getZ() + sizeZ;
            case WEST -> x = origin.getX();
            case EAST -> x = origin.getX() + sizeX;
            case DOWN -> y = origin.getY();
            case UP -> y = origin.getY() + sizeY;
            default -> {
            }
        }
        return new Vec3(x, y, z);
    }

    /** 世界坐标 → 表面像素坐标 U。 */
    public static double pixelU(BlockPos origin, int sizeX, int sizeY, int sizeZ,
                                Direction face, Direction busFacing, Vec3 world) {
        return world.subtract(faceCenter(origin, sizeX, sizeY, sizeZ, face))
                .dot(u(face, busFacing)) * PIXELS_PER_BLOCK;
    }

    /** 世界坐标 → 表面像素坐标 V。 */
    public static double pixelV(BlockPos origin, int sizeX, int sizeY, int sizeZ,
                                Direction face, Direction busFacing, Vec3 world) {
        return world.subtract(faceCenter(origin, sizeX, sizeY, sizeZ, face))
                .dot(v(face, busFacing)) * PIXELS_PER_BLOCK;
    }

    /** 表面像素坐标 → 世界坐标（outset 为沿外法线的额外外推距离）。 */
    public static Vec3 toWorld(BlockPos origin, int sizeX, int sizeY, int sizeZ,
                               Direction face, Direction busFacing,
                               double uPx, double vPx, double outset) {
        Vec3 center = faceCenter(origin, sizeX, sizeY, sizeZ, face);
        return center
                .add(u(face, busFacing).scale(uPx / PIXELS_PER_BLOCK))
                .add(v(face, busFacing).scale(vPx / PIXELS_PER_BLOCK))
                .add(normal(face).scale(outset));
    }

    /** 表面沿 U 方向的半宽（像素）。 */
    public static double halfWidthPx(Direction face, Direction busFacing, int sizeX, int sizeY, int sizeZ) {
        Vec3 u = u(face, busFacing);
        int extent = Math.abs(u.x()) > 0.5D ? sizeX : sizeZ;
        return extent * PIXELS_PER_BLOCK / 2.0D;
    }

    /** 表面沿 V 方向的半高（像素）。 */
    public static double halfHeightPx(Direction face, Direction busFacing, int sizeX, int sizeY, int sizeZ) {
        Vec3 v = v(face, busFacing);
        int extent;
        if (Math.abs(v.y()) > 0.5D) {
            extent = sizeY;
        } else {
            extent = Math.abs(v.x()) > 0.5D ? sizeX : sizeZ;
        }
        return extent * PIXELS_PER_BLOCK / 2.0D;
    }

    /** 表面像素点是否落在该表面的矩形范围内（留 0.01 像素容差）。 */
    public static boolean contains(Direction face, Direction busFacing,
                                   int sizeX, int sizeY, int sizeZ, double uPx, double vPx) {
        return Math.abs(uPx) <= halfWidthPx(face, busFacing, sizeX, sizeY, sizeZ) + 0.01D
                && Math.abs(vPx) <= halfHeightPx(face, busFacing, sizeX, sizeY, sizeZ) + 0.01D;
    }
}
