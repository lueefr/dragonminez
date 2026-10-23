package com.dragonminez.platform.resource;

import com.dragonminez.platform.event.Event;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.RepositorySource;
import java.util.function.Consumer;

/** Dispatched when the native built-in source enumerates packs for this repository. */
public final class AddPackFindersEvent extends Event {
    private final PackType type;
    private final Consumer<Pack> consumer;
    public AddPackFindersEvent(PackType type, Consumer<Pack> consumer) { this.type = type; this.consumer = consumer; }
    public PackType getPackType() { return type; }
    public void addRepositorySource(RepositorySource source) { source.loadPacks(consumer); }
}
