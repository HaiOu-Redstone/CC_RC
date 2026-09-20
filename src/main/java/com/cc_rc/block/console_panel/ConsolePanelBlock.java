package com.cc_rc.block.console_panel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 控制面板方块基类
 * 仅包含放置逻辑、碰撞箱和文字显示，无交互或红石相关代码
 * 指示灯、仪表等方块可继承此类复用放置和文字显示功能
 */
public class ConsolePanelBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock {
    // 14x14x3 碰撞箱
    public static final VoxelShape FLOOR_SHAPE = Block.box(1, 0, 1, 15, 3, 15);
    public static final VoxelShape CEILING_SHAPE = Block.box(1, 13, 1, 15, 16, 15);
    public static final VoxelShape WALL_NORTH_SHAPE = Block.box(1, 1, 13, 15, 15, 16);
    public static final VoxelShape WALL_SOUTH_SHAPE = Block.box(1, 1, 0, 15, 15, 3);
    public static final VoxelShape WALL_EAST_SHAPE = Block.box(0, 1, 1, 3, 15, 15);
    public static final VoxelShape WALL_WEST_SHAPE = Block.box(13, 1, 1, 16, 15, 15);

    public ConsolePanelBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE);
    }

    /**
     * 获取碰撞箱，所有继承此类的方块共用
     */
    public static VoxelShape getPanelShape(BlockState state) {
        AttachFace face = state.getValue(FACE);
        Direction facing = state.getValue(FACING);
        return switch (face) {
            case FLOOR -> FLOOR_SHAPE;
            case CEILING -> CEILING_SHAPE;
            case WALL -> switch (facing) {
                case NORTH -> WALL_NORTH_SHAPE;
                case SOUTH -> WALL_SOUTH_SHAPE;
                case EAST -> WALL_EAST_SHAPE;
                case WEST -> WALL_WEST_SHAPE;
                default -> WALL_NORTH_SHAPE;
            };
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getPanelShape(state);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConsolePanelBlockEntity(pos, state);
    }

    // ================= 染料染色（面板文字着色） =================

    /**
     * 判断该交互是否为「手持染料右击面板染色」：目标方块实体是控制面板类
     * （ConsolePanelBlockEntity 及其子类），且手中物品是染料。
     * 客户端用于预测返回成功、服务端用于实际执行染色，两端判断必须一致。
     */
    public static boolean isDyeTarget(Level level, BlockPos pos, ItemStack stack) {
        return stack.getItem() instanceof DyeItem
                && level.getBlockEntity(pos) instanceof ConsolePanelBlockEntity;
    }

    /**
     * 服务端执行染色：把面板文字颜色覆写为染料对应颜色，并播放音效。
     * 不消耗染料（编辑工具 GUI 已能直接改颜色，染料染色仅作快捷手段）。
     * 仅应在服务端调用（修改方块实体数据的权威写入）。
     */
    public static void applyDye(Level level, BlockPos pos, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof ConsolePanelBlockEntity be) {
            be.applyDyeColor(((DyeItem) stack.getItem()).getDyeColor());
        }
        level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 0.3F, 1.0F);
    }

    /**
     * 统一的染料染色入口：手持染料右键面板时调用（基类 use 与按钮类 use 共用）。
     * 客户端仅返回成功（不做修改，等待服务端权威同步），服务端实际执行染色。
     *
     * @return true 表示本次右键已被染色处理，调用方应直接结束交互；false 表示不是染色操作。
     */
    public static boolean handleDyeInteraction(Level level, BlockPos pos, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isDyeTarget(level, pos, stack)) return false;
        if (!level.isClientSide) {
            applyDye(level, pos, stack);
        }
        return true;
    }

    @Deprecated
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        // 手持染料右键面板 → 染色（按钮类子类覆写了 use，需在各自 use 中调用 handleDyeInteraction）
        if (handleDyeInteraction(level, pos, player, hand)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    /**
     * 放置时若物品有自定义名称，则保存到方块实体用于文字渲染
     * 所有继承此类的方块自动获得此功能
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (stack.hasCustomHoverName()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ConsolePanelBlockEntity panelBE) {
                panelBE.setText(stack.getHoverName());
            }
        }
    }
}
