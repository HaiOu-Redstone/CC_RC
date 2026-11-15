package com.cc_rc.block.console;

import com.cc_rc.gui.ConsoleProgramMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 控制柜辅助计算机。
 *
 * 纯装饰性方块，没有独立方块实体：所有变量与程序数据都保存在相邻的
 * 「控制柜总线」（controller_bus）里，这里只作为打开程序界面的入口。
 *
 * 水平四向可放置（FACING = 正面朝向，放置时朝向玩家），用于让屏幕面板
 * 对准操作方向；不同朝向只影响模型旋转，不影响总线扫描逻辑。
 *
 * 右键时扫描六邻方块：
 *  - 恰好一个总线：打开该总线的程序界面；
 *  - 没有总线：提示需要紧邻总线；
 *  - 多个总线：提示只保留一个，避免数据来源不确定。
 */
public class ControllerAuxBlock extends Block {

    /** 正面朝向（水平四向），由放置时玩家朝向决定。 */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /**
     * 外观颜色：跟随相邻总线（优先）或相邻控制柜结构方块的颜色，
     * 只用于切换贴图，功能与默认色完全一致。
     */
    public static final EnumProperty<ConsoleColor> COLOR =
            EnumProperty.create("color", ConsoleColor.class);

    public ControllerAuxBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(COLOR, ConsoleColor.DEFAULT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, COLOR);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 只保留水平四向：取玩家朝向的反方向，使正面朝向玩家
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** 放置后按邻居更新一次颜色（放置时方块已就位，可安全扫描六邻）。 */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
                        boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        updateColor(level, pos, state);
    }

    /**
     * 邻居变化（总线成型换色 / 结构方块增减）时跟随换色。
     * 只改自己的方块状态，不再触发邻居更新，避免连锁刷新。
     */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        updateColor(level, pos, state);
    }

    /**
     * 推断并写入外观颜色：优先取相邻总线的颜色，其次取相邻结构方块的颜色，
     * 都没有则用默认色。颜色未变化时不做任何写入。
     */
    private static void updateColor(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) {
            return;
        }
        ConsoleColor color = colorFromNeighbors(level, pos);
        if (state.getValue(COLOR) != color) {
            level.setBlock(pos, state.setValue(COLOR, color), 2);
        }
    }

    /** 六邻扫描：先找总线（含未成型总线，其颜色为默认色），再找结构方块。 */
    private static ConsoleColor colorFromNeighbors(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(dir));
            if (neighbor.getBlock() instanceof ConsoleBusBlock
                    && neighbor.hasProperty(ConsoleBusBlock.COLOR)) {
                return neighbor.getValue(ConsoleBusBlock.COLOR);
            }
        }
        for (Direction dir : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(dir));
            if (neighbor.getBlock() instanceof ConsoleBlock console) {
                return console.color;
            }
        }
        return ConsoleColor.DEFAULT;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        // 客户端直接返回成功，避免重复处理
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        // 扫描六邻，找出唯一的相邻总线
        ConsoleBusBlockEntity bus = null;
        int count = 0;
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(pos.relative(dir)) instanceof ConsoleBusBlockEntity found) {
                bus = found;
                count++;
            }
        }
        if (count == 0) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.cc_rc.controller.need_bus"), true);
            return InteractionResult.CONSUME;
        }
        if (count > 1) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.cc_rc.controller.multi_bus"), true);
            return InteractionResult.CONSUME;
        }

        ConsoleProgramMenu.open(serverPlayer, bus);
        return InteractionResult.CONSUME;
    }
}
