package com.cc_rc.block.console_panel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 控制面板方块基类
 * 仅包含放置逻辑、碰撞箱和文字显示，无交互或红石相关代码
 * 指示灯、仪表等方块可继承此类复用放置和文字显示功能
 */
public class ConsolePanelBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock {
    // 14x14x3 碰撞箱
    public static final VoxelShape FLOOR_SHAPE = Block.box(1, 0, 1, 15, 3, 15);
    public static final VoxelShape CEILING_SHAPE = Block.box(1, 13, 1, 15, 16, 15);
    public static final VoxelShape WALL_NORTH_SHAPE = Block.box(1, 1, 13, 15, 15, 16);
    public static final VoxelShape WALL_SOUTH_SHAPE = Block.box(1, 1, 0, 15, 15, 3);
    public static final VoxelShape WALL_EAST_SHAPE = Block.box(0, 1, 1, 3, 15, 15);
    public static final VoxelShape WALL_WEST_SHAPE = Block.box(13, 1, 1, 16, 15, 15);

    public ConsolePanelBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE);
    }

    /**
     * 获取碰撞箱，所有继承此类的方块共用
     */
    public static VoxelShape getPanelShape(BlockState state) {
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

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getPanelShape(state);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConsolePanelBlockEntity(pos, state);
    }

    /**
     * 放置时若物品有自定义名称，则保存到方块实体用于文字渲染
     * 所有继承此类的方块自动获得此功能
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (stack.hasCustomHoverName()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ConsolePanelBlockEntity panelBE) {
                panelBE.setText(stack.getHoverName());
            }
        }
    }
}
