package com.cc_rc.block.poster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 海报方块（poster_1）
 *
 * 贴墙装饰方块：可向 X±/Z± 四个水平方向放置（点击墙侧面，FACING = 点击墙面的方向），
 * 碰撞箱 16(宽) x 16(高) x 1(厚) 像素，板子贴墙渲染（正面显示海报图片，其余面透明，仅粒子）。
 *
 * 方向语义（与原版 WallSignBlock 一致）：
 *  - FACING = 点击的墙面方向（被点击的墙位于 pos.relative(FACING.getOpposite())，即 FACING 反方向）；
 *  - 板子在方块空间内位于 FACING 的反侧（离墙最近处渲染），正面朝 FACING 方向（玩家侧可看到图片）。
 *
 * 生命周期：
 *  - 只能贴墙放置（点击顶/底面返回 null = 放置失败）；
 *  - 支撑墙被破坏时自动掉落（updateShape 校验 canSurvive）。
 */
public class PosterBlock extends HorizontalDirectionalBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // 16x16x1 贴墙盒（板子位于方块空间离墙最近处 1 像素厚）：
    // FACING=north 贴北墙 → 板子在 z 大侧（z 15~16）；其余朝向按旋转对应
    private static final VoxelShape SHAPE_NORTH = Block.box(0, 0, 15, 16, 16, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0, 0, 0, 16, 16, 1);
    private static final VoxelShape SHAPE_EAST = Block.box(0, 0, 0, 1, 16, 16);
    private static final VoxelShape SHAPE_WEST = Block.box(15, 0, 0, 16, 16, 16);

    public PosterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 仅允许贴墙：点击水平墙面时 FACING = 点击面方向；点击顶/底面不能放置（返回 null）
        Direction dir = context.getClickedFace();
        if (dir.getAxis() != Direction.Axis.Y) {
            return this.defaultBlockState().setValue(FACING, dir);
        }
        return null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // 支撑墙位于 FACING 反方向（如 FACING=north 时墙在 z+1 侧，即被点击的墙面所在的方块）
        return level.getBlockState(pos.relative(state.getValue(FACING).getOpposite())).isSolid();
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                  LevelAccessor level, BlockPos pos, BlockPos facingPos) {
        // 支撑墙变化（facing 为墙→板方向，其反向等于 FACING）且不再满足支撑条件 → 方块转为空气掉落
        return facing.getOpposite() == state.getValue(FACING) && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, facing, facingState, level, pos, facingPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            default -> SHAPE_WEST;
        };
    }
}