package com.cc_rc.block.console_panel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 大号空控制面板方块
 * 放置逻辑与文字显示逻辑均复用 {@link ConsolePanelBlock}，
 * 仅碰撞箱不同：宽度 16（比空控制面板左右各多 1 像素）、高度 14、厚度 2
 */
public class ConsolePanelLargeBlock extends ConsolePanelBlock {
    // 16x14x2 碰撞箱（宽度满 16、厚度 2），按 FACE / FACING 分别定义
    // FLOOR/CEILING 因形状非旋转对称，需区分南北向（x 满 16）与东西向（z 满 16）
    public static final VoxelShape FLOOR_SHAPE = Block.box(0, 0, 1, 16, 2, 15);
    public static final VoxelShape FLOOR_EAST_WEST_SHAPE = Block.box(1, 0, 0, 15, 2, 16);
    public static final VoxelShape CEILING_SHAPE = Block.box(0, 14, 1, 16, 16, 15);
    public static final VoxelShape CEILING_EAST_WEST_SHAPE = Block.box(1, 14, 0, 15, 16, 16);
    public static final VoxelShape WALL_NORTH_SHAPE = Block.box(0, 1, 14, 16, 15, 16);
    public static final VoxelShape WALL_SOUTH_SHAPE = Block.box(0, 1, 0, 16, 15, 2);
    public static final VoxelShape WALL_EAST_SHAPE = Block.box(0, 1, 0, 2, 15, 16);
    public static final VoxelShape WALL_WEST_SHAPE = Block.box(14, 1, 0, 16, 15, 16);

    public ConsolePanelLargeBlock(Properties properties) {
        super(properties);
    }

    /**
     * 获取大号面板碰撞箱，按 FACE / FACING 返回对应形状。
     * 大号面板为 16x14 长方形，FLOOR/CEILING 需随朝向旋转形状，
     * 否则东西向放置时碰撞箱与模型错位（WALL 各朝向形状独立，本就正确）。
     */
    public static VoxelShape getLargePanelShape(BlockState state) {
        AttachFace face = state.getValue(FACE);
        Direction facing = state.getValue(FACING);
        return switch (face) {
            case FLOOR -> switch (facing) {
                case EAST, WEST -> FLOOR_EAST_WEST_SHAPE;
                default -> FLOOR_SHAPE;
            };
            case CEILING -> switch (facing) {
                case EAST, WEST -> CEILING_EAST_WEST_SHAPE;
                default -> CEILING_SHAPE;
            };
            case WALL -> switch (facing) {
                case NORTH -> WALL_NORTH_SHAPE;
                case SOUTH -> WALL_SOUTH_SHAPE;
                case EAST -> WALL_EAST_SHAPE;
                case WEST -> WALL_WEST_SHAPE;
                default -> WALL_NORTH_SHAPE;
            };
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getLargePanelShape(state);
    }
}
