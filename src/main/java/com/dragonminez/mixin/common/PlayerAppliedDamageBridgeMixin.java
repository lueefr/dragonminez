package com.dragonminez.mixin.common;

import com.dragonminez.platform.event.living.DamagePhases;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(net.minecraft.world.entity.player.Player.class)
public abstract class PlayerAppliedDamageBridgeMixin {
    @WrapMethod(method = "actuallyHurt")
    private void dmz$damagePhases(ServerLevel level, DamageSource source, float amount, Operation<Void> original) {
        DamagePhases.around((LivingEntity) (Object) this, source, () -> original.call(level, source, amount));
    }
    @ModifyVariable(method = "actuallyHurt", index = 3, argsOnly = true,
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setAbsorptionAmount(F)V", ordinal = 0, shift = At.Shift.AFTER)),
            at = @At(value = "LOAD", ordinal = 1))
    private float dmz$beforeHealthDamage(float amount, ServerLevel level, DamageSource source, float unused) {
        return DamagePhases.beforeApplied((LivingEntity) (Object) this, source, amount);
    }
}
