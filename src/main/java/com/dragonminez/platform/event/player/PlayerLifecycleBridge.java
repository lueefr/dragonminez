package com.dragonminez.platform.event.player;

import com.dragonminez.platform.event.GameEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class PlayerLifecycleBridge {
    private static boolean installed;
    private PlayerLifecycleBridge() {}
    public static synchronized void register() {
        if (installed) return;
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                GameEvents.BUS.post(new PlayerEvent.PlayerLoggedInEvent(handler.player)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                GameEvents.BUS.post(new PlayerEvent.PlayerLoggedOutEvent(handler.player)));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                GameEvents.BUS.post(new PlayerEvent.PlayerRespawnEvent(newPlayer, alive)));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, from, to) ->
                GameEvents.BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(player, from.dimension(), to.dimension())));
        installed = true;
    }
}
