package com.dragonminez.client.flight;

import com.dragonminez.api.flight.FlightMotion;
import com.dragonminez.api.flight.FlightMotion.*;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.flight.*;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.C2S.FlightInputC2S;
import com.dragonminez.common.network.S2C.FlightStateS2C;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayDeque;

public final class FlightClient {
    private record Pending(long sequence, Controls controls, boolean restricted) {}
    private static final ArrayDeque<Pending> PENDING = new ArrayDeque<>();
    private static Player owner;
    private static long session;
    private static long sequence;
    private static long lastAck;
    private static boolean ended;
    private static double pitch;
    private static double roll;
    private FlightClient() {}

    public static void clean(Player player) {
        if (owner != null && owner != player) {
            FlightFrames.remove(owner); owner = null; session = sequence = 0;
            PENDING.clear(); pitch = roll = 0;
        }
    }

    public static void mouse(double x, double y) {
        double scale = .015 * ConfigManager.getUserConfig().getFlightTurnSensitivity();
        roll = Math.clamp(roll + x * scale, -1, 1);
        pitch = Math.clamp(pitch + y * scale, -1, 1);
    }

    public static void receive(FlightStateS2C message) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().location().equals(message.dimension())
                || !(mc.level.getEntity(message.entity()) instanceof Player player)) return;
        if (player != mc.player) {
            if(!FlightVisuals.receive(player,message))return;
            if (message.active()) { FlightFrames.put(player, message.state()); FlightBody.apply(player, message.state()); }
            else FlightFrames.remove(player);
            return;
        }
        if (owner != player || session != message.session()) {
            if (owner == player && message.session() < session) return;
            owner = player; session = message.session(); sequence = message.sequence(); lastAck=message.sequence(); ended=false;
            PENDING.clear(); pitch = roll = 0;
        }
        if(message.sequence()<lastAck || ended&&message.active())return;
        lastAck=message.sequence();
        if (!message.active()) { ended=true; FlightFrames.remove(player); PENDING.clear(); return; }
        while (!PENDING.isEmpty() && PENDING.peekFirst().sequence() <= message.sequence()) PENDING.removeFirst();
        var state = message.state();
        for (var pending : PENDING) {
            var next = pending.restricted() ? new State(state.position(), Vector.ZERO, state.rotation(), Vector.ZERO)
                    : FlightMotion.step(state, pending.controls(), FlightBody.tuning(player));
            state = FlightBody.move(player, state, next);
        }
        FlightFrames.put(player, state);
        FlightBody.apply(player, state);
    }

    public static void tick(Player player, boolean restricted) {
        if (owner != player || !FlightFrames.active(player)) return;
        var mc = Minecraft.getInstance();
        boolean blocked = restricted || mc.screen != null;
        var keys = mc.options;
        var controls = blocked ? new Controls(0, 0, 0, 0, 0, false, true)
                : new Controls((float) pitch, (keys.keyRight.isDown() ? 1 : 0) - (keys.keyLeft.isDown() ? 1 : 0), (float) roll,
                    (keys.keyUp.isDown() ? 1 : 0) - (keys.keyDown.isDown() ? 1 : 0),
                    (keys.keyJump.isDown() ? 1 : 0) - (keys.keyShift.isDown() ? 1 : 0), keys.keySprint.isDown(), keys.keyDown.isDown());
        pitch = roll = 0;
        var data = com.dragonminez.common.stats.StatsProvider.get(com.dragonminez.common.stats.StatsCapability.INSTANCE, player).orElse(null);
        if (data != null && !player.isCreative() && data.getResources().getCurrentEnergy() < ConfigManager.getRuntime().flight().resolve(data).charge(controls)) restricted = true;
        if (PENDING.size() >= 64) return;
        sequence++;
        PENDING.addLast(new Pending(sequence, controls, restricted));
        NetworkHandler.sendToServer(new FlightInputC2S(session, sequence, controls));
        var state = FlightFrames.get(player);
        var next = restricted ? new State(state.position(), Vector.ZERO, state.rotation(), Vector.ZERO)
                : FlightMotion.step(state, controls, FlightBody.tuning(player));
        state = FlightBody.move(player, state, next);
        FlightFrames.put(player, state); FlightBody.apply(player, state);
    }
}
