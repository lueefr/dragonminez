package com.dragonminez.platform.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Slot operations shared by menus and automation, backed by a native Container. */
public interface IItemHandler extends Container {
    int getSlots();
    ItemStack getStackInSlot(int slot);
    void setStackInSlot(int slot, ItemStack stack);
    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);
    ItemStack extractItem(int slot, int amount, boolean simulate);
    int getSlotLimit(int slot);
    boolean isItemValid(int slot, ItemStack stack);
    @Override default int getContainerSize() { return getSlots(); }
    @Override default ItemStack getItem(int slot) { return getStackInSlot(slot); }
    @Override default void setItem(int slot, ItemStack stack) { setStackInSlot(slot, stack); }
    @Override default ItemStack removeItem(int slot, int amount) { return extractItem(slot, amount, false); }
    @Override default ItemStack removeItemNoUpdate(int slot) { return extractItem(slot, Integer.MAX_VALUE, false); }
    @Override default boolean canPlaceItem(int slot, ItemStack stack) { return isItemValid(slot, stack); }
    @Override default boolean stillValid(Player player) { return true; }
    @Override default boolean isEmpty() {
        for (int slot = 0; slot < getSlots(); slot++) if (!getStackInSlot(slot).isEmpty()) return false;
        return true;
    }
    @Override default void clearContent() {
        for (int slot = 0; slot < getSlots(); slot++) setStackInSlot(slot, ItemStack.EMPTY);
    }
}
