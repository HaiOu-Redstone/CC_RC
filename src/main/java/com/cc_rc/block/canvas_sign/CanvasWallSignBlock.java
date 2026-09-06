package com.cc_rc.block.canvas_sign;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;

/**
 * 壁挂式粗布告示牌方块：复用原版 WallSignBlock 的壁挂放置逻辑，
 * newBlockEntity 返回自定义 CanvasSignBlockEntity（同立式，走自定义方块实体类型）。
 */
public class CanvasWallSignBlock extends WallSignBlock {
    public CanvasWallSignBlock(Properties properties, WoodType type) {
        super(properties, type);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CanvasSignBlockEntity(pos, state);
    }
}
