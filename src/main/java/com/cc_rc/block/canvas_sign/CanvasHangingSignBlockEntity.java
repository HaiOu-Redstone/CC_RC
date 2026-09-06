package com.cc_rc.block.canvas_sign;

import com.cc_rc.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 悬挂式粗布告示牌方块实体（天花板悬挂/壁挂悬挂共用）。
 * 继承原版 HangingSignBlockEntity，但 getType 返回自定义方块实体类型 CANVAS_HANGING_SIGN_BE，
 * 该类型的 validBlocks 含本模组的悬挂式粗布告示牌方块，从而通过渲染调度器的 isValid 校验。
 */
public class CanvasHangingSignBlockEntity extends HangingSignBlockEntity {
    public CanvasHangingSignBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModBlockEntities.CANVAS_HANGING_SIGN_BE.get();
    }
}
