package com.cc_rc.block.audio_player;

import com.cc_rc.gui.AudioPlayerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * 音频播放器方块（audio_player）。
 *
 * 服务端只维护一个 POWERED 方块状态（是否被红石充能），客户端据此做边沿检测：
 *  - 触发模式：POWERED=true 时持续播放；
 *  - 反触发模式：POWERED=false 时持续播放；
 *  - 脉冲模式：POWERED 由 false → true 时播放一次；
 *  - 无红石模式：忽略 POWERED，始终播放。
 *
 * 玩家右键打开设置界面（AudioPlayerMenu → AudioPlayerScreen）。
 */
public class AudioPlayerBlock extends Block implements EntityBlock {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public AudioPlayerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AudioPlayerBlockEntity(pos, state);
    }

    /** 红石线可以连接本方块（视觉上连上，信号由 getBestNeighborSignal 读取）。 */
    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos,
                                      @Nullable Direction direction) {
        return true;
    }

    /** 邻居变化（红石线/充能方块状态改变）时刷新自身 POWERED。 */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            refreshPowered(state, level, pos);
        }
    }

    /** 放置时立即读取一次红石状态，避免放在已充能红石旁却显示未触发。 */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
                        boolean movedByPiston) {
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            refreshPowered(state, level, pos);
        }
    }

    /** 读取周围红石信号并更新 POWERED（flag=3：同步方块状态给客户端）。 */
    private static void refreshPowered(BlockState state, Level level, BlockPos pos) {
        boolean powered = level.getBestNeighborSignal(pos) > 0;
        if (state.getValue(POWERED) != powered) {
            level.setBlock(pos, state.setValue(POWERED, powered), 3);
        }
    }

    /** 右键打开音频播放器设置界面（Shift+右键不处理，方便取下/放置）。 */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof AudioPlayerBlockEntity be) {
            AudioPlayerMenu.open(serverPlayer, be);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
