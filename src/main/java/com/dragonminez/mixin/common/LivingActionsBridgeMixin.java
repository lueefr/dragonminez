package com.dragonminez.mixin.common;

import com.dragonminez.platform.event.GameEvents;
import com.dragonminez.platform.event.living.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingActionsBridgeMixin {
    @WrapMethod(method = "causeFallDamage")
    private boolean dmz$fall(double distance, float multiplier, net.minecraft.world.damagesource.DamageSource source,
                             Operation<Boolean> original) {
        var event = GameEvents.BUS.post(new LivingFallEvent((LivingEntity) (Object) this, distance, multiplier));
        return !event.isCanceled() && original.call((double) event.getDistance(), event.getDamageMultiplier(), source);
    }
    @WrapMethod(method = "heal")
    private void dmz$heal(float amount, Operation<Void> original) {
        var event = GameEvents.BUS.post(new LivingHealEvent((LivingEntity) (Object) this, amount));
        if (!event.isCanceled()) original.call(event.getAmount());
    }
    @WrapMethod(method = "knockback")
    private void dmz$knockback(double strength, double x, double z, Operation<Void> original) {
        var event = GameEvents.BUS.post(new LivingKnockBackEvent((LivingEntity) (Object) this, strength, x, z));
        if (!event.isCanceled()) original.call((double) event.getStrength(), event.getRatioX(), event.getRatioZ());
    }
    @Inject(method = "jumpFromGround", at = @At("RETURN"))
    private void dmz$jump(CallbackInfo callback) {
        GameEvents.BUS.post(new LivingEvent.LivingJumpEvent((LivingEntity) (Object) this));
    }
}
