package com.cc_rc.block.data_unit;

import com.cc_rc.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 数据单元方块
 *
 * 无方向完整方块（外观暂用原版书架模型/贴图占位，blockstates 直接引用
 * minecraft:block/bookshelf，后续替换专属模型时只需改 blockstates 与 item model）。
 *
 * 功能：
 *   - 完整方块碰撞箱，木质音效
 *   - 方块实体存「名称（字符串，可用编辑工具右键修改）」与「数据（整数列表，初始为空）」
 *   - 支持 CC: Tweaked 外设：读写名称、读写整个列表、读写列表某一位（Lua 索引 1 起）
 */
public class DataUnitBlock extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public DataUnitBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DataUnitBlockEntity(pos, state);
    }

    /** 注册方块实体 tick：用于 dirty 节流广播（每 tick 至多一次同步） */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.DATA_UNIT_BE.get()
                ? (l, p, s, be) -> DataUnitBlockEntity.tick(l, p, s, (DataUnitBlockEntity) be)
                : null;
    }

    /** 放置时若物品有自定义名称，则保存到方块实体的名称数据 */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (stack.hasCustomHoverName()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DataUnitBlockEntity unitBE) {
                unitBE.setText(stack.getHoverName());
            }
        }
    }
}
