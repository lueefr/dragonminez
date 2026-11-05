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
            if (data.getStatus().isHasCreatedCharacter()) data.getRuntime().tick(data);
        });
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
