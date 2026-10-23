package com.dragonminez.platform.nbt;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.*;
import java.util.UUID;

/** Keeps existing on-disk data semantics while using the 26.1 codec-based NBT API. */
public final class NbtCompat {
    private NbtCompat() {}
    public static boolean contains(CompoundTag tag, String key, int type) {
        Tag value = tag.get(key);
        return value != null && (type == 99 ? value.asNumber().isPresent() : value.getId() == type);
    }
    public static ListTag getList(CompoundTag tag, String key, int elementType) {
        ListTag list = tag.getListOrEmpty(key);
        for (Tag element : list) if (element.getId() != elementType) return new ListTag();
        return list;
    }
    public static boolean hasUUID(CompoundTag tag, String key) { return tag.read(key, UUIDUtil.CODEC).isPresent(); }
    public static UUID getUUID(CompoundTag tag, String key) {
        return tag.read(key, UUIDUtil.CODEC).orElseThrow(() -> new IllegalArgumentException("Missing or invalid UUID: " + key));
    }
    public static void putUUID(CompoundTag tag, String key, UUID uuid) { tag.store(key, UUIDUtil.CODEC, uuid); }
}
