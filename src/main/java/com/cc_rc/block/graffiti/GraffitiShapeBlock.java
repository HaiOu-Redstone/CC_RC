package com.cc_rc.block.graffiti;

import com.cc_rc.gui.GraffitiShapeMenu;
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
 * 涂鸦形状方块：贴在墙面上的透明方块，本身不渲染，图案由 {@link GraffitiShapeBER} 绘制。
 *
 * 放置方式：
 *  - 手持画图工具（draw_tool）右键任意实心墙面 → 在该墙面外侧放置本方块并立即打开图案设置界面；
 *  - 也可以直接放置本方块的物品，再右键打开设置界面。
 *
 * 几何与涂鸦文字 / 海报一致：四向贴墙（FACING = 被点击墙面的方向），方块空间内只有贴墙 1 像素厚，
 * 无碰撞，仅提供选中轮廓，支撑墙被破坏时自动掉落。
 */
public class GraffitiShapeBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // 与海报 / 涂鸦文字相同的 16x16x1 贴墙盒
    private static final VoxelShape SHAPE_NORTH = Block.box(0, 0, 15, 16, 16, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0, 0, 0, 16, 16, 1);
    private static final VoxelShape SHAPE_EAST = Block.box(0, 0, 0, 1, 16, 16);
    private static final VoxelShape SHAPE_WEST = Block.box(15, 0, 0, 16, 16, 16);

    public GraffitiShapeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 仅允许贴墙：点击水平墙面时 FACING = 点击面方向；点击顶/底面不能放置
        Direction dir = context.getClickedFace();
        if (dir.getAxis() != Direction.Axis.Y) {
            return this.defaultBlockState().setValue(FACING, dir);
        }
        return null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // 支撑墙位于 FACING 反方向
        return level.getBlockState(pos.relative(state.getValue(FACING).getOpposite())).isSolid();
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                  LevelAccessor level, BlockPos pos, BlockPos facingPos) {
        // 支撑墙消失 → 涂鸦方块一并掉落
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

    /** 方块本身不渲染（图案完全由 GraffitiShapeBER 绘制）。 */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GraffitiShapeBlockEntity(pos, state);
    }

    /** 右键打开图案设置界面（画图工具右键同样会走到这里）。 */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof GraffitiShapeBlockEntity be) {
            GraffitiShapeMenu.open(serverPlayer, be);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
