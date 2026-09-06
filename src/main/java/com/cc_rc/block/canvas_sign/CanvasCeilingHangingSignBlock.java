package com.cc_rc.block.canvas_sign;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;

/**
 * 天花板悬挂式粗布告示牌方块：复用原版 CeilingHangingSignBlock 的悬挂放置逻辑，
 * newBlockEntity 返回自定义 CanvasHangingSignBlockEntity（getType 返回自定义方块实体类型）。
 */
public class CanvasCeilingHangingSignBlock extends CeilingHangingSignBlock {
    public CanvasCeilingHangingSignBlock(Properties properties, WoodType type) {
        super(properties, type);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CanvasHangingSignBlockEntity(pos, state);
    }
}
