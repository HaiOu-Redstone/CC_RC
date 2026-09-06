package com.cc_rc.block.Console_lever;

import com.cc_rc.ModBlockEntities;
import com.cc_rc.block.console_panel.ConsolePanelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 3挡位控制台拉杆方块实体
 * 继承 ConsolePanelBlockEntity 复用文字存储/同步逻辑
 * 用于 console_lever_6 和 console_lever_7
 */
public class ConsoleLever3StageBlockEntity extends ConsolePanelBlockEntity {
    public ConsoleLever3StageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONSOLE_LEVER_3STAGE_BE.get(), pos, state);
    }
}
