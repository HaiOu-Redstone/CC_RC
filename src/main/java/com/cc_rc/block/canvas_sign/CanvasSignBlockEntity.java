package com.cc_rc.block.canvas_sign;

import com.cc_rc.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 粗布告示牌方块实体（立式/壁挂共用）。
 * 继承原版 SignBlockEntity，但 getType 返回自定义方块实体类型 CANVAS_SIGN_BE，
 * 该类型的 validBlocks 含本模组的粗布告示牌方块，从而通过渲染调度器的 isValid 校验。
 */
public class CanvasSignBlockEntity extends SignBlockEntity {
    public CanvasSignBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModBlockEntities.CANVAS_SIGN_BE.get();
    }
}
