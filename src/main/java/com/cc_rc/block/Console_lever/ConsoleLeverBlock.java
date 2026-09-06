package com.cc_rc.block.Console_lever;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 控制台拉杆方块
 * 继承原版 LeverBlock，支持显示文字
 * 放置时若物品有自定义名称，则显示该名称文字
 * 用于 console_lever_1 和 console_lever_2
 */
public class ConsoleLeverBlock extends LeverBlock implements EntityBlock {
    // 14x14x3 碰撞箱
    protected static final VoxelShape FLOOR_SHAPE = Block.box(1, 0, 1, 15, 3, 15);
    protected static final VoxelShape CEILING_SHAPE = Block.box(1, 13, 1, 15, 16, 15);
    protected static final VoxelShape WALL_NORTH_SHAPE = Block.box(1, 1, 13, 15, 15, 16);
    protected static final VoxelShape WALL_SOUTH_SHAPE = Block.box(1, 1, 0, 15, 15, 3);
    protected static final VoxelShape WALL_EAST_SHAPE = Block.box(0, 1, 1, 3, 15, 15);
    protected static final VoxelShape WALL_WEST_SHAPE = Block.box(13, 1, 1, 16, 15, 15);

    public ConsoleLeverBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        AttachFace face = state.getValue(FACE);
        Direction facing = state.getValue(FACING);
        return switch (face) {
            case FLOOR -> FLOOR_SHAPE;
            case CEILING -> CEILING_SHAPE;
            case WALL -> switch (facing) {
                case NORTH -> WALL_NORTH_SHAPE;
                case SOUTH -> WALL_SOUTH_SHAPE;
                case EAST -> WALL_EAST_SHAPE;
                case WEST -> WALL_WEST_SHAPE;
                default -> WALL_NORTH_SHAPE;
            };
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConsoleLeverBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (stack.hasCustomHoverName()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ConsoleLeverBlockEntity leverBE) {
                leverBE.setText(stack.getHoverName());
            }
        }
    }
}