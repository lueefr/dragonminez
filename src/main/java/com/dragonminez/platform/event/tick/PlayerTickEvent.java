package com.dragonminez.platform.event.tick;

import com.dragonminez.platform.event.Event;

/** Dispatched by the native Fabric lifecycle or entity tick bridge. */
public abstract class PlayerTickEvent extends Event {
    private final net.minecraft.world.entity.player.Player player;
    protected PlayerTickEvent(net.minecraft.world.entity.player.Player player) { this.player = java.util.Objects.requireNonNull(player); }
    public net.minecraft.world.entity.player.Player getEntity() { return player; }
    public static final class Post extends PlayerTickEvent { public Post(net.minecraft.world.entity.player.Player player) { super(player); } }
}
