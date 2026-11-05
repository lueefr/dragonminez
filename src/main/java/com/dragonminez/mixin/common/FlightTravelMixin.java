package com.dragonminez.mixin.common;

import com.dragonminez.common.flight.FlightFrames;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class FlightTravelMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void flightTravel(Vec3 input, CallbackInfo callback) {
        if ((Object) this instanceof Player player && FlightFrames.active(player)) callback.cancel();
    }

    @Inject(method = "knockback", at = @At("TAIL"))
    private void flightImpulse(double strength, double x, double z, CallbackInfo callback) {
        if ((Object) this instanceof net.minecraft.server.level.ServerPlayer player) {
            com.dragonminez.common.flight.FlightServer.impulse(player, player.getDeltaMovement());
        }
    }
}
