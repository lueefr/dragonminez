package com.dragonminez.mixin.client;

import com.dragonminez.api.lang.ConfigTranslations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLanguage.class)
public abstract class ConfigLanguageMixin {
    private static String dragonminez$custom(String key) {
        return ConfigTranslations.INSTANCE.find(Minecraft.getInstance().options.languageCode, key);
    }
    @Inject(method = "getOrDefault", at = @At("HEAD"), cancellable = true)
    private void dragonminez$configName(String key, String fallback, CallbackInfoReturnable<String> cir) {
        String value = dragonminez$custom(key);
        if (value != null) cir.setReturnValue(value);
    }
    @Inject(method = "getOrDefault", at = @At("RETURN"))
    private void dragonminez$auditMissing(String key,String fallback,CallbackInfoReturnable<String> cir) {
        if(key.equals(cir.getReturnValue())&&!((ClientLanguage)(Object)this).has(key))
            ConfigTranslations.INSTANCE.recordMissing(Minecraft.getInstance().options.languageCode,key);
    }
    @Inject(method = "has", at = @At("HEAD"), cancellable = true)
    private void dragonminez$hasConfigName(String key, CallbackInfoReturnable<Boolean> cir) {
        if (dragonminez$custom(key) != null) cir.setReturnValue(true);
    }
}
