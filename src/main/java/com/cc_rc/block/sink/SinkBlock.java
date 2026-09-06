package com.cc_rc.block.sink;

import com.cc_rc.ModRecipes;
import com.cc_rc.recipe.SinkConversionRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 水槽（搬运自 Cooking for Blockheads）
 *
 * 放置：水平四方向（模型正面水龙头/柜门位于 -Z 侧，朝向 = 玩家水平朝向的反方向，使正面面对玩家）
 * 功能：右键手持物品，通过 RecipeManager 查询 cc_rc:sink_conversion 类型配方执行四向转换：
 * 桶→水桶、玻璃瓶→水瓶、水桶→桶、水瓶→玻璃瓶（配方文件在 data/cc_rc/recipes/，支持输入/输出 NBT）。
 * 无方块实体，无限水源。
 * 渲染：模型非完整 16x16x16 方块，覆写 getOcclusionShape 返回空形状，
 * 使相邻完整方块的面不被剔除（同玻璃），避免缝隙透视。
 */
public class SinkBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public SinkBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 正面位于模型 -Z 侧：取玩家水平朝向的反方向，使放置后正面对玩家（同原版熔炉逻辑）
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        // 空遮挡形状：不剔除相邻完整方块朝向本方的面（同玻璃），缝隙处不再透视
        return Shapes.empty();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.isEmpty()) {
            return InteractionResult.PASS;
        }

        // 通过配方管理器查询匹配的转换配方（数据驱动，配方在 data/cc_rc/recipes/）
        RecipeManager recipeManager = level.getRecipeManager();
        SinkConversionRecipe recipe = null;
        for (SinkConversionRecipe candidate : recipeManager.getAllRecipesFor(ModRecipes.SINK_CONVERSION_TYPE.get())) {
            if (candidate.matchesItem(heldItem)) {
                recipe = candidate;
                break;
            }
        }

        if (recipe == null) {
            return InteractionResult.PASS;
        }

        // 转换产物：output.copy()（含输出 NBT，如水瓶的 Potion 标签）
        ItemStack result = recipe.getOutputCopy();

        if (!level.isClientSide) {
            // 消耗 1 个输入：堆叠数为 1 时直接替换手持；否则放入背包再扣减
            if (heldItem.getCount() == 1) {
                player.setItemInHand(hand, result);
            } else if (player.getInventory().add(result)) {
                heldItem.shrink(1);
            } else {
                // 背包已满，无法获得产物，不消耗输入物品
                return InteractionResult.FAIL;
            }

            // 依据输入/输出类型选择音效：桶类用装/倒水桶音，瓶类用装/倒水瓶音
            boolean isBucketLike = heldItem.is(Items.BUCKET) || heldItem.is(Items.WATER_BUCKET);
            boolean isFilling = result.is(Items.WATER_BUCKET) || result.is(Items.POTION);
            SoundEvent sound = isBucketLike
                    ? (isFilling ? SoundEvents.BUCKET_FILL : SoundEvents.BUCKET_EMPTY)
                    : (isFilling ? SoundEvents.BOTTLE_FILL : SoundEvents.BOTTLE_EMPTY);
            level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}