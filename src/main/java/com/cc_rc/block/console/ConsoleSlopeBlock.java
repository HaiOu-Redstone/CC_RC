package com.cc_rc.block.console;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 斜面控制台方块。
 *
 * 只有上表面（斜面）是工作面：把它贴在控制柜的某个竖直侧面外侧（或顶面上），
 * 斜面会以倾斜的方式显示宿主被盖住那一格的组件 / 文字 / 绘图，
 * 交互也会映射回宿主表面的同一格（数据仍保存在总线里，不做搬运）。
 *
 * 本方块不是控制柜结构方块（不参与长方体泛洪校验），但放置 / 拆除会让相邻结构失效，
 * 需要重新点击总线的「检测并成型」才会重新登记映射。
 *
 * FACING = 下坡方向（水平四向），放置时取玩家朝向的反方向。
 *
 * 颜色：默认色 / 蓝色 / 黑色各注册成独立方块（与 {@link ConsoleBlock} 一致），
 * 只有颜色与结构颜色相同的斜面才会被总线登记映射，异色互不识别。
 */
public class ConsoleSlopeBlock extends Block implements EntityBlock {

    /** 下坡方向（水平四向）。 */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** 本方块对应的控制柜颜色：只有同色斜面会映射到同色结构上。 */
    public final ConsoleColor color;

    /** 碰撞箱台阶级数：把 16 像素的水平进深均分，每级 2 像素。 */
    private static final int STEP_COUNT = 8;

    /** 四个水平朝向的台阶形状缓存，下标为 {@link Direction#get2DDataValue()}。 */
    private static final VoxelShape[] STEP_SHAPES = new VoxelShape[4];

    /** 台阶顶面相对坡面整体下沉的距离（像素）：让选中框边缘藏到斜板下方，避免与斜面重叠成多条横线。 */
    private static final double TOP_OFFSET_PX = 1.0D;

    public ConsoleSlopeBlock(Properties properties) {
        this(properties, ConsoleColor.DEFAULT);
    }

    public ConsoleSlopeBlock(Properties properties, ConsoleColor color) {
        super(properties);
        this.color = color;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 玩家面向控制柜放置：下坡方向朝玩家（即玩家朝向的反方向）
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConsoleSlopeBlockEntity(pos, state);
    }

    /** 选中框与碰撞箱共用台阶形状（贴合斜板，而不是整块立方体）。 */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return stepShape(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return stepShape(state.getValue(FACING));
    }

    /**
     * 按朝向取台阶形状（首次使用时构建并缓存）。
     *
     * 每级顶面取该级中点处的坡面高度，最大误差约 0.42 像素，
     * 视觉与碰撞基本一致；低端朝 FACING，上坡方向为 -FACING。
     */
    private static VoxelShape stepShape(Direction facing) {
        int index = facing.get2DDataValue();
        // 竖直朝向异常时退回 NORTH，避免数组越界
        if (index < 0 || index > 3) {
            index = Direction.NORTH.get2DDataValue();
        }
        VoxelShape cached = STEP_SHAPES[index];
        if (cached == null) {
            cached = buildStepShape(facing);
            STEP_SHAPES[index] = cached;
        }
        return cached;
    }

    /** 构建某个朝向的台阶形状：8 级、每级 2 像素进深，高度覆盖 0 ~ 坡面。 */
    private static VoxelShape buildStepShape(Direction facing) {
        double stepPx = ConsoleSurface.PIXELS_PER_BLOCK / STEP_COUNT;
        double tan = Math.tan(ConsoleSlopeGeometry.ANGLE);
        VoxelShape shape = Shapes.empty();
        for (int i = 0; i < STEP_COUNT; i++) {
            double from = i * stepPx;
            double to = (i + 1) * stepPx;
            // 该级中点处的坡面高度（像素），整体再下沉 1 像素 → 方块比例
            double top = (ConsoleSlopeGeometry.LOW_EDGE_Y_PX - TOP_OFFSET_PX
                    + (from + to) / 2.0D * tan) / ConsoleSurface.PIXELS_PER_BLOCK;
            double minX;
            double maxX;
            double minZ;
            double maxZ;
            double start = from / ConsoleSurface.PIXELS_PER_BLOCK;
            double end = to / ConsoleSurface.PIXELS_PER_BLOCK;
            switch (facing) {
                // 低端在 FACING 一侧，上坡方向是它的反方向
                case NORTH -> {
                    minX = 0.0D; maxX = 1.0D; minZ = start; maxZ = end;
                }
                case SOUTH -> {
                    minX = 0.0D; maxX = 1.0D; minZ = 1.0D - end; maxZ = 1.0D - start;
                }
                case WEST -> {
                    minX = start; maxX = end; minZ = 0.0D; maxZ = 1.0D;
                }
                case EAST -> {
                    minX = 1.0D - end; maxX = 1.0D - start; minZ = 0.0D; maxZ = 1.0D;
                }
                default -> {
                    minX = 0.0D; maxX = 1.0D; minZ = 0.0D; maxZ = 1.0D;
                }
            }
            shape = Shapes.or(shape, Shapes.box(minX, 0.0D, minZ, maxX, top, maxZ));
        }
        return shape.optimize();
    }

    /** 空手右键：操作映射到斜面上的组件。 */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (ConsoleCompUse.tryUse(level, player, pos, hit)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /** 放置后让相邻结构失效：斜面是否参与映射需要重新成型才生效。 */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
                        boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            invalidateNearby(level, pos);
        }
    }

    /** 拆除前让所属结构失效（映射信息由总线在失效时清理）。 */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            invalidateLinked(level, pos);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    /** 兜底：非玩家途径移除时同样解除结构。 */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            invalidateLinked(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** 优先按方块实体里记录的总线失效，没有记录时再扫描相邻控制柜。 */
    private static void invalidateLinked(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ConsoleSlopeBlockEntity slope
                && slope.getBusPos() != null
                && level.getBlockEntity(slope.getBusPos())
                instanceof ConsoleBusBlockEntity bus) {
            bus.invalidateStructure();
            return;
        }
        invalidateNearby(level, pos);
    }

    /** 让六向相邻控制柜方块所属的总线全部失效。 */
    private static void invalidateNearby(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos next = pos.relative(dir);
            if (ConsoleStructureHelper.isConsole(level, next)) {
                ConsoleStructureHelper.invalidateFor(level, next);
            }
        }
    }
}
