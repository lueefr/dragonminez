package com.dragonminez.mixin.common;
import com.dragonminez.platform.event.GameEvents;
import com.dragonminez.platform.event.entity.ProjectileImpactEvent;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Projectile.class)
public abstract class ProjectileImpactBridgeMixin {
    @Inject(method = "hitTargetOrDeflectSelf", at = @At("HEAD"), cancellable = true)
    private void dmz$impact(HitResult hit, CallbackInfoReturnable<ProjectileDeflection> callback) {
        if (hit.getType() != HitResult.Type.MISS && GameEvents.BUS.post(
                new ProjectileImpactEvent((Projectile) (Object) this, hit)).isCanceled())
            callback.setReturnValue(ProjectileDeflection.NONE);
    }
}
