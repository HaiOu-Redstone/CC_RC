package com.cc_rc.block.block_detector;

import com.cc_rc.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 方块探测器方块实体
 *
 * 仅作为 CC 外设宿主存在，不存储任何数据：探测器是「只读探测」设备，
 * 面向方块的信息由外设方法在**主线程**实时读取世界（Level 非线程安全，
 * 且需要访问服务端方块实体/区块加载状态），因此本方块实体无 tick。
 */
public class BlockDetectorBlockEntity extends BlockEntity {

    public BlockDetectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLOCK_DETECTOR_BE.get(), pos, state);
    }
}
