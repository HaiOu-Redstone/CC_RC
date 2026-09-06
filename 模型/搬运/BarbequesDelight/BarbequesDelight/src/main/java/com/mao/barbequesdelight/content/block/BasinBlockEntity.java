package com.mao.barbequesdelight.content.block;

import dev.xkmc.l2library.base.tile.BaseBlockEntity;
import dev.xkmc.l2modularblock.tile_api.BlockContainer;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

@SerialClass
public class BasinBlockEntity extends StorageTileBlockEntity {

	public BasinBlockEntity(BlockEntityType<? extends BasinBlockEntity> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		items.add(this);
	}

	public int size() {
		return 2;
	}

	@Override
	public AABB getBox() {
		return BasinBlock.OUTER.bounds().move(getBlockPos()).deflate(0.01f);
	}

	@Override
	public boolean specialClick(Player player, int i, InteractionHand hand) {
		return false;
	}

}