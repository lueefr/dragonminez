package com.dragonminez.mixin.client;
import com.dragonminez.platform.event.GameEvents;
import com.dragonminez.platform.event.client.ViewportEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(FogRenderer.class)
public abstract class ViewportFogBridgeMixin {
    @Shadow private FogType getFogType(Camera camera) { throw new AssertionError("Mixin shadow"); }
    @Inject(method = "computeFogColor", at = @At("RETURN"))
    private void dmz$color(Camera camera, float partialTick, ClientLevel level, int distance, float darken,
                           Vector4f color, CallbackInfo callback) {
        var event = GameEvents.BUS.post(new ViewportEvent.ComputeFogColor(camera, partialTick, color.x, color.y, color.z));
        color.set(event.getRed(), event.getGreen(), event.getBlue(), color.w);
    }
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void dmz$fog(Camera camera, int distance, DeltaTracker delta, float darken, ClientLevel level,
                         CallbackInfoReturnable<FogData> callback) {
        var data = callback.getReturnValue();
        var event = GameEvents.BUS.post(new ViewportEvent.RenderFog(camera, camera.getCameraEntityPartialTicks(delta),
                getFogType(camera), Math.min(data.environmentalStart, data.renderDistanceStart),
                Math.min(data.environmentalEnd, data.renderDistanceEnd)));
        if (event.isCanceled()) { data.environmentalStart = event.getNearPlaneDistance(); data.environmentalEnd = event.getFarPlaneDistance(); }
    }
}
