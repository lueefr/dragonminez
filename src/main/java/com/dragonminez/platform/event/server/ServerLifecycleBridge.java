package com.dragonminez.platform.event.server;

import com.dragonminez.platform.event.GameEvents;
import com.dragonminez.platform.event.level.LevelEvent;
import com.dragonminez.platform.event.level.ChunkEvent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;

public final class ServerLifecycleBridge {
    private static boolean installed;
    private ServerLifecycleBridge() {}
    public static synchronized void register() {
        if (installed) return;
        ServerLifecycleEvents.SERVER_STARTING.register(server -> GameEvents.BUS.post(new ServerAboutToStartEvent(server)));
        // The original starting handlers require live dimensions and SavedData.
        // Fabric's SERVER_STARTED runs after levels load and before players can join.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            GameEvents.BUS.post(new ServerStartingEvent(server));
            GameEvents.BUS.post(new ServerStartedEvent(server));
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> GameEvents.BUS.post(new ServerStoppingEvent(server)));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> GameEvents.BUS.post(new ServerStoppedEvent(server)));
        ServerLevelEvents.LOAD.register((server, level) -> GameEvents.BUS.post(new LevelEvent.Load(level)));
        ServerLevelEvents.UNLOAD.register((server, level) -> GameEvents.BUS.post(new LevelEvent.Unload(level)));
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> GameEvents.BUS.post(new ChunkEvent.Load(level, chunk)));
        installed = true;
    }
}
