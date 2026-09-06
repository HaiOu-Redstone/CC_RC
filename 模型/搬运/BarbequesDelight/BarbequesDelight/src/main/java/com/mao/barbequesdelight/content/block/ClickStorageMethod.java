package com.mao.barbequesdelight.content.block;

import dev.xkmc.l2modularblock.mult.OnClickBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public record ClickStorageMethod() implements OnClickBlockMethod {

	public static final ClickStorageMethod INS = new ClickStorageMethod();

	@Override
	public InteractionResult onClick(BlockState blockState, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof StorageTile basin))
			return InteractionResult.PASS;
		int i = basin.getSlotForHitting(hit, level);
		if (i < 0 || i >= basin.size())
			return InteractionResult.PASS;
		ItemStack handStack = player.getItemInHand(hand);
		if (basin.insert(level, i, handStack, !player.isShiftKeyDown())) {
			level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0f, 1.0f);
			return InteractionResult.SUCCESS;
		}
		if (basin.specialClick(player, i, hand)) {
			level.playSound(player, pos, SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1.0f, 1.0f);
			return InteractionResult.SUCCESS;
		}
		ItemStack basinStack = basin.getStack(i);
		if (hand == InteractionHand.MAIN_HAND && handStack.isEmpty() && !basinStack.isEmpty()) {
			level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0f, 1.0f);
			if (!level.isClientSide()) {
				ItemStack ret = basinStack.split(player.isShiftKeyDown() ? 1 : basinStack.getCount());
				if (player.getItemInHand(hand).isEmpty()) {
					player.setItemInHand(hand, ret);
				} else player.getInventory().placeItemBackInInventory(ret);
				basin.notifyTile();
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

}
