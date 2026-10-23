package com.dragonminez.mixin.client;
import com.dragonminez.platform.event.GameEvents;
import com.dragonminez.platform.event.client.InputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MouseHandler.class)
public abstract class MouseInputBridgeMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void dmz$scroll(long window, double x, double y, CallbackInfo callback) {
        var client = Minecraft.getInstance();
        if (window == client.getWindow().handle() && client.screen == null && client.getOverlay() == null
                && GameEvents.BUS.post(new InputEvent.MouseScrollingEvent(x, y)).isCanceled()) callback.cancel();
    }
}
