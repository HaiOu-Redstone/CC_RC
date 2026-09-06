package com.cc_rc.block.canvas_sign;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;

/**
 * 壁挂悬挂式粗布告示牌方块：复用原版 WallHangingSignBlock 的壁挂悬挂放置逻辑，
 * newBlockEntity 返回自定义 CanvasHangingSignBlockEntity（同天花板悬挂，走自定义方块实体类型）。
 */
public class CanvasWallHangingSignBlock extends WallHangingSignBlock {
    public CanvasWallHangingSignBlock(Properties properties, WoodType type) {
        super(properties, type);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CanvasHangingSignBlockEntity(pos, state);
    }
}
