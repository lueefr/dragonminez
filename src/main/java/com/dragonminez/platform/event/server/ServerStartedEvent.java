package com.dragonminez.platform.event.server;
import net.minecraft.server.MinecraftServer;
public final class ServerStartedEvent extends ServerEvent {
    public ServerStartedEvent(MinecraftServer server) { super(server); }
}
