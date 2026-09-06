package com.cc_rc.block.console_panel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 方块定时器到期回调接口
 * 由需要定时功能的方块实现；由方块自身的 tick() 心跳在倒计时归零时调用
 * （例如 SafeButtonBlock 的 1-tick 心跳链，见 SafeButtonBlock#tick）
 */
public interface TimerExpiredHandler {
    void onTimerExpired(BlockState state, Level level, BlockPos pos);
}
