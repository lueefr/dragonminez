package com.dragonminez.mixin.client;

import com.dragonminez.client.text.FormNameText;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only the vanilla hotbar popup opts in, never chat or unrelated HUD labels. */
@Mixin(Gui.class)
public abstract class FormPopupMixin {
    @Inject(method="renderOverlayMessage",at=@At("HEAD"))
    private void dragonminez$beginPopup(CallbackInfo ci) { FormNameText.enterPopup(); }
    @Inject(method="renderOverlayMessage",at=@At("RETURN"))
    private void dragonminez$endPopup(CallbackInfo ci) { FormNameText.leavePopup(); }
}
