package com.cc_rc.block.nuke_button;

import com.cc_rc.ModItems;
import com.cc_rc.block.console_panel.ConsolePanelBlock;
import com.cc_rc.block.console_panel.ConsolePanelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 核弹按钮方块
 * 继承 ConsolePanelBlock 复用放置逻辑、碰撞箱和文字显示（放置方式同控制面板）
 * 6 个状态（IntegerProperty state 1~6），输出对应强度的红石信号：
 *   1 = 初始（放置时）：无信号 0
 *   2 = 已插入钥匙1：信号 5
 *   3 = 已插入钥匙2（状态2-2）：信号 7
 *   4 = 双钥匙齐（状态3）：信号 10
 *   5 = 待发（状态4，shift 与状态3互相切换）：信号 10
 *   6 = 发射（状态5，信号 15，3 秒后自动回到状态1）
 * 状态转移：
 *   状态1 + 钥匙1 → 状态2；状态1 + 钥匙2 → 状态3（均消耗钥匙）
 *   状态2 + 钥匙2 → 状态4；状态3 + 钥匙1 → 状态4（均消耗钥匙）
 *   状态4/5 之间由 shift+右键互相切换；状态5 普通右键 → 状态6
 */
public class NukeButtonBlock extends ConsolePanelBlock {
    public static final IntegerProperty STATE = IntegerProperty.create("state", 1, 6);

    private static final int STATE1_IDLE = 1;    // 初始
    private static final int STATE2_KEY1 = 2;    // 插钥匙1
    private static final int STATE3_KEY2 = 3;    // 插钥匙2（2-2）
    private static final int STATE4_BOTH = 4;    // 双钥匙（状态3）
    private static final int STATE5_ARMED = 5;   // 待发（状态4）
    private static final int STATE6_LAUNCH = 6;  // 发射（状态5）

    private static final int LAUNCH_TICKS = 60;  // 发射后 3 秒回初始

    public NukeButtonBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(STATE, STATE1_IDLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE, STATE);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        int st = state.getValue(STATE);
        ItemStack held = player.getItemInHand(hand);
        boolean isKey1 = held.is(ModItems.KEY_1.get());
        boolean isKey2 = held.is(ModItems.KEY_2.get());

        // 钥匙插入：消耗钥匙并推进状态
        if (isKey1 || isKey2) {
            int target = 0;
            if (isKey1) {
                if (st == STATE1_IDLE) {
                    target = STATE2_KEY1;
                } else if (st == STATE3_KEY2) {
                    target = STATE4_BOTH;
                }
            } else {
                if (st == STATE1_IDLE) {
                    target = STATE3_KEY2;
                } else if (st == STATE2_KEY1) {
                    target = STATE4_BOTH;
                }
            }
            if (target != 0) {
                held.shrink(1); // 消耗钥匙（仅服务端执行）
                transition(state, level, pos, target);
            }
            return InteractionResult.CONSUME;
        }

        // 空手交互：状态3/4 用 shift 互相切换；状态4 普通右键触发发射
        if (player.isShiftKeyDown()) {
            if (st == STATE4_BOTH) {
                transition(state, level, pos, STATE5_ARMED);
            } else if (st == STATE5_ARMED) {
                transition(state, level, pos, STATE4_BOTH);
            }
        } else {
            if (st == STATE5_ARMED) {
                transition(state, level, pos, STATE6_LAUNCH);
                level.scheduleTick(pos, this, LAUNCH_TICKS); // 3 秒后由 tick 复位
            }
        }
        return InteractionResult.CONSUME;
    }

    /** 状态切换：写方块、播放音效、刷新红石邻居 */
    private void transition(BlockState state, Level level, BlockPos pos, int target) {
        BlockState next = state.setValue(STATE, target);
        level.setBlock(pos, next, 3);
        if (target == STATE6_LAUNCH) {
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.5F, 0.5F);
        } else {
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3F, 0.6F);
        }
        updateNeighbours(next, level, pos);
        if (level.getBlockEntity(pos) instanceof ConsolePanelBlockEntity be) {
            be.setChanged();
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 状态6（发射）计时结束 → 复位为状态1
        if (state.getValue(STATE) == STATE6_LAUNCH) {
            transition(state, level, pos, STATE1_IDLE);
        }
    }

    /** 各状态对应的红石信号强度：1→0，2→5，3→7，4/5→10，6→15 */
    private int signalOf(int st) {
        return switch (st) {
            case STATE2_KEY1 -> 5;
            case STATE3_KEY2 -> 7;
            case STATE4_BOTH, STATE5_ARMED -> 10;
            case STATE6_LAUNCH -> 15;
            default -> 0;
        };
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // 弱信号全方向 = 当前状态信号强度
        return signalOf(state.getValue(STATE));
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // 连接方向强充能（类似按钮/刷卡机，使贴墙方块被强充能）
        int sig = signalOf(state.getValue(STATE));
        return sig > 0 && getConnectedDirection(state) == direction ? sig : 0;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    private void updateNeighbours(BlockState state, Level level, BlockPos pos) {
        // LeverBlock 字节码金标准：刷新自身与连接方向反向邻居
        Direction dir = getConnectedDirection(state);
        level.updateNeighborsAt(pos, this);
        level.updateNeighborsAt(pos.relative(dir.getOpposite()), this);
    }

    /**
     * 原版 FaceAttachedHorizontalDirectionalBlock#getConnectedDirection 字节级真值表：
     * FLOOR   → UP
     * CEILING → DOWN
     * WALL    → FACING 本身
     */
    public static Direction getConnectedDirection(BlockState state) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> Direction.UP;
            case CEILING -> Direction.DOWN;
            case WALL -> state.getValue(FACING);
        };
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!isMoving && !state.is(newState.getBlock())) {
            if (signalOf(state.getValue(STATE)) > 0) {
                updateNeighbours(state, level, pos);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}