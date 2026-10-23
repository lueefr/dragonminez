package com.dragonminez.platform.registry;

import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.*;
import java.util.*;
import java.util.function.*;

/** Explicit registration phase; independent of a loader event bus. */
public final class DeferredRegister<R> {
    private final Registry<R> registry;
    private final String namespace;
    private final Map<String, Runnable> pending = new LinkedHashMap<>();
    private final List<DeferredHolder<R, ? extends R>> entries = new ArrayList<>();
    private boolean registered;
    private DeferredRegister(Registry<R> registry, String namespace) { this.registry=registry; this.namespace=namespace; }
    public static <R> DeferredRegister<R> create(Registry<R> registry, String namespace) { return new DeferredRegister<>(registry, namespace); }
    @SuppressWarnings("unchecked")
    public static <R> DeferredRegister<R> create(ResourceKey<? extends Registry<R>> key, String namespace) {
        Registry<R> registry = (Registry<R>) BuiltInRegistries.REGISTRY.getValue(key.identifier());
        if (registry == null) throw new IllegalArgumentException("Not a built-in registry: " + key);
        return create(registry, namespace);
    }
    public <T extends R> DeferredHolder<R,T> register(String name, Supplier<? extends T> factory) {
        return register(name, id -> factory.get());
    }
    public synchronized <T extends R> DeferredHolder<R,T> register(String name, Function<Identifier, ? extends T> factory) {
        if (registered) throw new IllegalStateException("Registration phase already completed for " + registry.key());
        if (pending.containsKey(name)) throw new IllegalArgumentException("Duplicate registry entry " + namespace + ":" + name);
        Identifier id = Identifier.fromNamespaceAndPath(namespace, name);
        DeferredHolder<R,T> reference = new DeferredHolder<>(ResourceKey.create(registry.key(), id));
        pending.put(name, () -> RegistrationScope.withId(id, () -> {
            T value = Objects.requireNonNull(factory.apply(id), "Registry factory returned null: " + id);
            @SuppressWarnings("unchecked") Holder<R> holder = (Holder<R>) (Holder<?>) Registry.registerForHolder(registry, id, value);
            reference.bind(holder, value);
            return value;
        }));
        entries.add(reference);
        return reference;
    }
    public synchronized void register() {
        if (registered) return;
        for (Runnable registration : pending.values()) registration.run();
        registered = true;
        pending.clear();
    }
    public Collection<DeferredHolder<R, ? extends R>> getEntries() { return List.copyOf(entries); }
    public ResourceKey<? extends Registry<R>> getRegistryKey() { return registry.key(); }
}
