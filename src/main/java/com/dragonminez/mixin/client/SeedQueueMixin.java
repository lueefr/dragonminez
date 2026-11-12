package com.dragonminez.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "mobius.seedrenderer.dh.ClientTerrain", remap = false)
public abstract class SeedQueueMixin {
    @Inject(method = "queueFor", at = @At("HEAD"), cancellable = true, require = 0)
    private static void guard(ClientLevel level, CallbackInfoReturnable<Object> result) {
        if (level.dimension().location().toString().equals("dragonminez:otherworld")) result.setReturnValue(null);
    }
}
