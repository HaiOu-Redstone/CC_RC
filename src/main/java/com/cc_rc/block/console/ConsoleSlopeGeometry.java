package com.cc_rc.block.console;

import com.cc_rc.block.console.draw.ConsoleShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 斜面控制台的几何工具。
 *
 * 斜面模型（见 模型/控制台类/斜控制台方块/model.json）：
 *  - 斜面长 17.3 像素、宽 16 像素，低端在局部 (y=9, z=0)，高端在 (y≈15.62, z≈15.98)；
 *  - 局部 +z 是上坡方向，坡面与水平面的夹角为 22.5°。
 *
 * 映射规则（只做视觉 / 交互映射，不搬运数据）：
 *  - 宿主表面上被斜面盖住的那一格（16×16 像素）映射到坡面；
 *  - 宿主 V 方向的上端对齐坡面高端，因此宿主格子的 v ∈ [中心-8, 中心+8]
 *    对应坡面 t ∈ [1.3, 17.3]（t 为从低端沿坡面算起的像素距离）；
 *  - 宿主 U 轴直接作为坡面的横向轴，保证文字 / 组件不会被镜像。
 */
public final class ConsoleSlopeGeometry {

    /** 坡面倾角（弧度）。 */
    public static final double ANGLE = Math.toRadians(22.5D);
    public static final double SIN = Math.sin(ANGLE);
    public static final double COS = Math.cos(ANGLE);

    /** 坡面总长（像素）。 */
    public static final double LENGTH_PX = 17.3D;
    /** 坡板低端表面所在高度（方块局部坐标，像素）。 */
    public static final double LOW_EDGE_Y_PX = 9.0D;
    /** 被覆盖的 16 像素格子在坡面上的起点 / 中点（沿坡距离，像素）。 */
    public static final double COVERED_START_PX = LENGTH_PX - 16.0D;
    public static final double COVERED_MID_PX = COVERED_START_PX + 8.0D;

    private ConsoleSlopeGeometry() {
    }

    /** 一次命中在宿主表面上的等效坐标。 */
    public record Target(BlockPos busPos, Direction face, float u, float v) {
    }

    /** 坐标是否落在被斜面覆盖的 16×16 格子内。 */
    public static boolean inCell(ConsoleBusBlockEntity bus, BlockPos hostPos, Direction hostFace,
                                 double u, double v) {
        double[] center = cellCenterPixels(bus, hostPos, hostFace);
        return Math.abs(u - center[0]) <= 8.01D && Math.abs(v - center[1]) <= 8.01D;
    }

    /** 宿主格子的中心在该表面像素坐标系中的 (u, v)。 */
    public static double[] cellCenterPixels(ConsoleBusBlockEntity bus, BlockPos hostPos,
                                            Direction hostFace) {
        Vec3 cellCenter = new Vec3(hostPos.getX() + 0.5D, hostPos.getY() + 0.5D,
                hostPos.getZ() + 0.5D).add(ConsoleSurface.normal(hostFace).scale(0.5D));
        double u = ConsoleSurface.pixelU(bus.getOrigin(), bus.getSizeX(), bus.getSizeY(),
                bus.getSizeZ(), hostFace, bus.getBusFacing(), cellCenter);
        double v = ConsoleSurface.pixelV(bus.getOrigin(), bus.getSizeX(), bus.getSizeY(),
                bus.getSizeZ(), hostFace, bus.getBusFacing(), cellCenter);
        return new double[]{u, v};
    }

    /**
     * 构造斜面的绘制坐标系；映射信息不完整或与方块朝向不符时返回 null。
     *
     * 返回的坐标系以「宿主表面像素 (0,0)」为原点（可能在坡面外，仅用于数学换算），
     * 因此绘制组件 / 文字时可以直接沿用它们在宿主表面上的 (u, v)。
     */
    @Nullable
    public static ConsoleFrame frame(ConsoleSlopeBlockEntity slope, ConsoleBusBlockEntity bus) {
        BlockPos hostPos = slope.getHostPos();
        Direction hostFace = slope.getHostFace();
        if (hostPos == null || hostFace == null || bus.getOrigin() == null) {
            return null;
        }
        BlockState state = slope.getBlockState();
        if (!(state.getBlock() instanceof ConsoleSlopeBlock)) {
            return null;
        }
        Direction facing = state.getValue(ConsoleSlopeBlock.FACING);
        Direction busFacing = ConsoleSurface.busFacing(bus.getBusFacing());
        // 顶面贴合时 U 轴是否取反：与总线背面一致时保持原样（从背面阅读），
        // 与总线正面一致时取反（从正面阅读，内容相当于旋转 180°）
        boolean flipU = false;
        if (hostFace.getAxis().isHorizontal()) {
            // 竖直面：下坡方向必须朝外
            if (facing != hostFace) {
                return null;
            }
        } else if (hostFace != Direction.UP) {
            return null;
        } else {
            // 顶面：只接受与总线前后方向对齐的朝向（垂直方向会让内容旋转 90°，不支持）
            if (facing.getAxis() != busFacing.getAxis()) {
                return null;
            }
            flipU = facing != busFacing;
        }

        // 上坡方向：水平分量指向柜体（-facing），垂直分量向上
        Vec3 upSlope = new Vec3(-facing.getStepX() * COS, SIN, -facing.getStepZ() * COS);
        // 坡板低边在模型局部 z=0（即 FACING 一侧），且位于方块中线，
        // 因此低边中心 = 方块中心沿 FACING 外移半格，高度固定为 LOW_EDGE_Y_PX
        Vec3 lowEdgeCenter = new Vec3(
                slope.getBlockPos().getX() + 0.5D + facing.getStepX() * 0.5D,
                slope.getBlockPos().getY() + LOW_EDGE_Y_PX / ConsoleSurface.PIXELS_PER_BLOCK,
                slope.getBlockPos().getZ() + 0.5D + facing.getStepZ() * 0.5D);
        // 宿主格子中心在坡面上的位置
        Vec3 panelOrigin = lowEdgeCenter.add(
                upSlope.scale(COVERED_MID_PX / ConsoleSurface.PIXELS_PER_BLOCK));
        Vec3 u = ConsoleSurface.u(hostFace, bus.getBusFacing());
        if (flipU) {
            u = u.scale(-1.0D);
        }
        double[] cell = cellCenterPixels(bus, hostPos, hostFace);
        // 平移回「宿主像素 (0,0)」：绘制时可直接使用宿主坐标
        Vec3 origin = panelOrigin
                .subtract(u.scale(cell[0] / ConsoleSurface.PIXELS_PER_BLOCK))
                .subtract(upSlope.scale(cell[1] / ConsoleSurface.PIXELS_PER_BLOCK));
        Vec3 n = u.cross(upSlope).normalize();
        return new ConsoleFrame(slope.getBlockPos(), origin, u, upSlope, n);
    }

    /**
     * 命中斜面方块时，把命中点换算成宿主表面像素坐标。
     *
     * 斜面方块的碰撞箱是近似坡面的台阶形状，命中点通常落在坡面之外（上方或前方），
     * 因此这里用「眼睛 → 命中点」这条视线与坡面平面求交，取交点再反算宿主 (u, v)；
     * 若改用「命中点沿法线垂直投影」，在斜视角下会产生明显的偏移 / 放大。
     *
     * @param eyePos      视线起点（玩家眼睛位置），与命中点一起确定视线
     * @param hitLocation 方块射线命中的精确位置
     * @return 不在斜面工作面上时返回 null
     */
    @Nullable
    public static Target resolve(Level level, BlockPos clickedPos, Vec3 eyePos, Vec3 hitLocation) {
        if (!(level.getBlockState(clickedPos).getBlock() instanceof ConsoleSlopeBlock)) {
            return null;
        }
        if (!(level.getBlockEntity(clickedPos) instanceof ConsoleSlopeBlockEntity slope)) {
            return null;
        }
        BlockPos busPos = slope.getBusPos();
        if (busPos == null
                || !(level.getBlockEntity(busPos) instanceof ConsoleBusBlockEntity bus)) {
            return null;
        }
        if (bus.getFormState() == ConsoleBusBlockEntity.State.UNFORMED || bus.getOrigin() == null) {
            return null;
        }
        Direction hostFace = slope.getHostFace();
        BlockPos hostPos = slope.getHostPos();
        if (hostFace == null || hostPos == null) {
            return null;
        }
        ConsoleFrame frame = frame(slope, bus);
        if (frame == null) {
            return null;
        }
        // 视线：眼睛 → 命中点；必须从坡面外侧射入（与法线方向相反）才响应，避免从背面隔块操作
        Vec3 dir = hitLocation.subtract(eyePos);
        Vec3 normal = frame.n();
        double denom = dir.dot(normal);
        if (denom >= -1.0E-6D) {
            return null;
        }
        Vec3 planePoint = frame.toWorld(0.0D, 0.0D, 0.0D);
        double t = planePoint.subtract(eyePos).dot(normal) / denom;
        // 交点必须位于眼睛与命中点之间（坡面在碰撞箱内部），否则说明没有真正看向坡面
        if (t <= 0.0D || t > 1.02D) {
            return null;
        }
        Vec3 onPlane = eyePos.add(dir.scale(t)).subtract(planePoint);
        double u = onPlane.dot(frame.u()) * ConsoleSurface.PIXELS_PER_BLOCK;
        double v = onPlane.dot(frame.v()) * ConsoleSurface.PIXELS_PER_BLOCK;
        // 只在被覆盖的 16×16 格子内响应
        if (!inCell(bus, hostPos, hostFace, u, v)) {
            return null;
        }
        return new Target(busPos.immutable(), hostFace, (float) u, (float) v);
    }

    /** 绘图图形是否属于该格子（按图形中心近似判断，避免整幅图形跑到坡面外）。 */
    public static boolean shapeInCell(ConsoleBusBlockEntity bus, BlockPos hostPos,
                                      Direction hostFace, ConsoleShape shape) {
        float u;
        float v;
        if (shape.type == com.cc_rc.block.console.draw.ConsoleShapeType.LINE
                || shape.type == com.cc_rc.block.console.draw.ConsoleShapeType.ARROW) {
            u = (shape.u1 + shape.u2) / 2.0F;
            v = (shape.v1 + shape.v2) / 2.0F;
        } else if (shape.type == com.cc_rc.block.console.draw.ConsoleShapeType.RECT
                || shape.type == com.cc_rc.block.console.draw.ConsoleShapeType.RECT_OUTLINE) {
            u = (shape.u1 + shape.u2) / 2.0F;
            v = (shape.v1 + shape.v2) / 2.0F;
        } else {
            // 预设图案以 (u1, v1) 为中心
            u = shape.u1;
            v = shape.v1;
        }
        return inCell(bus, hostPos, hostFace, u, v);
    }
}
