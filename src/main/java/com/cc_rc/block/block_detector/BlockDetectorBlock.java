package com.cc_rc.block.block_detector;

import com.cc_rc.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 方块探测器（block_detector）
 *
 * 完整方块，可朝 x±/y±/z± 六个方向放置（FACING，同原版观察者），外观暂用
 * 原版观察者模型/贴图占位（blockstates 直接引用 minecraft:block/observer，
 * 后续换专属模型只需改 blockstates 与 item model）。
 *
 * 功能：CC: Tweaked 外设宿主（方块实体仅为外设承载），可**只读**探测其
 * 面向（FACING 前方）的方块信息：
 *   - 坐标（x/y/z）
 *   - 名称（注册名，如 minecraft:stone）
 *   - 模组来源（注册名命名空间，如 minecraft / cc_rc）
 *   - 若目标为方块实体，可获取其 NBT 数据（类似 /data get block，不可修改）
 */
public class BlockDetectorBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public BlockDetectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** 放置时面向玩家视线方向（与原版观察者一致）；支持上下左右前后共六个方向 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** 方块实体仅作 CC 外设承载（不存储数据，探测结果实时读取世界） */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockDetectorBlockEntity(pos, state);
    }
}
