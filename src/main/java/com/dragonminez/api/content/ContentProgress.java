package com.dragonminez.api.content;

import net.minecraft.nbt.CompoundTag;
import java.util.*;

/** Lifetime completion flags; independent of the repeatable quest's current status. */
public final class ContentProgress {
    private final Map<String, Integer> completions = new TreeMap<>();
    private long revision;

    public static String requireId(String id) {
        if (id == null || id.length() > 256 || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Expected namespaced content id, got: " + id);
        }
        return id;
    }
    public boolean hasCompleted(String id) { return count(id) > 0; }
    public int count(String id) { return completions.getOrDefault(requireId(id), 0); }
    public long revision() { return revision; }
    public Map<String, Integer> snapshot() { return Map.copyOf(completions); }
    int complete(String id) {
        int count = (int) Math.min(Integer.MAX_VALUE, (long) count(id) + 1);
        completions.put(id, count);
        revision++;
        return count;
    }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        completions.forEach(tag::putInt);
        return tag;
    }
    public void load(CompoundTag tag) {
        Map<String, Integer> next = new TreeMap<>();
        for (String id : tag.getAllKeys()) {
            requireId(id);
            int count = tag.getInt(id);
            if (count > 0) next.put(id, count);
        }
        completions.clear();
        completions.putAll(next);
        revision++;
    }
    public void copyFrom(ContentProgress other) {
        completions.clear();
        completions.putAll(other.completions);
        revision++;
    }
}
