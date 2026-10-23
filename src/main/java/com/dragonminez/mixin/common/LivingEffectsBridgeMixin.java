package com.dragonminez.mixin.common;

import com.dragonminez.platform.event.GameEvents;
import com.dragonminez.platform.event.living.MobEffectEvent;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Collection;
import java.util.List;

@Mixin(LivingEntity.class)
public abstract class LivingEffectsBridgeMixin {
    @Inject(method = "onEffectAdded", at = @At("RETURN"))
    private void dmz$added(MobEffectInstance effect, Entity source, CallbackInfo callback) {
        GameEvents.BUS.post(new MobEffectEvent.Added((LivingEntity) (Object) this, effect));
    }
    @WrapMethod(method = "removeEffect")
    private boolean dmz$remove(Holder<MobEffect> effect, Operation<Boolean> original) {
        var entity = (LivingEntity) (Object) this;
        if (!entity.hasEffect(effect)) return original.call(effect);
        var event = GameEvents.BUS.post(new MobEffectEvent.Remove(entity, effect));
        return !event.isCanceled() && original.call(effect);
    }
    @WrapMethod(method = "removeAllEffects")
    private boolean dmz$removeAll(Operation<Boolean> original) {
        var entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide()) return original.call();
        boolean removed = false;
        for (var effect : List.copyOf(entity.getActiveEffects())) removed |= entity.removeEffect(effect.getEffect());
        return removed;
    }
    @WrapOperation(method = "tickEffects", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;onEffectsRemoved(Ljava/util/Collection;)V"))
    private void dmz$expired(LivingEntity entity, Collection<MobEffectInstance> effects, Operation<Void> original) {
        // The native iterator already removed these effects. Notify before normal attribute cleanup;
        // explicit removal has a separate path and must not trigger expiration-driven content.
        for (var effect : effects) GameEvents.BUS.post(new MobEffectEvent.Expired(entity, effect));
        original.call(entity, effects);
    }
}
