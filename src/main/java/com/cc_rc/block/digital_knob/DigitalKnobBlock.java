package com.cc_rc.block.digital_knob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 数字调节器方块
 *
 * 功能：
 *   - 完整方块碰撞箱（和数码显示器相同）
 *   - 可朝水平四方向（NORTH/SOUTH/EAST/WEST）放置
 *   - 前面水平分布四个按钮，点击后调整数值并切换对应模型1-4，2tick后恢复模型0
 *   - 可显示0~1000的整数，默认值0
 *   - 支持CC: Tweaked外设接口，可读写整数值（范围钳制0~1000）
 */
public class DigitalKnobBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty MODEL = IntegerProperty.create("model", 0, 4);

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public DigitalKnobBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MODEL, 0));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MODEL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection()).setValue(MODEL, 0);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DigitalKnobBlockEntity(pos, state);
    }

    /**
     * 放置时若物品有自定义名称，则保存到方块实体用于白色名称文字渲染
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (stack.hasCustomHoverName()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DigitalKnobBlockEntity knobBE) {
                knobBE.setText(stack.getHoverName());
            }
        }
    }

    /**
     * 右键交互：根据点击位置判断哪个按钮被点击，调整对应数值
     * 按钮按钮（屏幕局部坐标，从左到右）：
     *   按钮0(-20)：x:1~3  y:1~4
     *   按钮1(-1) ：x:5~7  y:1~4
     *   按钮2(+1) ：x:9~11 y:1~4
     *   按钮3(+20)：x:13~15 y:1~4
     * （宽2像素、高3像素、距底部1像素、距侧边1像素、水平间距2像素，与需求一致）
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && hand == InteractionHand.MAIN_HAND) {
            // 仅在正面（朝向玩家一侧）可交互，背面操作无效
            if (hit.getDirection() != state.getValue(FACING).getOpposite()) {
                return InteractionResult.PASS;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DigitalKnobBlockEntity knobBE) {
                int button = getButtonFromHit(state.getValue(FACING), hit.getLocation(), pos);
                if (button != -1) {
                    knobBE.adjustValue(button);
                    // 模型序号与按下的物理按钮对应：
                    //   模型1=按下最右按钮(+20)、模型2=(+1)、模型3=(=1)、模型4=按下最左按钮(-20)
                    // 按压玩家视角按钮 button(0左~3右) 时采用模型 button+1，
                    // 使凹陷按钮正好落在玩家所点击的位置（与朝向解耦，四个方向均一致）。
                    level.setBlock(pos, state.setValue(MODEL, button + 1), 3);
                    level.scheduleTick(pos, this, 2);
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }

    /**
     * 根据命中位置与朝向判断点击的按钮。
     * 返回 0~3（对应 -20,-1,+1,+20），非按钮区域返回 -1。
     */
    private int getButtonFromHit(Direction facing, Vec3 hitLoc, BlockPos pos) {
        double hitX = hitLoc.x - pos.getX();
        double hitY = hitLoc.y - pos.getY();
        double hitZ = hitLoc.z - pos.getZ();

        // 把命中点变换到“正面屏幕”局部坐标：屏幕中心朝外，屏幕面上的 x 为局部水平、y 为屏幕竖向。
        // 局部坐标统一为 0~16（左→右、下→上），与贴图/模型元素坐标一致。
        double localY = hitY * 16.0;

        // Y 范围检查：按钮高 3 像素，距底部 1 像素 => y 1~4
        if (localY < 1.0 || localY > 4.0) {
            return -1;
        }

        double localX;
        switch (facing) {
            case NORTH -> {
                // 面朝北：玩家左侧为西(-x)，按钮0(-20)在最左
                localX = hitX * 16.0;
            }
            case SOUTH -> {
                // 面朝南：玩家左侧为东(+x)
                localX = (1.0 - hitX) * 16.0;
            }
            case EAST -> {
                // 面朝东：玩家左侧为北(-z)
                localX = hitZ * 16.0;
            }
            case WEST -> {
                // 面朝西：玩家左侧为南(+z)
                localX = (1.0 - hitZ) * 16.0;
            }
            default -> {
                return -1;
            }
        }

        // 四个按钮分段判断
        if (localX >= 1.0 && localX <= 3.0) return 0;   // -20
        if (localX >= 5.0 && localX <= 7.0) return 1;   // -1
        if (localX >= 9.0 && localX <= 11.0) return 2;  // +1
        if (localX >= 13.0 && localX <= 15.0) return 3; // +20
        return -1;
    }

    /** 2tick 后恢复模型0 */
    @Override
    public void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(MODEL, 0), 3);
        }
    }
}