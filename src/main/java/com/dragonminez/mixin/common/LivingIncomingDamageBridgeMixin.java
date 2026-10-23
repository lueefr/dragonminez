package com.dragonminez.mixin.common;

import com.dragonminez.platform.event.GameEvents;
import com.dragonminez.platform.event.living.LivingIncomingDamageEvent;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingIncomingDamageBridgeMixin {
    @Inject(method = "hurtServer", order = 900, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z"), cancellable = true)
    private void dmz$incoming(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback,
            @Share("dmz$incoming") LocalRef<LivingIncomingDamageEvent> shared) {
        var event = GameEvents.BUS.post(new LivingIncomingDamageEvent((LivingEntity) (Object) this, source, amount));
        shared.set(event);
        if (event.isCanceled()) callback.setReturnValue(false);
    }
    @ModifyVariable(method = "hurtServer", order = 1000, index = 3, argsOnly = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z"))
    private float dmz$incomingAmount(float amount, @Share("dmz$incoming") LocalRef<LivingIncomingDamageEvent> shared) {
        var event = shared.get();
        if (event == null) throw new IllegalStateException("Incoming damage hook ran before event dispatch");
        return event.getAmount();
    }
}
