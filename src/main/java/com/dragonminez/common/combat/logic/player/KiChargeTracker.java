package com.dragonminez.common.combat.logic.player;

import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.Reference;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Tracks entities, not spatial proximity: overcharged attacks can be far from their caster. */
@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class KiChargeTracker {
    private static final Map<ServerLevel, Set<UUID>> ENTITIES = new HashMap<>();

    private KiChargeTracker() {}

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof AbstractKiProjectile ki)
            ENTITIES.computeIfAbsent(level, unused -> new HashSet<>()).add(ki.getUUID());
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            Set<UUID> entities = ENTITIES.get(level);
            if (entities != null) entities.remove(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) ENTITIES.remove(level);
    }

    public static List<AbstractKiProjectile> charging(LivingEntity owner) {
        return owned(owner).stream().filter(ki -> !ki.isFiring()).toList();
    }

    public static List<AbstractKiProjectile> owned(LivingEntity owner) {
        List<AbstractKiProjectile> result = new ArrayList<>();
        if (!(owner.level() instanceof ServerLevel level)) return result;
        for (UUID id : List.copyOf(ENTITIES.getOrDefault(level, Set.of()))) {
            if (level.getEntity(id) instanceof AbstractKiProjectile ki && !ki.isRemoved()
                    && ki.getOwner() == owner) result.add(ki);
        }
        return result;
    }

    /** Form-bound mouths stop existing immediately when the caster loses that form. */
    public static boolean validate(AbstractKiProjectile ki) {
        if (!(ki.getOwner() instanceof net.minecraft.world.entity.player.Player owner)) return true;
        if (!owner.isAlive() || owner.isRemoved() || owner.level() != ki.level()) return false;
        String id = ki.getTechniqueId();
        if (!"mouth_blast".equals(id) && !"rage_scream".equals(id)) return true;
        return StatsProvider.get(StatsCapability.INSTANCE, owner)
                .map(stats -> stats.getCharacter().isOozaruCached()).orElse(false);
    }
}
