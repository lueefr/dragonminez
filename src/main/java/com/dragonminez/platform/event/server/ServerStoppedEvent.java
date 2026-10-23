package com.dragonminez.platform.event.server;
import net.minecraft.server.MinecraftServer;
public final class ServerStoppedEvent extends ServerEvent {
    public ServerStoppedEvent(MinecraftServer server) { super(server); }
}
