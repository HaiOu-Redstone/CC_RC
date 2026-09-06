package com.mao.barbequesdelight.content.block;

import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

@SerialClass
public class TrayBlockEntity extends StorageTileBlockEntity {

	public TrayBlockEntity(BlockEntityType<? extends TrayBlockEntity> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		items.add(this);
	}

	public int size() {
		return 3;
	}

	@Override
	public AABB getBox() {
		return TrayBlock.OUTER.bounds().move(getBlockPos()).deflate(0.01f);
	}

	@Override
	public boolean specialClick(Player player, int i, InteractionHand hand) {
		return false;
	}

}
