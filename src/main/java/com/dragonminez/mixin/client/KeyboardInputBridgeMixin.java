package com.dragonminez.mixin.client;
import com.dragonminez.platform.event.GameEvents;
import com.dragonminez.platform.event.client.InputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyboardHandler.class)
public abstract class KeyboardInputBridgeMixin {
    @Inject(method = "keyPress", at = @At("RETURN"))
    private void dmz$key(long window, int action, KeyEvent key, CallbackInfo callback) {
        if (window == Minecraft.getInstance().getWindow().handle()) GameEvents.BUS.post(new InputEvent.Key(key, action));
    }
}
