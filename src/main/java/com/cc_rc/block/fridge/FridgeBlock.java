package com.cc_rc.block.fridge;

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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 冰箱（搬运自 Cooking for Blockheads）
 *
 * 放置：水平四方向（模型正面门位于 -Z 侧，朝向 = 玩家水平朝向的反方向，使门面对玩家）
 * 功能：27 格容器，右键打开原版箱子样式的 GUI（ChestMenu.threeRows）。
 * 破坏时由 onRemove 把容器内物品全部掉落（与原版箱子一致）。
 * 渲染：模型非完整 16x16x16 方块，覆写 getOcclusionShape 返回空形状，
 * 使相邻完整方块的面不被剔除（同玻璃），避免缝隙透视。
 */
public class FridgeBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public FridgeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 门面位于模型 -Z 侧：取玩家水平朝向的反方向，使放置后门面对玩家（同原版熔炉逻辑）
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        // 空遮挡形状：不剔除相邻完整方块朝向本方的面（同玻璃），缝隙处不再透视
        return Shapes.empty();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FridgeBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FridgeBlockEntity) {
                player.openMenu((FridgeBlockEntity) blockEntity);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FridgeBlockEntity) {
                net.minecraft.world.Containers.dropContents(level, pos, (FridgeBlockEntity) blockEntity);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
