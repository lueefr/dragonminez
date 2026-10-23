package com.dragonminez.platform.entity;

import com.dragonminez.mixin.common.SpawnPlacementsInvoker;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Resolves specific overrides before registering each mod entity's native spawn predicate once. */
public final class SpawnPlacementRegistry {
    private final Map<EntityType<?>, Runnable> entries = new LinkedHashMap<>();
    private boolean committed;
    public <T extends Mob> void register(EntityType<T> type, SpawnPlacementType placement,
            Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate) {
        if (committed) throw new IllegalStateException("Spawn placements already committed");
        entries.put(type, () -> SpawnPlacementsInvoker.dmz$register(type, placement, heightmap, predicate));
    }
    public void commit() {
        if (committed) return;
        entries.values().forEach(Runnable::run);
        committed = true;
    }
}
