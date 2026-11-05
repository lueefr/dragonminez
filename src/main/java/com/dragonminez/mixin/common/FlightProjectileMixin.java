package com.dragonminez.mixin.common;

import com.dragonminez.common.flight.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;
import java.util.function.Predicate;

@Mixin(ProjectileUtil.class)
public abstract class FlightProjectileMixin {
    @Unique private static boolean flightActive(Entity entity) {
        return entity instanceof Player player && FlightFrames.active(player);
    }

    @Unique private static Optional<Vec3> flightClip(Entity entity, Vec3 start, Vec3 end, double padding) {
        if (entity instanceof Player player) {
            var state = FlightFrames.get(player);
            if (state != null) {
                var fraction = FlightBody.shape(player, state).clip(FlightBody.vector(start), FlightBody.vector(end), padding);
                return fraction.isEmpty() ? Optional.empty() : Optional.of(start.lerp(end, fraction.getAsDouble()));
            }
        }
        return entity.getBoundingBox().inflate(padding).clip(start, end);
    }

    @Inject(method = "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;", at = @At("HEAD"), cancellable = true)
    private static void flightPick(Entity shooter, Vec3 start, Vec3 end, AABB bounds, Predicate<Entity> filter,
                                   double distance, CallbackInfoReturnable<EntityHitResult> callback) {
        if (!FlightFrames.any(shooter.level())) return;
        var candidates = shooter.level().getEntities(shooter, bounds, filter);
        if (candidates.stream().noneMatch(FlightProjectileMixin::flightActive)) return;
        Entity selected = null;
        Vec3 point = null;
        for (var entity : candidates) {
            var hit = flightClip(entity, start, end, entity.getPickRadius());
            boolean inside;
            if (entity instanceof Player player && FlightFrames.active(player)) {
                inside = FlightBody.shape(player, FlightFrames.get(player)).contains(FlightBody.vector(start));
            } else inside = entity.getBoundingBox().inflate(entity.getPickRadius()).contains(start);
            if (inside && distance >= 0) {
                selected = entity; point = hit.orElse(start); distance = 0;
            } else if (hit.isPresent()) {
                double length = start.distanceToSqr(hit.get());
                if (length < distance || distance == 0) {
                    if (entity.getRootVehicle() == shooter.getRootVehicle() && !entity.canRiderInteract()) {
                        if (distance == 0) { selected = entity; point = hit.get(); }
                    } else { selected = entity; point = hit.get(); distance = length; }
                }
            }
        }
        callback.setReturnValue(selected == null ? null : new EntityHitResult(selected, point));
    }

    @Inject(method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;", at = @At("HEAD"), cancellable = true)
    private static void flightHit(Level level, Entity projectile, Vec3 start, Vec3 end, AABB bounds,
                                  Predicate<Entity> filter, float padding, CallbackInfoReturnable<EntityHitResult> callback) {
        if (!FlightFrames.any(level)) return;
        var candidates = level.getEntities(projectile, bounds, filter);
        if (candidates.stream().noneMatch(FlightProjectileMixin::flightActive)) return;
        Entity selected = null;
        Vec3 point = null;
        double distance = Double.MAX_VALUE;
        for (var entity : candidates) {
            var hit = flightClip(entity, start, end, padding);
            if (hit.isPresent() && start.distanceToSqr(hit.get()) < distance) {
                selected = entity; point = hit.get(); distance = start.distanceToSqr(point);
            }
        }
        callback.setReturnValue(selected == null ? null : flightActive(selected) ? new EntityHitResult(selected, point) : new EntityHitResult(selected));
    }
}
