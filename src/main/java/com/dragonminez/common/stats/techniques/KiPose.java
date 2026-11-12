package com.dragonminez.common.stats.techniques;

import com.dragonminez.common.init.entities.ki.*;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Native cast attachments, independent of damage, color and predefined attack behavior. */
public final class KiPose {
    private KiPose() {}

    public static void apply(AbstractKiProjectile projectile, LivingEntity owner) {
        if (projectile.isFiring() && !(projectile instanceof KiWaveEntity) && !(projectile instanceof KiLaserEntity)) return;
        String pose = projectile.castPose();
        if (!projectile.level().isClientSide && !PredefinedTechniques.isPredefinedTechniqueId(projectile.getTechniqueId())) {
            String resolved = StatsProvider.get(StatsCapability.INSTANCE, owner).map(data -> {
                var technique = data.getTechniques().getUnlockedTechniques().get(projectile.getTechniqueId());
                return technique instanceof KiAttackData ki && ki.getAnimation() != null ? ki.getAnimation() : null;
            }).orElse(null);
            if (resolved != null) {
                pose = resolved;
                if (!pose.equals(projectile.castPose())) projectile.castPose(pose);
            }
        }
        if (pose.isEmpty()) return;
        if (projectile instanceof KiBlastEntity ball) ball.poseOffset(owner, pose);
        Vec3 offset = offset(pose, projectile.tickCount);
        if (offset == null) return;
        if (pose.equals("ki.mouth_blast") && projectile instanceof KiWaveEntity) offset = new Vec3(0,-.25,.3);
        if (projectile instanceof KiWaveEntity wave) wave.setCastOffsets((float)offset.x, (float)offset.y, (float)offset.z);
        else if (projectile instanceof KiLaserEntity laser) laser.setCastOffsets((float)offset.x, (float)offset.y, (float)offset.z);
        else if (projectile instanceof KiDiskEntity disk) disk.setCastOffsets((float)offset.x, (float)offset.y, (float)offset.z);
        else if (projectile instanceof KiBlastEntity ball) ball.setCastOffsets((float)offset.x, (float)offset.y, (float)offset.z);
    }

    public static Vec3 offset(String pose, int tick) {
        return switch (pose) {
            case "ki.kameha" -> tick <= 5 ? new Vec3(0, 0.1, 0.5) : tick < 15 ? new Vec3(0, 0.3, 0.5) : new Vec3(0.4, 0.1, 0);
            case "ki.galick" -> tick <= 5 ? new Vec3(0, 0.3, 0.5) : new Vec3(0.2, 0.4, -0.3);
            case "ki.finalflash" -> new Vec3(0, 0.3, 2.5);
            case "ki.masenko" -> new Vec3(0, 0.9, 0.2);
            case "ki.cupula" -> Vec3.ZERO;
            case "ki.mouth_blast" -> new Vec3(0, 0.55, 0.3);
            case "ki.laser", "ki.emperor_laser" -> new Vec3(0.3, 0.4, 0.5);
            case "ki.dodonpa" -> new Vec3(0.3, -0.1, 0.5);
            case "ki.makkako" -> new Vec3(-0.2, 0.7, 0.5);
            case "ki.kikohu" -> new Vec3(0, 0, 0.7);
            case "ki.kienzan", "ki.kienzandoble" -> new Vec3(0.4, 0.7, 0.2);
            case "ki.kill_driver" -> new Vec3(0, 0.7, 0.2);
            case "ki.barrage", "ki.hellzone_grenade" -> new Vec3(0, 0, 0.7);
            case "ki.assault_rain" -> new Vec3(-0.4, 1.3, 0);
            case "ki.blaster_meteor" -> Vec3.ZERO;
            default -> null;
        };
    }
}
