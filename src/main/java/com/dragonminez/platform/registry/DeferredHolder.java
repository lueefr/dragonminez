package com.dragonminez.platform.registry;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.*;
import net.minecraft.tags.TagKey;
import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;

/** A stable reference to an entry registered in a real vanilla/Fabric registry. */
public final class DeferredHolder<R, T extends R> implements Holder<R>, Supplier<T> {
    private final ResourceKey<R> key;
    private Holder<R> bound;
    private T registeredValue;
    DeferredHolder(ResourceKey<R> key) { this.key = key; }
    void bind(Holder<R> holder, T value) {
        if (bound != null) throw new IllegalStateException("Registry entry already bound: " + key);
        bound = holder;
        registeredValue = Objects.requireNonNull(value);
    }
    private Holder<R> bound() {
        if (bound == null) throw new IllegalStateException("Registry entry used before registration: " + key);
        return bound;
    }
    public Identifier getId() { return key.identifier(); }
    public ResourceKey<R> getKey() { return key; }
    public T get() { bound(); return registeredValue; }
    @Override public R value() { return get(); }
    @Override public boolean isBound() { return registeredValue != null; }
    @Override public boolean areComponentsBound() { return bound != null && bound.areComponentsBound(); }
    @Override public boolean is(Identifier id) { return key.identifier().equals(id); }
    @Override public boolean is(ResourceKey<R> key) { return this.key.equals(key); }
    @Override public boolean is(Predicate<ResourceKey<R>> predicate) { return predicate.test(key); }
    @Override public boolean is(TagKey<R> tag) { return bound().is(tag); }
    @Override public boolean is(Holder<R> holder) { return holder.unwrapKey().map(this::is).orElse(false); }
    @Override public Stream<TagKey<R>> tags() { return bound().tags(); }
    @Override public DataComponentMap components() { return bound().components(); }
    @Override public Either<ResourceKey<R>, R> unwrap() { return Either.left(key); }
    @Override public Optional<ResourceKey<R>> unwrapKey() { return Optional.of(key); }
    @Override public Kind kind() { return Kind.REFERENCE; }
    @Override public boolean canSerializeIn(HolderOwner<R> owner) { return bound().canSerializeIn(owner); }
}
