package com.cc_rc.block.digital_plotter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

/**
 * 数字圆盘记录仪方块
 *
 * 功能：
 *   - 完整方块碰撞箱（继承 Block 默认全方块形），复用圆盘记录仪模型
 *   - 可朝水平四方向（NORTH/SOUTH/EAST/WEST）放置，屏幕朝向放置时玩家面对的方向（同数码显示器）
 *   - 放置时若物品有自定义名称，保存到方块实体作为命名文字（白色，无数值文字）
 *   - 正面渲染红色趋势线图（50 点，值 0~100），无额外状态（不自动采样）
 */
public class DigitalPlotterBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public DigitalPlotterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** 放置时根据玩家朝向设置 FACING，屏幕朝向玩家面对的方向 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DigitalPlotterBlockEntity(pos, state);
    }

    /**
     * 放置时若物品有自定义名称，则保存到方块实体用于命名文字渲染
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (stack.hasCustomHoverName()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DigitalPlotterBlockEntity plotterBE) {
                plotterBE.setText(stack.getHoverName());
            }
        }
    }
}
