package com.dragonminez.mixin.client;

import com.dragonminez.client.text.FormNameText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Font.class)
public abstract class FormNameFontMixin {
    @Inject(method="drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I",at=@At("HEAD"),cancellable=true)
    private void dragonminez$formOutline(FormattedCharSequence text,float x,float y,int color,boolean shadow,Matrix4f matrix,MultiBufferSource buffers,Font.DisplayMode mode,int background,int light,CallbackInfoReturnable<Integer> cir) {
        Integer result=FormNameText.draw((Font)(Object)this,text,x,y,color,shadow,matrix,buffers,mode,background,light);
        if(result!=null)cir.setReturnValue(result);
    }

}
