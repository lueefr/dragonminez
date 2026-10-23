package com.dragonminez.platform.event.tick;

import com.dragonminez.platform.event.Event;

/** Dispatched by the native Fabric lifecycle or entity tick bridge. */
public abstract class LevelTickEvent extends Event {
    private final net.minecraft.world.level.Level level;
    protected LevelTickEvent(net.minecraft.world.level.Level level) { this.level = java.util.Objects.requireNonNull(level); }
    public net.minecraft.world.level.Level getLevel() { return level; }
    public static final class Pre extends LevelTickEvent { public Pre(net.minecraft.world.level.Level level) { super(level); } }
    public static final class Post extends LevelTickEvent { public Post(net.minecraft.world.level.Level level) { super(level); } }
}
