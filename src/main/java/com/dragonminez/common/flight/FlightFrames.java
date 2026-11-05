package com.dragonminez.common.flight;

import com.dragonminez.api.flight.FlightMotion.State;
import net.minecraft.world.entity.player.Player;

/** Transient physical poses shared by movement, targeting and rendering adapters. */
public final class FlightFrames {
    private static final IdentityStore<Player, State> STATES = new IdentityStore<>();
    private FlightFrames() {}
    public static State get(Player player) { return STATES.get(player); }
    public static boolean active(Player player) { return STATES.get(player) != null; }
    public static boolean any(net.minecraft.world.level.Level level) {
        return STATES.any(player -> player.level() == level);
    }
    public static void put(Player player, State state) { STATES.put(player, state); }
    public static void remove(Player player) {
        if (STATES.remove(player) == null) return;
        player.refreshDimensions();
        var bounds = player.getBoundingBox();
        if (player.isAlive() && player.getPose() == net.minecraft.world.entity.Pose.STANDING && !player.level().noCollision(player, bounds)) {
            var crouch = net.minecraft.world.entity.Pose.CROUCHING;
            var size = net.neoforged.neoforge.event.EventHooks.getEntitySizeForge(player, crouch, player.getDimensions(crouch)).getNewSize();
            player.setPose(player.level().noCollision(player, size.makeBoundingBox(player.position()))
                    ? crouch : net.minecraft.world.entity.Pose.SWIMMING);
        }
        player.refreshDimensions();
    }
}
