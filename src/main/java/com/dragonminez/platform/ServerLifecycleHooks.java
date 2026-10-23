package com.dragonminez.platform;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

/** Server ownership is installed before world loading, including integrated-server sessions. */
public final class ServerLifecycleHooks {
    private static volatile MinecraftServer current;
    private static boolean installed;
    private ServerLifecycleHooks() {}
    public static synchronized void initialize() {
        if (installed) return;
        ServerLifecycleEvents.SERVER_STARTING.register(server -> current=server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { if (current==server) current=null; });
        installed=true;
    }
    public static MinecraftServer getCurrentServer() { return current; }
}
