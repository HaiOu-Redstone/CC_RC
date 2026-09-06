package com.cc_rc.block.fridge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 冰箱方块实体（搬运自 Cooking for Blockheads）
 *
 * 27 格容器，基于原版 RandomizableContainerBlockEntity（支持箱子式掉落物表与命名），
 * GUI 直接复用原版箱子菜单（ChestMenu.threeRows），无需自建菜单/界面。
 * 需实现的抽象方法：getItems / setItems（物品存取）、getContainerSize（格数）、
 * getDefaultName（窗口标题）、createMenu(int, Inventory)（菜单构造）。
 */
public class FridgeBlockEntity extends RandomizableContainerBlockEntity {
    public static final int CONTAINER_SIZE = 27;

    // 容器内物品列表（27 格，全部初始为空）
    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

    public FridgeBlockEntity(BlockPos pos, BlockState state) {
        super(com.cc_rc.ModBlockEntities.FRIDGE_BE.get(), pos, state);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.cc_rc.fridge");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory playerInventory) {
        // 复用原版箱子 3 行 GUI（27 格容器对应箱子大小）
        return ChestMenu.threeRows(id, playerInventory, this);
    }
}
