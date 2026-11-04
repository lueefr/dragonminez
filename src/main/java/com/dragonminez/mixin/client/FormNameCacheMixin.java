package com.dragonminez.mixin.client;

import com.dragonminez.client.text.FormNamePalette;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.util.FormattedCharSequence;

/** Existing labels refresh after language, synchronized config or base-hair changes. */
@Mixin(MutableComponent.class)
public abstract class FormNameCacheMixin {
    @Shadow private Language decomposedWith;
    @Unique private long dragonminez$formRevision=-1;
    @Inject(method="getVisualOrderText",at=@At("HEAD"))
    private void dragonminez$refreshNames(CallbackInfoReturnable<FormattedCharSequence> cir) {
        if(!RenderSystem.isOnRenderThread())return;
        long revision=FormNamePalette.revision();
        if(revision!=dragonminez$formRevision){decomposedWith=null;dragonminez$formRevision=revision;}
    }
}
