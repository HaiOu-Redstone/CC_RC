package com.cc_rc.block.console;

import com.cc_rc.ModBlockEntities;
import com.cc_rc.gui.ConsoleMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 控制台总线方块。
 *
 * 水平四向可放置（FACING = 玩家放置时面向的水平方向的反方向，即总线背面），
 * 需要与任意一个控制台结构方块相邻。右键打开总线设置界面：
 * 校验结构、成型/锁定、查看尺寸。
 *
 * FACING 同时决定结构顶面/底面的文字「头朝向」：
 *  - 顶面文字的上方 = 总线背面方向（-FACING）；
 *  - 底面文字的上方同样取自 FACING，只是左右（U 轴）取反。
 */
public class ConsoleBusBlock extends Block implements EntityBlock {

    /** 总线背面方向（水平四向），决定顶/底面文字的朝向。 */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /**
     * 总线外观颜色：成型时由识别到的结构颜色写入（详见 ConsoleBusBlockEntity#formStructure），
     * 仅用于切换方块模型贴图，不影响任何功能。
     */
    public static final EnumProperty<ConsoleColor> COLOR =
            EnumProperty.create("color", ConsoleColor.class);

    public ConsoleBusBlock(Properties properties) {
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
        // 只保留水平四向：取玩家朝向的反方向作为总线背面
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConsoleBusBlockEntity(pos, state);
    }

    /** 服务端低频巡检结构是否仍然完整（爆炸/指令等外部改动会自动解除成型）。 */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        return level.isClientSide ? null
                : (type == ModBlockEntities.CONSOLE_BUS_BE.get()
                ? (l, p, s, be) -> ConsoleBusBlockEntity.tick(l, p, s, (ConsoleBusBlockEntity) be)
                : null);
    }

    /** 右键：优先操作命中的组件；否则（非潜行）打开总线设置界面。 */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        // 空手点中组件时执行组件操作（开关 / 按钮 / 弧形仪表）
        if (player.getItemInHand(hand).isEmpty()
                && ConsoleCompUse.tryUse(level, player, pos, hit)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // 手持控制柜钥匙时不拦截，交给物品的 useOn 处理（插入钥匙开关）
        if (player.getItemInHand(hand).getItem() instanceof com.cc_rc.item.ConsoleKeyItem) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof ConsoleBusBlockEntity be) {
            ConsoleMenu.open(serverPlayer, be);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * 总线被玩家拆除时立即解除成型：清空成员标记并同步客户端，避免成员残留网格。
     * 必须在方块实体仍存在时处理（playerWillDestroy 阶段）。
     */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ConsoleBusBlockEntity bus) {
            bus.invalidateStructure();
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    /** 兜底：非玩家途径（活塞/指令等）移除总线时同样解除成型。 */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof ConsoleBusBlockEntity bus) {
            bus.invalidateStructure();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
