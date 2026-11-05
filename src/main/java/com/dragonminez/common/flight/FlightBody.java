package com.dragonminez.common.flight;

import com.dragonminez.api.flight.FlightMotion;
import com.dragonminez.api.flight.FlightMotion.*;
import com.dragonminez.api.flight.FlightShape;
import com.dragonminez.common.combat.collision.OrientedBoundingBox;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Prone body geometry; the entity position remains the bottom center of its world bounds. */
public final class FlightBody {
    private record Size(long tick, net.minecraft.world.entity.EntityDimensions dimensions) {}
    private static final IdentityStore<Player, Size> SIZES = new IdentityStore<>();
    private FlightBody() {}

    public static Vector vector(Vec3 value) { return new Vector(value.x, value.y, value.z); }
    public static Vec3 vector(Vector value) { return new Vec3(value.x(), value.y(), value.z()); }

    public static net.minecraft.world.entity.EntityDimensions dimensions(Player player) {
        long tick = player.level().getGameTime();
        var cached = SIZES.get(player);
        if (cached != null && cached.tick() == tick) return cached.dimensions();
        var nativeSize = player.getDimensions(Pose.STANDING);
        SIZES.put(player, new Size(tick, nativeSize));
        var size = net.neoforged.neoforge.event.EventHooks.getEntitySizeForge(player, Pose.STANDING, nativeSize).getNewSize();
        SIZES.put(player, new Size(tick, size));
        return size;
    }

    public static Tuning tuning(Player player) {
        var data = com.dragonminez.common.stats.StatsProvider.get(com.dragonminez.common.stats.StatsCapability.INSTANCE, player).orElse(null);
        var config = com.dragonminez.common.config.ConfigManager.getRuntime().flight();
        var base = (data == null ? config : config.resolve(data)).tuning();
        double factor = 1;
        if (data != null) {
            var attribute = com.dragonminez.common.init.EntityAttributes.FLY_SPEED;
            double speed = player.getAttributes().hasAttribute(attribute) ? player.getAttributeValue(attribute) : .35;
            factor = (speed <= 0 ? 1 : Math.clamp(speed / .35, .25, 4)) * data.getFlightStateSpeedMultiplier()
                    * (1 + .2 * data.getSkills().getSkillLevel("fly"))
                    * Math.clamp(data.getResources().getFlightSpeedLimit() / 100d, 0, 1);
        }
        if (!Double.isFinite(factor)) factor = 0;
        return new Tuning(Math.clamp(base.thrust() * factor, 0, 1), base.drag(), base.braking(), base.gravity(), base.lift(),
                Math.clamp(base.speed() * factor, 1e-6, 8), base.boost(), base.turn(), base.smoothing(), base.banking(), base.pitchRate(), base.yawRate(), base.rollRate());
    }

    public static FlightShape shape(Player player, State state) {
        var size = dimensions(player);
        var extent = new Vector(size.width() / 2, size.width() / 2, size.height() / 2);
        var body = new FlightShape(Vector.ZERO, state.rotation(), extent);
        return new FlightShape(state.position().add(new Vector(0, body.radius().y(), 0)), state.rotation(), extent);
    }

    public static AABB bounds(FlightShape shape) {
        var center = vector(shape.center());
        var radius = vector(shape.radius());
        return new AABB(center.subtract(radius), center.add(radius));
    }

    public static OrientedBoundingBox box(FlightShape shape) {
        var box = new OrientedBoundingBox(bounds(shape));
        box.center = vector(shape.center());
        box.extent = vector(shape.extent());
        box.axisX = vector(shape.rotation().rotate(new Vector(1, 0, 0)));
        box.axisY = vector(shape.rotation().rotate(new Vector(0, 1, 0)));
        box.axisZ = vector(shape.rotation().rotate(new Vector(0, 0, 1)));
        return box.updateVertex();
    }

    public static boolean clear(Player player, State state) {
        var shape = shape(player, state);
        var bounds = bounds(shape).deflate(1e-6);
        if (!player.level().getWorldBorder().isWithinBounds(bounds)
                || bounds.minY < -20000000 || bounds.maxY > 20000000
                || !player.level().hasChunksAt(net.minecraft.core.BlockPos.containing(bounds.minX, player.level().getMinBuildHeight(), bounds.minZ),
                        net.minecraft.core.BlockPos.containing(bounds.maxX, player.level().getMinBuildHeight(), bounds.maxZ))) return false;
        var body = box(shape);
        for (var collision : player.level().getCollisions(player, bounds)) {
            for (var block : collision.toAabbs()) if (body.intersects(block.deflate(1e-7))) return false;
        }
        return true;
    }

    /** Substeps sweep translation and rotation, preventing a fast body crossing thin walls. */
    public static State move(Player player, State before, State after) {
        double distance = after.position().add(before.position().scale(-1)).length();
        double dot = Math.abs(before.rotation().x() * after.rotation().x() + before.rotation().y() * after.rotation().y()
                + before.rotation().z() * after.rotation().z() + before.rotation().w() * after.rotation().w());
        double turn = 2 * Math.acos(Math.clamp(dot, 0, 1));
        double radius = shape(player, before).extent().length();
        double stride = Math.max(.025, Math.min(.15, dimensions(player).width() / 4));
        int steps = (int) Math.ceil((distance + turn * radius) / stride);
        if (steps > 512) return new State(before.position(), Vector.ZERO, before.rotation(), Vector.ZERO);
        State result = before;
        var center = shape(player, before).center();
        var travel = after.position().add(before.position().scale(-1));
        var extent = shape(player, before).extent();
        for (int i = 1; i <= Math.max(1, steps); i++) {
            double ratio = i / (double) Math.max(1, steps);
            var rotation = before.rotation().blend(after.rotation(), ratio);
            var body = new FlightShape(Vector.ZERO, rotation, extent);
            var position = center.add(travel.scale(ratio)).add(new Vector(0, -body.radius().y(), 0));
            var next = new State(position, after.velocity(), rotation, after.angular());
            if (!clear(player, next)) return new State(result.position(), Vector.ZERO, result.rotation(), Vector.ZERO);
            result = next;
        }
        return result;
    }

    public static void apply(Player player, State state) {
        player.setPos(vector(state.position()));
        player.setBoundingBox(bounds(shape(player, state)));
        player.setDeltaMovement(vector(state.velocity()));
        player.setYRot((float) FlightMotion.yaw(state.rotation()));
        player.setXRot((float) FlightMotion.pitch(state.rotation()));
        player.setYHeadRot(player.getYRot());
        player.setYBodyRot(player.getYRot());
        player.setOnGround(false);
        player.resetFallDistance();
    }
}
