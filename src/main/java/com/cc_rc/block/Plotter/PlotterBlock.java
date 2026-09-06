package com.cc_rc.block.Plotter;

import com.cc_rc.block.console_panel.ConsolePanelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 圆盘记录仪方块
 * 继承 ConsolePanelBlock 复用放置逻辑、碰撞箱和文字显示
 * 被动读取红石信号，按模式间隔将信号写入数据列表并渲染趋势图
 * 使用 MULTI_TOOL 右键切换模式（逻辑在 MultiToolItem.useOn）
 */
public class PlotterBlock extends ConsolePanelBlock {

    public PlotterBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlotterBlockEntity(pos, state);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(state.getBlock())) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isClientSide) return;
        if (!(level.getBlockEntity(pos) instanceof PlotterBlockEntity be)) return;

        be.tick();
        level.scheduleTick(pos, this, 1);
    }
}
