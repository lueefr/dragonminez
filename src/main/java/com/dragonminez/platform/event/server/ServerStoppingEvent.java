package com.dragonminez.platform.event.server;
import net.minecraft.server.MinecraftServer;
public final class ServerStoppingEvent extends ServerEvent {
    public ServerStoppingEvent(MinecraftServer server) { super(server); }
}
