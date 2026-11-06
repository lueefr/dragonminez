package com.dragonminez.common.flight;

import com.dragonminez.Reference;
import com.dragonminez.api.flight.FlightInput;
import com.dragonminez.api.flight.FlightMotion;
import com.dragonminez.api.flight.FlightMotion.*;
import com.dragonminez.api.rules.Conditions;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.FlightStateS2C;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.common.stats.techniques.TechniqueDispatcher;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.concurrent.atomic.AtomicLong;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class FlightServer {
    private static final AtomicLong SERIAL = new AtomicLong();
    private static final IdentityStore<ServerPlayer, Session> SESSIONS = new IdentityStore<>();
    private static final class Session {
        final long id = SERIAL.incrementAndGet();
        final FlightInput input = new FlightInput(id);
        final ResourceKey<Level> dimension;
        State state;
        boolean boost;
        long tick = Long.MIN_VALUE;
        Session(ServerPlayer player) {
            dimension = player.level().dimension();
            state = FlightMotion.initial(FlightBody.vector(player.position()), FlightBody.vector(player.getDeltaMovement()), player.getYRot(), player.getXRot());
        }
    }
    private FlightServer() {}

    public static boolean eligible(ServerPlayer player, StatsData data) {
        var config = ConfigManager.getRuntime().flight();
        return config.enabled() && player.isAlive() && !player.isSpectator() && !player.isPassenger()
                && data.getStatus().isHasCreatedCharacter() && data.getSkills().isSkillActive("fly")
                && data.getSkills().getSkillLevel("fly") > 0 && data.getStatus().getFlightMode() == Status.FLIGHT_SEARCH
                && !data.getStatus().isStrikeLocked() && Conditions.test(new Conditions.Context(data, null), config.when());
    }

    public static boolean accept(ServerPlayer player, long session, long sequence, Controls controls) {
        var current = SESSIONS.get(player);
        return current != null && current.dimension == player.level().dimension()
                && current.input.accept(session, sequence, player.level().getGameTime(), controls);
    }

    public static boolean boosted(ServerPlayer player) {
        var session = SESSIONS.get(player);
        return session != null && session.boost;
    }

    public static boolean impulse(ServerPlayer player, net.minecraft.world.phys.Vec3 velocity) {
        var session = SESSIONS.get(player);
        if (session == null) return false;
        var motion = FlightBody.vector(velocity);
        if (motion.length() > 32) motion = motion.scale(32 / motion.length());
        session.state = new State(session.state.position(), motion, session.state.rotation(), session.state.angular());
        FlightFrames.put(player, session.state);
        NetworkHandler.sendToTrackingEntityAndSelf(new FlightStateS2C(player.getId(), session.id, session.input.sequence(),
                player.level().dimension().location(), true, session.state), player);
        return true;
    }

    public static void stop(ServerPlayer player) {
        var session = SESSIONS.remove(player);
        FlightFrames.remove(player);
        if (session != null && !player.hasDisconnected()) NetworkHandler.sendToTrackingEntityAndSelf(
                new FlightStateS2C(player.getId(), session.id, session.input.sequence(), player.level().dimension().location(), false, session.state), player);
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) stop(player);
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) stop(player);
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer player) stop(player);
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        for (var player : event.getServer().getPlayerList().getPlayers()) advance(player);
    }

    public static void advance(ServerPlayer player) {
        var data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        if (data == null || !eligible(player, data) || ((FlightConnection) player.connection).awaiting()) {
            stop(player); return;
        }
        var session = SESSIONS.get(player);
        if (session != null && (session.dimension != player.level().dimension()
                || player.position().distanceToSqr(FlightBody.vector(session.state.position())) > .01)) {
            stop(player); return;
        }
        if (session == null) {
            session = new Session(player);
            if (!FlightBody.clear(player, session.state)) return;
            SESSIONS.put(player, session);
        }
        if (session.tick == player.level().getGameTime()) return;
        session.tick = player.level().getGameTime();
        var config = ConfigManager.getRuntime().flight().resolve(data);
        var controls = session.input.controls(player.level().getGameTime());
        boolean restricted = data.getStatus().isStunned() || data.getStatus().isActionCharging()
                || data.getStatus().isChargingKi() || TechniqueDispatcher.isMovementRestrictedKiAttack(player, data);
        double cost = config.charge(controls);
        if (!player.isCreative() && data.getResources().getCurrentEnergy() < cost) restricted = true;
        State next = restricted ? new State(session.state.position(), Vector.ZERO, session.state.rotation(), Vector.ZERO)
                : FlightMotion.step(session.state, controls, FlightBody.tuning(player));
        var resolved = FlightBody.move(player, session.state, next);
        player.horizontalCollision = Math.abs(resolved.velocity().x()-next.velocity().x())>1e-8 || Math.abs(resolved.velocity().z()-next.velocity().z())>1e-8;
        player.verticalCollision = Math.abs(resolved.velocity().y()-next.velocity().y())>1e-8;
        session.state = resolved;
        session.boost = !restricted && controls.boost();
        player.setSprinting(session.boost);
        if (!restricted && !player.isCreative() && cost > 0) data.getResources().removeEnergy((float) cost);
        FlightFrames.put(player, resolved);
        FlightBody.apply(player, resolved);
        ((FlightConnection) player.connection).anchor();
        NetworkHandler.sendToTrackingEntityAndSelf(new FlightStateS2C(player.getId(), session.id, session.input.sequence(),
                player.level().dimension().location(), true, resolved), player);
    }
}
