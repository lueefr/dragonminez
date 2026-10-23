package com.dragonminez.mixin.client;

import com.dragonminez.client.flight.RollCamera;
import com.dragonminez.client.gui.tooltip.ScrollTracker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Shadow @Final private Camera mainCamera;


    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;rotation()Lorg/joml/Quaternionf;"))
    private org.joml.Quaternionf dragonminez$applyFlightRoll(org.joml.Quaternionf rotation) {
        float roll = ((RollCamera) mainCamera).dragonminez$getRoll();
        return Math.abs(roll) > 0.01F ? new org.joml.Quaternionf(rotation).rotateZ(-roll * ((float)Math.PI / 180.0F)) : rotation;
    }

	@Inject(method = "render", at = @At("RETURN"))
	private void dragonminez$checkTooltipRendered(CallbackInfo ci) {
		if (!ScrollTracker.renderedThisFrame) ScrollTracker.reset();
		ScrollTracker.renderedThisFrame = false;
	}
}
