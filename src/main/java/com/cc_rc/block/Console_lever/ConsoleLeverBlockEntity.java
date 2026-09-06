package com.cc_rc.block.Console_lever;

import com.cc_rc.ModBlockEntities;
import com.cc_rc.block.console_panel.ConsolePanelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 控制台拉杆方块实体
 * 继承 ConsolePanelBlockEntity 复用文字存储/同步逻辑
 */
public class ConsoleLeverBlockEntity extends ConsolePanelBlockEntity {
    public ConsoleLeverBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONSOLE_LEVER_BE.get(), pos, state);
    }
}
