package com.cc_rc.block.Point_lamp;

import com.cc_rc.block.console_panel.ConsolePanelBlock;
import com.cc_rc.block.console_panel.ConsolePanelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.item.context.BlockPlaceContext;

/**
 * 指示灯方块
 * 继承 ConsolePanelBlock 复用放置逻辑和文字显示
 * 额外添加 LIT 属性，被动接受红石信号控制亮灭
 * 不可点击交互
 */
public class PointLampBlock extends ConsolePanelBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public PointLampBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        // 父类找不到有效附着面时会返回 null（正常情况，交由 BlockItem 处理放置失败），不可继续 setValue
        if (state == null) {
            return null;
        }
        boolean hasSignal = context.getLevel().hasNeighborSignal(context.getClickedPos());
        return state.setValue(LIT, hasSignal);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            boolean hasSignal = level.hasNeighborSignal(pos);
            if (hasSignal != state.getValue(LIT)) {
                level.setBlock(pos, state.setValue(LIT, hasSignal), 2);
            }
        }
    }
}
