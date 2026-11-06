package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.common.stats.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.Map;

/** Only committed server damage can feed the runtime damage events. */
@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class RuntimeEvents {
    private RuntimeEvents() {}
    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (data.getStatus().isHasCreatedCharacter()) {
                com.dragonminez.common.config.ConfigManager.getAttributes().capture(data);
                data.getRuntime().tick(data);
            }
        });
    }
    @SubscribeEvent
    public static void completed(com.dragonminez.api.content.ContentCompletedEvent event) {
        StatsProvider.get(StatsCapability.INSTANCE, event.getPlayer()).ifPresent(data -> com.dragonminez.common.config.ConfigManager.getAttributes().capture(data));
    }
    @SubscribeEvent
    public static void cast(com.dragonminez.common.events.DMZEvent.KiAttackCastEvent event) {
        if (event.getStatsData().getStatus().isHasCreatedCharacter()) event.getStatsData().getRuntime().startAttack(event.getStatsData(), event.getKiAttack().getId());
    }
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void fire(com.dragonminez.common.events.DMZEvent.KiAttackFireEvent event) {
        if (event.getStatsData().getStatus().isHasCreatedCharacter()) event.getStatsData().getRuntime().releaseAttack(event.getStatsData(), event.getKiAttack().getId(), event.getChargeMultiplier() * 100);
    }
    @SubscribeEvent
    public static void strikeCast(com.dragonminez.common.events.DMZEvent.StrikeAttackCastEvent event) {
        if(event.getStatsData().getStatus().isHasCreatedCharacter())event.getStatsData().getRuntime().startStrike(event.getStatsData(),event.getStrike().getId());
    }
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
    public static void strikeFire(com.dragonminez.common.events.DMZEvent.StrikeAttackFireEvent event) {
        if(event.getStatsData().getStatus().isHasCreatedCharacter())event.getStatsData().getRuntime().releaseAttack(event.getStatsData(),event.getStrike().getId(),100);
    }
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void meleeGate(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer))return;
        StatsProvider.get(StatsCapability.INSTANCE,event.getEntity()).ifPresent(data->{if(!data.getRuntime().permits("melee"))event.setCanceled(true);});
    }
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
    public static void meleePhase(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer))return;
        StatsProvider.get(StatsCapability.INSTANCE,event.getEntity()).ifPresent(data->{if(data.getStatus().isHasCreatedCharacter())data.getRuntime().releaseAttack(data,"dmz:melee",100);});
    }
    @SubscribeEvent
    public static void damage(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide() || event.getNewDamage() <= 0 || !Float.isFinite(event.getNewDamage())) return;
        Map<String, Double> values = Map.of("event.damage", (double) event.getNewDamage(), "event.originalDamage", (double) event.getOriginalDamage(), "event.blockedDamage", (double) event.getBlockedDamage());
        if (event.getEntity() instanceof ServerPlayer player) apply(player, "dmz:damage_received", values);
        if (event.getSource().getEntity() instanceof ServerPlayer player && player != event.getEntity()) apply(player, "dmz:damage_dealt", values);
    }
    private static void apply(ServerPlayer player, String event, Map<String, Double> values) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (!data.getStatus().isHasCreatedCharacter()) return;
            data.getRuntime().contact(data); data.getRuntime().dispatch(data, event, values);
        });
    }
}
