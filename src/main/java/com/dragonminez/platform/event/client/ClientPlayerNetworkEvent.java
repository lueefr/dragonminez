package com.dragonminez.platform.event.client;
import com.dragonminez.platform.event.Event;
import net.minecraft.client.player.LocalPlayer;
public abstract class ClientPlayerNetworkEvent extends Event {
    private final LocalPlayer player;
    private ClientPlayerNetworkEvent(LocalPlayer player) { this.player = player; }
    public LocalPlayer getPlayer() { return player; }
    public static final class LoggingIn extends ClientPlayerNetworkEvent {
        public LoggingIn(LocalPlayer player) { super(player); }
    }
    public static final class LoggingOut extends ClientPlayerNetworkEvent {
        public LoggingOut(LocalPlayer player) { super(player); }
    }
    public static final class Clone extends ClientPlayerNetworkEvent {
        private final LocalPlayer oldPlayer;
        public Clone(LocalPlayer oldPlayer, LocalPlayer newPlayer) { super(newPlayer); this.oldPlayer = oldPlayer; }
        public LocalPlayer getOldPlayer() { return oldPlayer; }
    }
}
