package com.dragonminez.platform.event.tick;

import com.dragonminez.platform.event.GameEvents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

@Environment(EnvType.CLIENT)
public final class ClientTickBridge {
    private static boolean installed;
    private ClientTickBridge() {}
    public static synchronized void register() {
        if (installed) return;
        ClientTickEvents.START_CLIENT_TICK.register(client -> GameEvents.BUS.post(new ClientTickEvent.Pre()));
        ClientTickEvents.END_CLIENT_TICK.register(client -> GameEvents.BUS.post(new ClientTickEvent.Post()));
        ClientTickEvents.START_LEVEL_TICK.register(level -> GameEvents.BUS.post(new LevelTickEvent.Pre(level)));
        ClientTickEvents.END_LEVEL_TICK.register(level -> GameEvents.BUS.post(new LevelTickEvent.Post(level)));
        installed = true;
    }
}
