package com.dragonminez.mixin.common;

import com.dragonminez.api.lang.ConfigTranslations;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TranslatableContents.class)
public abstract class ConfigTranslationCacheMixin {
    @Shadow private Language decomposedWith;
    @Unique private long dragonminez$translationRevision = -1;
    @Inject(method = "decompose", at = @At("HEAD"))
    private void dragonminez$invalidateConfigNames(CallbackInfo ci) {
        long revision = ConfigTranslations.INSTANCE.revision();
        if (dragonminez$translationRevision != revision) {
            decomposedWith = null;
            dragonminez$translationRevision = revision;
        }
    }
}
