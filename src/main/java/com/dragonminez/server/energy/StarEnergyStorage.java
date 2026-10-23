package com.dragonminez.server.energy;

import net.minecraft.nbt.CompoundTag;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/** Transactional Fabric energy with the machine-facing integer operations retained. */
public class StarEnergyStorage extends SimpleEnergyStorage {
    public StarEnergyStorage(int capacity, int maxTransfer) { super(capacity, maxTransfer, maxTransfer); }
    public int extractEnergy(int requested, boolean simulate) {
        if (requested <= 0) return 0;
        try (var transaction = Transaction.openOuter()) {
            int extracted = (int) extract(requested, transaction);
            if (!simulate) transaction.commit();
            return extracted;
        }
    }
    public int receiveEnergy(int requested, boolean simulate) {
        if (requested <= 0) return 0;
        try (var transaction = Transaction.openOuter()) {
            int received = (int) insert(requested, transaction);
            if (!simulate) transaction.commit();
            return received;
        }
    }
    public int getEnergyStored() { return (int) amount; }
    public int getMaxEnergyStored() { return (int) capacity; }
    public boolean canReceive() { return supportsInsertion(); }
    public boolean canExtract() { return supportsExtraction(); }
    public void setEnergy(int energy) {
        long clamped = Math.clamp((long) energy, 0, capacity);
        if (amount != clamped) { amount = clamped; onEnergyChanged(); }
    }
    @Override protected void onFinalCommit() { onEnergyChanged(); }
    public void onEnergyChanged() {}
    public void saveNBT(CompoundTag tag) { tag.putInt("energy", (int) amount); }
    public void loadNBT(CompoundTag tag) { amount = Math.clamp((long) tag.getIntOr("energy", 0), 0, capacity); }
}
