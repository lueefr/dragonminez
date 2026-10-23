package com.dragonminez.mixin.client;

import com.dragonminez.platform.event.GameEvents;
import com.dragonminez.platform.event.tick.EntityTickEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelTickBridgeMixin {
    @Inject(method = "tickNonPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V", shift = At.Shift.AFTER))
    private void dmz$afterEntityTick(Entity entity, CallbackInfo callback) {
        GameEvents.BUS.post(new EntityTickEvent.Post(entity));
    }
    @Inject(method = "tickPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;rideTick()V", shift = At.Shift.AFTER))
    private void dmz$afterPassengerTick(Entity vehicle, Entity passenger, CallbackInfo callback) {
        GameEvents.BUS.post(new EntityTickEvent.Post(passenger));
    }
}
