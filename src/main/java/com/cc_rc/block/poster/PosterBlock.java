package com.cc_rc.block.poster;

import com.cc_rc.gui.PosterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 海报方块（全局仅此一种方块 poster）。
 *
 * 贴墙装饰方块：可向 X±/Z± 四个水平方向放置（点击墙侧面，FACING = 点击墙面的方向），
 * 碰撞箱 16(宽) x 16(高) x 1(厚) 像素，板子贴墙。
 *
 * 与旧实现的区别（旧版把每张海报各注册成一个方块 poster_2 ~ poster_26）：
 * 现在只注册一个方块，具体显示哪张图片、缩放倍率、垂直/水平偏移量都存在方块实体里，
 * 由客户端 PosterBER 直接绘制贴图四边形（因此不再受原版方块模型元素坐标 ±16/32 的限制，
 * 可以任意大、可超出格子）。玩家右键打开设置界面（PosterScreen）来切换图片与调整参数。
 *
 * 方向语义（与原版 WallSignBlock 一致）：
 *  - FACING = 点击的墙面方向（被点击的墙位于 pos.relative(FACING.getOpposite())，即 FACING 反方向）；
 *  - 板子在方块空间内位于 FACING 的反侧（离墙最近处渲染），正面朝 FACING 方向（玩家侧可看到图片）。
 *
 * 生命周期：
 *  - 只能贴墙放置（点击顶/底面返回 null = 放置失败）；
 *  - 支撑墙被破坏时自动掉落（updateShape 校验 canSurvive）。
 */
public class PosterBlock extends HorizontalDirectionalBlock implements EntityBlock {
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

    /** 方块本身不渲染（贴图完全由 PosterBER 按方块实体参数绘制）。 */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PosterBlockEntity(pos, state);
    }

    /** 右键（非潜行）打开海报设置界面：选择图片、缩放倍率、垂直/水平偏移。 */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        // 仅服务端打开菜单（客户端返回成功避免额外逻辑）
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof PosterBlockEntity be) {
            PosterMenu.open(serverPlayer, be);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
