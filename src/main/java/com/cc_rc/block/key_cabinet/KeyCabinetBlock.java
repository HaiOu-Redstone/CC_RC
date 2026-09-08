package com.cc_rc.block.key_cabinet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 钥匙柜方块
 * 可向 X+/-、Z+/- 四个水平方向放置（HORIZONTAL_FACING）。
 * 模型"模型/特殊控制台/钥匙柜/钥匙柜.json"转换（新模型默认贴 z 0~8 侧 = 靠墙侧，柜门朝外）。
 * 放置时 FACING = 玩家反方向：对墙放置时柜体贴在远离玩家的墙侧，柜门正对玩家。
 * 碰撞箱为贴墙长方体：宽 12（x 2~14，左右各空 2）、高 14（y 0~14）、厚 7（贴墙侧），
 * 四个朝向随 FACING 旋转。
 * 无方块实体；其坐标与朝向由多功能工具右键记录后录入钥匙分发控制器。
 */
public class KeyCabinetBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // 贴墙盒（facing=south / y=0 基准，模型贴 z 0~7）：宽12 x2..14 / 高14 y0..14 / 厚7 z0..7
    private static final VoxelShape SHAPE_NORTH = Block.box(2, 0, 9, 14, 14, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(2, 0, 0, 14, 14, 7);
    private static final VoxelShape SHAPE_EAST = Block.box(0, 0, 2, 7, 14, 14);
    private static final VoxelShape SHAPE_WEST = Block.box(9, 0, 2, 16, 14, 14);

    public KeyCabinetBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // FACING = 玩家朝向的反方向：柜体贴在玩家前方远离玩家一侧的墙上，柜门朝玩家
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_SOUTH;
        };
    }
}