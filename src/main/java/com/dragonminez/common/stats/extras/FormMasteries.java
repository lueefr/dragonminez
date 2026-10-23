package com.dragonminez.common.stats.extras;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Map;

public class FormMasteries {
    private long progressionRevision;
    public long getProgressionRevision() { return progressionRevision; }
    private final Map<String, Double> masteries = new HashMap<>();

    public FormMasteries() {}

    public double getMastery(String formGroup, String formName) {
        String key = getKey(formGroup, formName);
        return masteries.getOrDefault(key, 0.0);
    }

    public void setMastery(String formGroup, String formName, double mastery, double maxMastery) {
        progressionRevision++;
        String key = getKey(formGroup, formName);
        masteries.put(key, Math.max(0.0, Math.min(maxMastery, mastery)));
    }

    public void addMastery(String formGroup, String formName, double amount, double maxMastery) {
        progressionRevision++;
        String key = getKey(formGroup, formName);
        double current = masteries.getOrDefault(key, 0.0);
        double newMastery = Math.min(maxMastery, current + amount);
        masteries.put(key, newMastery);
    }

    public boolean hasMaxMastery(String formGroup, String formName, double maxMastery) {
        return getMastery(formGroup, formName) >= maxMastery;
    }

    private String getKey(String formGroup, String formName) {
        return formGroup.toLowerCase() + ":" + formName.toLowerCase();
    }

    public Map<String, Double> entries() {
        return new HashMap<>(masteries);
    }

    public double getRaw(String key) {
        return masteries.getOrDefault(key, 0.0);
    }

    public void putMax(String key, double value) {
        progressionRevision++;
        masteries.merge(key, value, Math::max);
    }

    public void addRaw(String key, double amount) {
        progressionRevision++;
        masteries.merge(key, amount, Double::sum);
    }

    public void clear() {
        progressionRevision++;
        masteries.clear();
    }

    public CompoundTag save() {
        CompoundTag nbt = new CompoundTag();
        for (Map.Entry<String, Double> entry : masteries.entrySet()) {
            nbt.putDouble(entry.getKey(), entry.getValue());
        }
        return nbt;
    }

    public void load(CompoundTag nbt) {
        progressionRevision++;
        masteries.clear();
        for (String key : nbt.keySet()) {
            masteries.put(key, nbt.getDoubleOr(key, 0));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(masteries.size());
        for (Map.Entry<String, Double> entry : masteries.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeDouble(entry.getValue());
        }
    }

    public void fromBytes(FriendlyByteBuf buf) {
        progressionRevision++;
        masteries.clear();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();
            double value = buf.readDouble();
            masteries.put(key, value);
        }
    }

    public void copyFrom(FormMasteries other) {
        progressionRevision++;
        this.masteries.clear();
        this.masteries.putAll(other.masteries);
    }
}

