package com.dragonminez.mixin.client;
import com.dragonminez.platform.event.GameEvents;
import com.dragonminez.platform.event.client.ViewportEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Camera.class)
public abstract class ViewportCameraBridgeMixin {
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow @Final private Quaternionf rotation;
    @Shadow @Final private Vector3f forwards;
    @Shadow @Final private Vector3f up;
    @Shadow @Final private Vector3f left;
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void dmz$fov(float partialTick, CallbackInfoReturnable<Float> callback) {
        callback.setReturnValue((float) GameEvents.BUS.post(new ViewportEvent.ComputeFov((Camera) (Object) this,
                partialTick, callback.getReturnValue())).getFOV());
    }
    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;alignWithEntity(F)V", shift = At.Shift.AFTER))
    private void dmz$angles(DeltaTracker delta, CallbackInfo callback) {
        var camera = (Camera) (Object) this;
        var event = new ViewportEvent.ComputeCameraAngles(camera,
                camera.getCameraEntityPartialTicks(delta), camera.yRot(), camera.xRot());
        if (camera instanceof com.dragonminez.client.flight.RollCamera roll) event.setRoll(roll.dragonminez$getRoll());
        GameEvents.BUS.post(event);
        setRotation(event.getYaw(), event.getPitch());
        if (event.getRoll() != 0) {
            rotation.rotateZ(-event.getRoll() * ((float) Math.PI / 180));
            forwards.set(0, 0, -1).rotate(rotation);
            up.set(0, 1, 0).rotate(rotation);
            left.set(-1, 0, 0).rotate(rotation);
        }
    }
}
