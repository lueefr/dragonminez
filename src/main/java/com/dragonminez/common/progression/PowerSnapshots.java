package com.dragonminez.common.progression;

import net.minecraft.nbt.*;
import java.util.*;

/** Persistent resolved form factors, captured by the server rather than by stat queries. */
public final class PowerSnapshots {
    public static final Set<String> STATS = Set.of("STR", "SKP", "STM", "DEF", "VIT", "PWR", "ENE", "SPEED");
    private final Map<String, Map<String, Double>> values = new LinkedHashMap<>();
    private Set<String> active = Set.of();
    public boolean has(String id) { return values.containsKey(id); }
    public boolean active(String id) { return active.contains(id); }
    public double value(String id, String stat) {
        var snapshot = values.get(id);
        if (snapshot == null) return 1;
        if (stat.equals("RES")) return snapshot.get("DEF") / 2 + snapshot.get("STM") / 2;
        return snapshot.getOrDefault(stat, 1d);
    }
    public void capture(String id, Map<String, Double> factors) {
        if (id == null || id.isBlank() || !factors.keySet().equals(STATS)) throw new IllegalArgumentException("Invalid power snapshot");
        factors.values().forEach(value -> { if (value == null || !Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Invalid captured factor"); });
        if (!values.containsKey(id) && values.size() >= 4096) throw new IllegalArgumentException("Power snapshot limit reached");
        values.put(id, Map.copyOf(factors));
    }
    public void refresh(Set<String> ids) { active = Set.copyOf(ids); }
    public void clear(String id) {
        values.remove(id); var next = new HashSet<>(active); next.remove(id); active = Set.copyOf(next);
    }
    public CompoundTag save() {
        var result = new CompoundTag();
        values.forEach((id, factors) -> {
            var entry = new CompoundTag(); factors.forEach(entry::putDouble); entry.putBoolean("Active", active.contains(id)); result.put(id, entry);
        });
        return result;
    }
    public void load(CompoundTag tag) {
        Map<String, Map<String, Double>> next = new LinkedHashMap<>(); Set<String> current = new HashSet<>();
        for (String id : tag.getAllKeys()) {
            if (next.size() >= 4096 || !tag.contains(id, Tag.TAG_COMPOUND)) continue;
            var entry = tag.getCompound(id); Map<String, Double> factors = new LinkedHashMap<>();
            for (String stat : STATS) if (entry.contains(stat, Tag.TAG_ANY_NUMERIC)) {
                double value = entry.getDouble(stat); if (Double.isFinite(value) && value >= 0) factors.put(stat, value);
            }
            if (!factors.keySet().equals(STATS)) continue;
            next.put(id, Map.copyOf(factors)); if (entry.getBoolean("Active")) current.add(id);
        }
        values.clear(); values.putAll(next); active = Set.copyOf(current);
    }
}
