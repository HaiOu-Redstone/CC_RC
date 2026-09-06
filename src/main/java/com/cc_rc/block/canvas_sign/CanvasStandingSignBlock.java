package com.cc_rc.block.canvas_sign;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;

/**
 * 立式粗布告示牌方块：复用原版 StandingSignBlock 的立式放置逻辑，
 * 但 newBlockEntity 返回自定义 CanvasSignBlockEntity（getType 返回自定义方块实体类型，
 * 使渲染调度器能通过 isValid 校验并正确渲染）。
 */
public class CanvasStandingSignBlock extends StandingSignBlock {
    public CanvasStandingSignBlock(Properties properties, WoodType type) {
        super(properties, type);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CanvasSignBlockEntity(pos, state);
    }
}
