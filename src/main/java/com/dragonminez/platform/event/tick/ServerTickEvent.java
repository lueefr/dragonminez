package com.dragonminez.platform.event.tick;

import com.dragonminez.platform.event.Event;

/** Dispatched by the native Fabric lifecycle or entity tick bridge. */
public abstract class ServerTickEvent extends Event {
    private final net.minecraft.server.MinecraftServer server;
    protected ServerTickEvent(net.minecraft.server.MinecraftServer server) { this.server = java.util.Objects.requireNonNull(server); }
    public net.minecraft.server.MinecraftServer getServer() { return server; }
    public static final class Pre extends ServerTickEvent { public Pre(net.minecraft.server.MinecraftServer server) { super(server); } }
    public static final class Post extends ServerTickEvent { public Post(net.minecraft.server.MinecraftServer server) { super(server); } }
}
