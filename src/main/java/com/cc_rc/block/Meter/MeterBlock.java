package com.cc_rc.block.Meter;

import com.cc_rc.block.console_panel.ConsolePanelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.item.context.BlockPlaceContext;

/**
 * 仪表方块
 * 继承 ConsolePanelBlock 复用放置逻辑和文字显示
 * 额外添加 POWER 属性(0-15)，被动接受红石信号强度
 * 不可点击交互
 */
public class MeterBlock extends ConsolePanelBlock {
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    public MeterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE, POWER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        // 父类找不到有效附着面时会返回 null（正常情况，交由 BlockItem 处理放置失败），不可继续 setValue
        if (state == null) {
            return null;
        }
        int signal = context.getLevel().getBestNeighborSignal(context.getClickedPos());
        return state.setValue(POWER, signal);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            int signal = level.getBestNeighborSignal(pos);
            int current = state.getValue(POWER);
            if (signal != current) {
                level.setBlock(pos, state.setValue(POWER, signal), 2);
            }
        }
    }
}
