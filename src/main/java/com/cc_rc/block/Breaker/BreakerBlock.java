package com.cc_rc.block.Breaker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 断路器方块
 *
 * 功能：
 *   - 完整方块碰撞箱，可朝水平四方向（NORTH/SOUTH/EAST/WEST）放置
 *   - 两个状态：on（通电）/off（断电），初始放置为 off
 *   - 右键点击切换状态（手动合闸/分闸）
 *   - on 状态向"后方"和"上方"发出 15 的红石信号
 *       （后方 = 朝向的反方向，即 facing.getOpposite()）
 *   - 底面接收到红石信号时自动跳闸（on → off）
 *
 * 红石逻辑：
 *   - getSignal(state, level, pos, direction)：direction 是信号输出方向
 *       on 状态下，direction == 后方 或 direction == UP 时返回 15
 *   - neighborChanged：检查底面信号 level.getSignal(pos.below(), UP)
 *       若 > 0 且当前 on，则跳闸为 off
 */
public class BreakerBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public BreakerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    /** 放置时根据玩家朝向设置 FACING（方块朝向玩家面对的方向） */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    /** 右键切换 on/off */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (player.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            boolean newPowered = !state.getValue(POWERED);
            // 先切换状态
            level.setBlock(pos, state.setValue(POWERED, newPowered), 3);
            // 播放声音（合闸高音，分闸低音）
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
                    0.3F, newPowered ? 0.6F : 0.5F);

            // 若切换到 on，检查底面信号，有信号则立即跳闸
            if (newPowered) {
                int signalFromBelow = level.getSignal(pos.below(), Direction.UP);
                if (signalFromBelow > 0) {
                    level.setBlock(pos, state.setValue(POWERED, false), 3);
                    level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
                            0.3F, 0.5F);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** 是红石信号源 */
    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    /**
     * 输出红石信号
     * @param direction 信号输出方向
     * on 状态：向"后方"（facing.getOpposite()）和"上方"输出 15
     */
    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (!state.getValue(POWERED)) {
            return 0;
        }
        Direction back = state.getValue(FACING).getOpposite();
        if (direction == back || direction == Direction.UP) {
            return 15;
        }
        return 0;
    }

    /** 邻居改变时检查底面信号，有信号则跳闸 */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                 BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && state.getValue(POWERED)) {
            int signalFromBelow = level.getSignal(pos.below(), Direction.UP);
            if (signalFromBelow > 0) {
                // 底面收到信号，自动跳闸
                level.setBlock(pos, state.setValue(POWERED, false), 3);
                level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
                        0.3F, 0.5F);
            }
        }
    }
}
