package com.mao.barbequesdelight.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public interface BlockSlot {

	default float getOffset(int index) {
		return -((index + 0.5f) / size() - 0.5f) * 0.8f;
	}

	AABB getBox();

	int size();

	default int getSlotForHitting(BlockHitResult hit, Level level) {
		if (hit.getType() != HitResult.Type.BLOCK)
			return -1;
		Vec3 pos1 = hit.getLocation();
		if (!getBox().contains(pos1))
			return -1;
		Direction facing = level.getBlockState(hit.getBlockPos())
				.getValue(HorizontalDirectionalBlock.FACING).getOpposite();
		BlockPos pos = hit.getBlockPos();
		int size = size();
		double index = 0;
		switch (facing) {
			case NORTH -> index = pos1.x - pos.getX();
			case SOUTH -> index = 1 - (pos1.x - pos.getX());
			case EAST -> index = pos1.z - pos.getZ();
			case WEST -> index = 1 - (pos1.z - pos.getZ());
		}
		int ans = (int) (index * size);
		return ans >= 0 && ans < size ? ans : -1;
	}

}
