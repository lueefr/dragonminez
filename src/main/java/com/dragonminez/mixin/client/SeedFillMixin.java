package com.dragonminez.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "mobius.voxyseedgen.voxy.SeedFill", remap = false)
public abstract class SeedFillMixin {
    @Inject(method = "start(IIZ)Lnet/minecraft/network/chat/Component;", at = @At("HEAD"), cancellable = true, require = 0)
    private static void guard(int radius, int threads, boolean announce, CallbackInfoReturnable<Component> result) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.dimension().location().toString().equals("dragonminez:otherworld")) {
            result.setReturnValue(Component.translatable("message.dragonminez.seed.otherworld"));
        }
    }
}
