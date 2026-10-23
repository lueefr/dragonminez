package com.dragonminez.platform.inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SlotItemHandler extends Slot {
    private final IItemHandler handler;
    private final int slot;
    public SlotItemHandler(IItemHandler handler, int slot, int x, int y) {
        super(handler, slot, x, y); this.handler = handler; this.slot = slot;
    }
    @Override public boolean mayPlace(ItemStack stack) { return handler.isItemValid(slot, stack); }
    @Override public int getMaxStackSize() { return handler.getSlotLimit(slot); }
    @Override public int getMaxStackSize(ItemStack stack) { return Math.min(getMaxStackSize(), stack.getMaxStackSize()); }
    @Override public boolean mayPickup(Player player) { return !handler.extractItem(slot, 1, true).isEmpty(); }
    @Override public ItemStack remove(int amount) { return handler.extractItem(slot, amount, false); }
}
