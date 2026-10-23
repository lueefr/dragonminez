package com.dragonminez.platform.event.server;
import net.minecraft.server.MinecraftServer;
public final class ServerStartingEvent extends ServerEvent {
    public ServerStartingEvent(MinecraftServer server) { super(server); }
}
