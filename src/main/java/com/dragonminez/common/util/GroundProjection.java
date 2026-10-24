package com.dragonminez.common.util;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Finds a collision surface beneath the current feet, without tying effects to onGround. */
public final class GroundProjection {
    private GroundProjection() {}

    public static Optional<Vec3> below(Entity entity, Vec3 feet) {
        // Starting just above the feet also hits the surface when standing exactly on it.
        var hit = entity.level().clip(new ClipContext(feet.add(0, 0.01, 0), feet.add(0, -2.001, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) return Optional.empty();
        double distance = feet.y - hit.getLocation().y;
        return distance >= -0.001 && distance <= 2.000001 ? Optional.of(hit.getLocation()) : Optional.empty();
    }
}
