package com.dragonminez.mixin.client;

import com.dragonminez.api.forms.FormAccess;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Color the form component itself, including names inside translated arguments. */
@Mixin(TranslatableContents.class)
public abstract class FormNameColorMixin {
    @Shadow @Final private String key;
    @Shadow private net.minecraft.locale.Language decomposedWith;
    @Unique private long dragonminez$variantRevision=-1;
    @Inject(method="decompose",at=@At("HEAD"))
    private void dragonminez$refreshVariant(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if(!com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread())return;
        long revision=com.dragonminez.client.text.FormNamePalette.revision();
        if(revision!=dragonminez$variantRevision){decomposedWith=null;dragonminez$variantRevision=revision;}
    }

    @ModifyVariable(method="visit(Lnet/minecraft/network/chat/FormattedText$StyledContentConsumer;Lnet/minecraft/network/chat/Style;)Ljava/util/Optional;",at=@At("HEAD"),argsOnly=true)
    private Style dragonminez$hairColor(Style style) {
        if(!com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()||!key.startsWith("race.dragonminez.")||!key.contains(".form."))return style;
        var player=Minecraft.getInstance().player;
        var data=player==null?null:StatsProvider.get(StatsCapability.INSTANCE,player).orElse(null);
        Integer color=FormAccess.colorForKey(data,key);
        return color==null?style:com.dragonminez.client.text.FormNameText.mark(style,color);
    }
    @ModifyVariable(method="visit(Lnet/minecraft/network/chat/FormattedText$StyledContentConsumer;Lnet/minecraft/network/chat/Style;)Ljava/util/Optional;",at=@At("HEAD"),argsOnly=true)
    private <T> net.minecraft.network.chat.FormattedText.StyledContentConsumer<T> dragonminez$inlineNames(net.minecraft.network.chat.FormattedText.StyledContentConsumer<T> visitor) {
        if(!com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()||!key.contains(".dragonminez.")&&!key.startsWith("dmz."))return visitor;
        return (style,text)->com.dragonminez.client.text.FormNamePalette.visit(visitor,style,text);
    }

}
