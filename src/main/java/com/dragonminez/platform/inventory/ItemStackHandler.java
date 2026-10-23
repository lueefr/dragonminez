package com.dragonminez.platform.inventory;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.*;
import java.util.Objects;

/** Native container with non-mutating insertion/extraction simulation and legacy inventory keys. */
public class ItemStackHandler implements IItemHandler {
    protected NonNullList<ItemStack> stacks;
    public ItemStackHandler(int slots) { setSize(slots); }
    public void setSize(int slots) {
        if (slots < 0) throw new IllegalArgumentException("Negative inventory size");
        stacks = NonNullList.withSize(slots, ItemStack.EMPTY);
    }
    protected void validateSlotIndex(int slot) {
        if (slot < 0 || slot >= stacks.size()) throw new IndexOutOfBoundsException("Slot " + slot + " outside inventory of " + stacks.size());
    }
    @Override public int getSlots() { return stacks.size(); }
    @Override public ItemStack getStackInSlot(int slot) { validateSlotIndex(slot); return stacks.get(slot); }
    @Override public void setStackInSlot(int slot, ItemStack stack) {
        validateSlotIndex(slot); stacks.set(slot, Objects.requireNonNull(stack)); onContentsChanged(slot);
    }
    @Override public int getSlotLimit(int slot) { validateSlotIndex(slot); return 64; }
    protected int getStackLimit(int slot, ItemStack stack) { return Math.min(getSlotLimit(slot), stack.getMaxStackSize()); }
    @Override public boolean isItemValid(int slot, ItemStack stack) { validateSlotIndex(slot); return true; }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        validateSlotIndex(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!isItemValid(slot, stack)) return stack;
        ItemStack existing = stacks.get(slot);
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) return stack;
        int inserted = Math.min(stack.getCount(), getStackLimit(slot, stack) - existing.getCount());
        if (inserted <= 0) return stack;
        if (!simulate) {
            if (existing.isEmpty()) stacks.set(slot, stack.copyWithCount(inserted));
            else existing.grow(inserted);
            onContentsChanged(slot);
        }
        return inserted == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
    }
    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        validateSlotIndex(slot);
        ItemStack existing = stacks.get(slot);
        if (amount <= 0 || existing.isEmpty()) return ItemStack.EMPTY;
        int extracted = Math.min(amount, existing.getCount());
        ItemStack result = existing.copyWithCount(extracted);
        if (!simulate) {
            stacks.set(slot, extracted == existing.getCount() ? ItemStack.EMPTY : existing.copyWithCount(existing.getCount() - extracted));
            onContentsChanged(slot);
        }
        return result;
    }
    @Override public void setChanged() { onContentsChanged(-1); }
    protected void onContentsChanged(int slot) {}
    protected void onLoad() {}
    public void writeValue(ValueOutput output) {
        output.putInt("Size", stacks.size());
        var items = output.childrenList("Items");
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            if (stack.isEmpty()) continue;
            var entry = items.addChild(); entry.putInt("Slot", slot); entry.store(ItemStack.MAP_CODEC, stack);
        }
    }
    public void readValue(ValueInput input) {
        setSize(input.getIntOr("Size", stacks.size()));
        for (var entry : input.childrenListOrEmpty("Items")) {
            int slot = entry.getIntOr("Slot", -1);
            if (slot >= 0 && slot < stacks.size()) stacks.set(slot, entry.read(ItemStack.MAP_CODEC).orElse(ItemStack.EMPTY));
        }
        onLoad();
    }
    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        writeValue(output); return output.buildResult();
    }
    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        readValue(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
    }
}
