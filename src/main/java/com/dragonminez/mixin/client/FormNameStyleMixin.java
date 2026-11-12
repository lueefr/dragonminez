package com.dragonminez.mixin.client;

import com.dragonminez.client.text.FormNameStyle;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Style.class)
public abstract class FormNameStyleMixin implements FormNameStyle {
    @Shadow @Final private TextColor color;
    @Shadow @Final private Boolean bold,italic,underlined,strikethrough,obfuscated;
    @Shadow @Final private ClickEvent clickEvent;
    @Shadow @Final private HoverEvent hoverEvent;
    @Shadow @Final private String insertion;
    @Shadow @Final private ResourceLocation font;
    @Unique private Integer dragonminez$formColor;

    @Invoker("<init>")
    private static Style dragonminez$newStyle(TextColor color,Boolean bold,Boolean italic,Boolean underlined,Boolean strikethrough,Boolean obfuscated,ClickEvent click,HoverEvent hover,String insertion,ResourceLocation font){throw new AssertionError();}
    public Integer dragonminez$formColor(){return dragonminez$formColor;}
    public void dragonminez$setColor(Integer color){dragonminez$formColor=color;}
    public Style dragonminez$copyColor(Integer rgb) {
        Style copy=dragonminez$newStyle(color,bold,italic,underlined,strikethrough,obfuscated,clickEvent,hoverEvent,insertion,font);
        ((FormNameStyle)copy).dragonminez$setColor(rgb);
        return copy;
    }
    @Inject(method={"withColor","withBold","withItalic","withUnderlined","withStrikethrough","withObfuscated","withClickEvent","withHoverEvent","withInsertion","withFont","applyFormat","applyLegacyFormat","applyFormats"},at=@At("RETURN"),cancellable=true)
    private void dragonminez$preserveColor(CallbackInfoReturnable<Style> cir) {
        if(dragonminez$formColor!=null&&cir.getReturnValue()!=(Object)this)
            cir.setReturnValue(((FormNameStyle)cir.getReturnValue()).dragonminez$copyColor(dragonminez$formColor));
    }
    @Inject(method="applyTo",at=@At("RETURN"),cancellable=true)
    private void dragonminez$inheritColor(Style parent,CallbackInfoReturnable<Style> cir) {
        Integer rgb=dragonminez$formColor!=null?dragonminez$formColor:((FormNameStyle)parent).dragonminez$formColor();
        if(rgb!=null)cir.setReturnValue(((FormNameStyle)cir.getReturnValue()).dragonminez$copyColor(rgb));
    }
}
