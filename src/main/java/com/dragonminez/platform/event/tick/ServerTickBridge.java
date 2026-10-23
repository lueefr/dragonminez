package com.dragonminez.platform.event.tick;

import com.dragonminez.platform.event.GameEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class ServerTickBridge {
    private static boolean installed;
    private ServerTickBridge() {}
    public static synchronized void register() {
        if (installed) return;
        ServerTickEvents.START_SERVER_TICK.register(server -> GameEvents.BUS.post(new ServerTickEvent.Pre(server)));
        ServerTickEvents.END_SERVER_TICK.register(server -> GameEvents.BUS.post(new ServerTickEvent.Post(server)));
        ServerTickEvents.START_LEVEL_TICK.register(level -> GameEvents.BUS.post(new LevelTickEvent.Pre(level)));
        ServerTickEvents.END_LEVEL_TICK.register(level -> GameEvents.BUS.post(new LevelTickEvent.Post(level)));
        installed = true;
    }
}
