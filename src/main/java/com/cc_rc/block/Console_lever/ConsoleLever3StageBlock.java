package com.cc_rc.block.Console_lever;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 3挡位控制台拉杆方块
 * 继承原版 LeverBlock，使用 IntegerProperty STAGE (0,1,2) 替代原版的 POWERED 布尔值
 * 点击时依次切换挡位：0 -> 1 -> 2 -> 0
 * 红石信号输出：挡位0=0，挡位1=8，挡位2=15
 * 杆模型旋转角度：挡位0=0°，挡位1=90°，挡位2=180°
 * 用于 console_lever_6 和 console_lever_7
 */
public class ConsoleLever3StageBlock extends LeverBlock implements EntityBlock {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 2);

    // 14x14x3 碰撞箱
    protected static final VoxelShape FLOOR_SHAPE = Block.box(1, 0, 1, 15, 3, 15);
    protected static final VoxelShape CEILING_SHAPE = Block.box(1, 13, 1, 15, 16, 15);
    protected static final VoxelShape WALL_NORTH_SHAPE = Block.box(1, 1, 13, 15, 15, 16);
    protected static final VoxelShape WALL_SOUTH_SHAPE = Block.box(1, 1, 0, 15, 15, 3);
    protected static final VoxelShape WALL_EAST_SHAPE = Block.box(0, 1, 1, 3, 15, 15);
    protected static final VoxelShape WALL_WEST_SHAPE = Block.box(13, 1, 1, 16, 15, 15);

    public ConsoleLever3StageBlock(Properties properties) {
        super(properties);
        // 在 LeverBlock 默认状态基础上添加 STAGE 属性（默认值 0）
        this.registerDefaultState(this.defaultBlockState().setValue(STAGE, 0));
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

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STAGE);
    }

    /**
     * 点击时循环切换挡位
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        int currentStage = state.getValue(STAGE);
        int nextStage = (currentStage + 1) % 3;
        BlockState newState = state.setValue(STAGE, nextStage);
        level.setBlock(pos, newState, 3);
        level.updateNeighborsAt(pos, this);
        level.updateNeighborsAt(pos.relative(getConnectedDirection(newState).getOpposite()), this);
        // 不同挡位播放不同音高的拉杆声
        float pitch = nextStage == 0 ? 0.5F : (nextStage == 1 ? 0.6F : 0.7F);
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, pitch);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        int stage = state.getValue(STAGE);
        switch (stage) {
            case 0: return 0;
            case 1: return 8;
            case 2: return 15;
            default: return 0;
        }
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // 强信号只向"连接方向"输出（与 LeverBlock 一致），防止向四周泄漏；
        // 强度按档位输出（二档 8 / 三档 15），与 getSignal 保持一致。
        return getSignal(state, level, pos, direction) > 0
                && getConnectedDirection(state) == direction
                ? getSignal(state, level, pos, direction) : 0;
    }

    /**
     * 方块被移除时更新邻居（确保红石信号归零）
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!isMoving && !state.is(newState.getBlock())) {
            if (state.getValue(STAGE) > 0) {
                level.updateNeighborsAt(pos, this);
                level.updateNeighborsAt(pos.relative(getConnectedDirection(state).getOpposite()), this);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConsoleLever3StageBlockEntity(pos, state);
    }

    /**
     * 放置时若物品有自定义名称，则保存到方块实体用于文字渲染
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (stack.hasCustomHoverName()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ConsoleLever3StageBlockEntity leverBE) {
                leverBE.setText(stack.getHoverName());
            }
        }
    }
}