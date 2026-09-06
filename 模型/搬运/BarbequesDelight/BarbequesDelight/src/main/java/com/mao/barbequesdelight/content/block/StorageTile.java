package com.mao.barbequesdelight.content.block;

import dev.xkmc.l2library.base.tile.BaseContainerListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface StorageTile extends BlockSlot, BaseContainerListener {

	ItemStack getStack(int i);

	void setStack(int i, ItemStack split);

	boolean specialClick(Player player, int i, InteractionHand hand);

	default boolean insert(Level level, int i, ItemStack handStack, boolean all) {
		if (handStack.isEmpty()) return false;
		ItemStack basinStack = getStack(i);
		if (!basinStack.isEmpty() && !ItemStack.isSameItemSameTags(basinStack, handStack)) return false;
		int old = basinStack.getCount();
		int max = handStack.getMaxStackSize();
		int split = all ? handStack.getCount() : 1;
		split = Math.min(max - old, split);
		if (split <= 0) return false;
		if (!level.isClientSide()) {
			ItemStack copy = handStack.split(split);
			copy.grow(old);
			setStack(i, copy);
		}
		return true;
	}

}
