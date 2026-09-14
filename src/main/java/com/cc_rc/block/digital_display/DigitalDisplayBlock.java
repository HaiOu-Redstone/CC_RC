package com.cc_rc.block.digital_display;

import com.cc_rc.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

/**
 * 数码显示器方块
 *
 * 功能：
 *   - 完整方块碰撞箱（继承 Block 默认全方块形）
 *   - 可朝水平四方向（NORTH/SOUTH/EAST/WEST）放置，屏幕朝向放置时玩家面对的方向
 *   - 放置时若物品有自定义名称，保存到方块实体作为第一行白色文字
 *   - 第二行固定橙色文字（默认 "----"，可通过 setStatus 修改）
 */
public class DigitalDisplayBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public DigitalDisplayBlock(Properties properties) {
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
        return new DigitalDisplayBlockEntity(pos, state);
    }

    /** 注册方块实体 tick：用于 dirty 节流广播（每 tick 至多一次同步） */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.DIGITAL_DISPLAY_BE.get()
                ? (l, p, s, be) -> DigitalDisplayBlockEntity.tick(l, p, s, (DigitalDisplayBlockEntity) be)
                : null;
    }

    /**
     * 放置时若物品有自定义名称，则保存到方块实体用于第一行白色文字渲染
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (stack.hasCustomHoverName()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DigitalDisplayBlockEntity displayBE) {
                displayBE.setText(stack.getHoverName());
            }
        }
    }
}