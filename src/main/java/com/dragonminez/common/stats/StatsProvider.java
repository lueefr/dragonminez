package com.dragonminez.common.stats;

import com.dragonminez.Reference;
import com.dragonminez.compat.capabilities.*;
import com.dragonminez.compat.util.LazyOptional;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Fabric-persisted stats whose player owner is bound after attachment decoding. */
public final class StatsProvider implements ICapabilityProvider {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "player_stats");
    public static final Codec<StatsProvider> CODEC = CompoundTag.CODEC.xmap(StatsProvider::new, StatsProvider::snapshot);
    private StatsData data;
    private CompoundTag pending;
    private LazyOptional<StatsData> optional;

    public StatsProvider() { pending = new CompoundTag(); }
    public StatsProvider(Player player) { this(); bindPlayer(player); }
    private StatsProvider(CompoundTag encoded) { pending = encoded.copy(); }

    private void bindPlayer(Player player) {
        if (data != null) {
            if (data.getPlayer() != player) throw new IllegalStateException("Stats attachment is already bound to another player entity");
            return;
        }
        StatsData decoded = new StatsData(player);
        if (pending != null && !pending.isEmpty()) {
            try { decoded.load(pending); }
            catch (ClassNotFoundException exception) { throw new IllegalArgumentException("Invalid persisted player stats", exception); }
        }
        data = decoded;
        pending = null;
        optional = LazyOptional.of(() -> data);
    }

    public static StatsProvider getOrCreate(Player player) {
        AttachmentTarget target = (AttachmentTarget) player;
        StatsProvider provider = target.getAttachedOrCreate(StatsCapability.PLAYER_STATS);
        // Fabric may copy an attachment value by reference on respawn. Do not rebind the old
        // player's live stats object: retain its snapshot and give the new entity a new owner.
        if (provider.data != null && provider.data.getPlayer() != player) {
            provider = new StatsProvider(provider.snapshot());
            target.setAttached(StatsCapability.PLAYER_STATS, provider);
        }
        provider.bindPlayer(player);
        return provider;
    }

    public static void remove(Player player) {
        // The entity owns its Fabric attachment. It must remain available during clone callbacks.
    }

    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        return capability == StatsCapability.INSTANCE && optional != null ? optional.cast() : LazyOptional.empty();
    }
    public static <T> @NotNull LazyOptional<T> get(Capability<T> capability, Entity entity) {
        if (!(entity instanceof Player player) || capability != StatsCapability.INSTANCE) return LazyOptional.empty();
        return getOrCreate(player).optional.cast();
    }
    private CompoundTag snapshot() { return data != null ? data.save() : pending.copy(); }
    public CompoundTag serializeNBT(HolderLookup.Provider registries) { return snapshot(); }
    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        if (data == null) pending = tag.copy();
        else {
            try { data.load(tag); }
            catch (ClassNotFoundException exception) { throw new IllegalArgumentException("Invalid persisted player stats", exception); }
        }
    }
    void invalidate() { if (optional != null) optional.invalidate(); }
}
