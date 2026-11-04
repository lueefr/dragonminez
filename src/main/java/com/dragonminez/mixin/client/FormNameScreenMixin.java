package com.dragonminez.mixin.client;

import com.dragonminez.client.text.FormNameText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class FormNameScreenMixin {
    @Inject(method="renderWithTooltip",at=@At("HEAD"))
    private void dragonminez$beginGui(GuiGraphics graphics,int x,int y,float tick,CallbackInfo ci){FormNameText.enterScreen(this);}
    @Inject(method="renderWithTooltip",at=@At("RETURN"))
    private void dragonminez$endGui(GuiGraphics graphics,int x,int y,float tick,CallbackInfo ci){FormNameText.leaveScreen();}
}
