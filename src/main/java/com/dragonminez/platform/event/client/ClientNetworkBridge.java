package com.dragonminez.platform.event.client;
import com.dragonminez.platform.event.GameEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
public final class ClientNetworkBridge {
    private static boolean installed;
    private ClientNetworkBridge() {}
    public static synchronized void register() {
        if (installed) return;
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                GameEvents.BUS.post(new ClientPlayerNetworkEvent.LoggingIn(client.player)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                GameEvents.BUS.post(new ClientPlayerNetworkEvent.LoggingOut(client.player)));
        installed = true;
    }
}
