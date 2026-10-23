package com.dragonminez.platform.event.server;
import com.dragonminez.platform.event.Event;
import net.minecraft.server.MinecraftServer;
public abstract class ServerEvent extends Event {
    private final MinecraftServer server;
    protected ServerEvent(MinecraftServer server) { this.server = java.util.Objects.requireNonNull(server); }
    public MinecraftServer getServer() { return server; }
}
