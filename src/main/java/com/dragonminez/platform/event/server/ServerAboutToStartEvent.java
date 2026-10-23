package com.dragonminez.platform.event.server;
import net.minecraft.server.MinecraftServer;
public final class ServerAboutToStartEvent extends ServerEvent {
    public ServerAboutToStartEvent(MinecraftServer server) { super(server); }
}
