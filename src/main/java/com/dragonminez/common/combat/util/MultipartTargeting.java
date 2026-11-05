package com.dragonminez.common.combat.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

import java.util.ArrayList;
import java.util.List;

public final class MultipartTargeting {

    private MultipartTargeting() {
    }

    public static List<LivingEntity> collectTargets(Level level, AABB box) {
        List<LivingEntity> out = level.getEntitiesOfClass(LivingEntity.class, box);
        if (level instanceof ServerLevel serverLevel) {
            for (PartEntity<?> part : serverLevel.getPartEntities()) {
                if (part.getParent() instanceof LivingEntity parent
                        && part.getBoundingBox().intersects(box)
                        && !out.contains(parent)) {
                    out.add(parent);
                }
            }
        }
        return out;
    }

    public static List<AABB> hitBoxes(Entity target) {
        if (target.isMultipartEntity() && target.getParts() != null) {
            List<AABB> boxes = new ArrayList<>();
            for (PartEntity<?> part : target.getParts()) {
                boxes.add(part.getBoundingBox());
            }
            if (!boxes.isEmpty()) return boxes;
        }
        return List.of(target.getBoundingBox());
    }

    public static boolean withinRadius(Entity target, Vec3 center, double radius) {
        if (target instanceof net.minecraft.world.entity.player.Player player) {
            var state = com.dragonminez.common.flight.FlightFrames.get(player);
            if (state != null) {
                var shape = com.dragonminez.common.flight.FlightBody.shape(player, state);
                var point = shape.rotation().inverse().rotate(com.dragonminez.common.flight.FlightBody.vector(center).add(shape.center().scale(-1)));
                double x = Math.max(0, Math.abs(point.x()) - shape.extent().x());
                double y = Math.max(0, Math.abs(point.y()) - shape.extent().y());
                double z = Math.max(0, Math.abs(point.z()) - shape.extent().z());
                return x*x + y*y + z*z <= radius*radius;
            }
        }
        double r2 = radius * radius;
        for (AABB box : hitBoxes(target)) {
            double dx = Math.max(Math.max(box.minX - center.x, 0.0), center.x - box.maxX);
            double dy = Math.max(Math.max(box.minY - center.y, 0.0), center.y - box.maxY);
            double dz = Math.max(Math.max(box.minZ - center.z, 0.0), center.z - box.maxZ);
            if (dx * dx + dy * dy + dz * dz <= r2) return true;
        }
        return false;
    }

    public static boolean intersects(Entity target, Vec3 start, Vec3 end, double padding) {
        if (target instanceof net.minecraft.world.entity.player.Player player) {
            var state = com.dragonminez.common.flight.FlightFrames.get(player);
            if (state != null) return com.dragonminez.common.flight.FlightBody.shape(player, state).clip(
                    com.dragonminez.common.flight.FlightBody.vector(start), com.dragonminez.common.flight.FlightBody.vector(end), padding).isPresent();
        }
        for (var box : hitBoxes(target)) {
            var expanded = box.inflate(padding);
            if (expanded.contains(start) || expanded.clip(start, end).isPresent()) return true;
        }
        return false;
    }
}
