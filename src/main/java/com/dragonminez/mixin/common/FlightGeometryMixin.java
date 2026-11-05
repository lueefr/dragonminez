package com.dragonminez.mixin.common;

import com.dragonminez.common.flight.*;
import com.dragonminez.api.flight.FlightMotion.Vector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class FlightGeometryMixin {
    @Inject(method = "getBbHeight", at = @At("HEAD"), cancellable = true)
    private void flightHeight(CallbackInfoReturnable<Float> callback) {
        if ((Object) this instanceof Player player && FlightFrames.active(player)) callback.setReturnValue((float) player.getBoundingBox().getYsize());
    }

    @Inject(method = "getBbWidth", at = @At("HEAD"), cancellable = true)
    private void flightWidth(CallbackInfoReturnable<Float> callback) {
        if ((Object) this instanceof Player player && FlightFrames.active(player)) callback.setReturnValue((float) player.getBoundingBox().getXsize());
    }

    @Inject(method = "getEyeHeight()F", at = @At("HEAD"), cancellable = true)
    private void flightSight(CallbackInfoReturnable<Float> callback) {
        if ((Object) this instanceof Player player && FlightFrames.active(player)) callback.setReturnValue((float) (player.getEyePosition().y - player.getY()));
    }

    @Inject(method = "getEyeY", at = @At("HEAD"), cancellable = true)
    private void flightAltitude(CallbackInfoReturnable<Double> callback) {
        if ((Object) this instanceof Player player && FlightFrames.active(player)) callback.setReturnValue(player.getEyePosition().y);
    }

    @Inject(method = "makeBoundingBox", at = @At("HEAD"), cancellable = true)
    private void flightBounds(CallbackInfoReturnable<AABB> callback) {
        if ((Object) this instanceof Player player) {
            var state = FlightFrames.get(player);
            if (state != null) {
                var current = new com.dragonminez.api.flight.FlightMotion.State(FlightBody.vector(player.position()), state.velocity(), state.rotation(), state.angular());
                callback.setReturnValue(FlightBody.bounds(FlightBody.shape(player, current)));
            }
        }
    }

    @Inject(method = "getEyePosition()Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void flightEye(CallbackInfoReturnable<Vec3> callback) {
        if ((Object) this instanceof Player player) {
            var state = FlightFrames.get(player);
            if (state != null) {
                var shape = FlightBody.shape(player, state);
                var head = shape.rotation().rotate(new Vector(0, 0, -shape.extent().z() * .8));
                callback.setReturnValue(FlightBody.vector(shape.center().add(head)));
            }
        }
    }

    @Inject(method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void flightEye(float partial, CallbackInfoReturnable<Vec3> callback) {
        if ((Object) this instanceof Player player && FlightFrames.active(player)) {
            var eye = player.getEyePosition();
            callback.setReturnValue(eye.add((player.xo - player.getX()) * (1 - partial),
                    (player.yo - player.getY()) * (1 - partial), (player.zo - player.getZ()) * (1 - partial)));
        }
    }
}
