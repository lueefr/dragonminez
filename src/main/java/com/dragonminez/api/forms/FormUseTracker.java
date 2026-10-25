package com.dragonminez.api.forms;

import com.dragonminez.common.stats.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid="dragonminez")
public final class FormUseTracker {
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void tick(PlayerTickEvent.Post event) {
        if(event.getEntity() instanceof ServerPlayer player && !com.dragonminez.server.storage.StorageManager.isLoadPending(player))
            StatsProvider.get(StatsCapability.INSTANCE,player).ifPresent(data -> update(player,data));
    }
    public static void update(ServerPlayer player,StatsData data) {
        var c=data.getCharacter();
        update(player,data,"base",c.hasActiveForm()?c.getActiveFormGroup()+"."+c.getActiveForm():"");
        update(player,data,"stack",c.hasActiveStackForm()?c.getActiveStackFormGroup()+"."+c.getActiveStackForm():"");
    }
    private static void update(ServerPlayer player,StatsData data,String channel,String form){
        var history=data.getFormHistory();String race=data.getCharacter().getRaceName();
        long before=history.revision();
        if(history.update(channel,race,form))NeoForge.EVENT_BUS.post(new FormUsedEvent(player,form,history.first(race,form)));
        if(before!=history.revision())com.dragonminez.common.network.S2C.StatsSyncS2C.sendRequested(player);
    }
    private FormUseTracker(){}
}
