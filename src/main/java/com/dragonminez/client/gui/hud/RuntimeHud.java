package com.dragonminez.client.gui.hud;

import com.dragonminez.Reference;
import com.dragonminez.api.lang.ConfigTranslations;
import com.dragonminez.api.rules.Conditions;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid=Reference.MOD_ID,value=Dist.CLIENT)
public final class RuntimeHud {
    private RuntimeHud() {}
    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||mc.options.hideGui||mc.player.isSpectator())return;
        var bars=ConfigManager.getRuntime().hud();
        if(bars.isEmpty())return;
        var data=StatsProvider.get(StatsCapability.INSTANCE,mc.player).orElse(null);
        if(data==null||!data.getStatus().isHasCreatedCharacter())return;
        var graphics=event.getGuiGraphics();
        for(var bar:bars){
            if(!data.getRuntime().has(bar.meter())||!Conditions.test(new Conditions.Context(data,null),bar.when()))continue;
            double value=data.getRuntime().value(bar.meter());
            int[] box=bar.bounds(graphics.guiWidth(),graphics.guiHeight());
            graphics.fill(box[0],box[1],box[0]+box[2],box[1]+box[3],bar.background());
            int filled=(int)Math.round(box[2]*bar.fraction(value));
            if(filled>0)graphics.fill(box[0],box[1],box[0]+filled,box[1]+box[3],bar.color());
            Component label=ConfigTranslations.text(bar.label());
            if(bar.showValue())label=label.copy().append(label.getString().isEmpty()?"":" ").append(Component.translatable("format.dragonminez.meter.value",String.format(java.util.Locale.ROOT,"%.0f",value)));
            if(!label.getString().isEmpty()){
                int width=mc.font.width(label), x=Math.clamp(box[0],0,Math.max(0,graphics.guiWidth()-width));
                int y=box[1]>=mc.font.lineHeight?box[1]-mc.font.lineHeight:Math.min(graphics.guiHeight()-mc.font.lineHeight,box[1]+box[3]);
                graphics.drawString(mc.font,label,x,Math.max(0,y),0xFFFFFFFF,true);
            }
        }
    }
}
