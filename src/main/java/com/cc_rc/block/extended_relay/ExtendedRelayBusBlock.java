package com.cc_rc.block.extended_relay;

import com.cc_rc.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

/**
 * 扩展红石继电器总线方块。
 *
 * 完整方块，可朝 x±/y±/z± 六个方向放置（FACING，与红石信号发射器 redstone_sender
 * 一致）；贴图为 bus 六面 + top/bottom 顶底。放置时面向玩家视线反方向。
 *
 * 总线是 CC: Tweaked 外设宿主（外设类型 "redstone_relay_bus"）：沿自身 FACING
 * 方向在任意距离搜索扩展红石继电器并对其读写红石信号（见
 * ExtendedRelayBusBlockEntity / ExtendedRelayBusPeripheral）。
 *
 * 红石行为（玻璃式）：
 *  - canConnectRedstone=false：红石线不连接本方块；
 *  - isRedstoneConductor=false（Properties StatePredicate 设置，1.20.1 无 Block 层
 *    可覆写方法）：本方块不作为红石导体（同玻璃，信号不穿过）。
 */
public class ExtendedRelayBusBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public ExtendedRelayBusBlock(Properties properties) {
        // 1.20.1 中 isRedstoneConductor 定义在 BlockBehaviour.Properties（StatePredicate 形式），
        // Block 层已无该可覆写方法；false = 玻璃式非导体（红石线不将其当作导电方块传递信号）
        super(properties.isRedstoneConductor((state, level, pos) -> false));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** 放置时朝向玩家视线反方向（面向玩家），支持六个方向。 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    /** 类似玻璃：总线自身不连接红石线、不传导；红石信号完全由 CC 外设读写继电器。 */
    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExtendedRelayBusBlockEntity(pos, state);
    }

    /**
     * 注册方块实体 tick：主线程每 tick 重建「distance → 继电器状态」快照缓存
     * （Lua 线程读该 volatile 缓存识别继电器，无延迟改造后不再实时查 Level）。
     */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.RELAY_BUS_BE.get()
                ? (l, p, s, be) -> ExtendedRelayBusBlockEntity.tick(l, p, s, (ExtendedRelayBusBlockEntity) be)
                : null;
    }
}