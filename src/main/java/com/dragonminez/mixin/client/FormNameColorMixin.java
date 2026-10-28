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
    @ModifyVariable(method="visit(Lnet/minecraft/network/chat/FormattedText$StyledContentConsumer;Lnet/minecraft/network/chat/Style;)Ljava/util/Optional;",at=@At("HEAD"),argsOnly=true)
    private Style dragonminez$formHairColor(Style style) {
        if(!key.startsWith("race.dragonminez.")||!key.contains(".form."))return style;
        var player=Minecraft.getInstance().player;
        var data=player==null?null:StatsProvider.get(StatsCapability.INSTANCE,player).orElse(null);
        Integer color=FormAccess.colorForKey(data,key);
        return color==null?style:style.withColor(color);
    }
}
