package com.dragonminez.platform.event.tick;

import com.dragonminez.platform.event.Event;

/** Dispatched by the native Fabric lifecycle or entity tick bridge. */
public abstract class EntityTickEvent extends Event {
    private final net.minecraft.world.entity.Entity entity;
    protected EntityTickEvent(net.minecraft.world.entity.Entity entity) { this.entity = java.util.Objects.requireNonNull(entity); }
    public net.minecraft.world.entity.Entity getEntity() { return entity; }
    public static final class Post extends EntityTickEvent { public Post(net.minecraft.world.entity.Entity entity) { super(entity); } }
}
