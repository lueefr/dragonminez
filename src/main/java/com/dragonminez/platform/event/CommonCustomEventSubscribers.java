package com.dragonminez.platform.event;

import com.dragonminez.common.events.DMZEvent;

/** Explicit subscriptions for the loader-independent DragonMineZ gameplay API. */
public final class CommonCustomEventSubscribers {
    private static boolean installed;
    private CommonCustomEventSubscribers() {}
    public static synchronized void register() {
        if (installed) return;
        GameEvents.BUS.addListener(DMZEvent.HealthRegenEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.passives.PassiveEventHandler::onHealthRegen);
        GameEvents.BUS.addListener(DMZEvent.StaminaRegenEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.passives.PassiveEventHandler::onStaminaRegen);
        GameEvents.BUS.addListener(DMZEvent.CritChanceEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.passives.PassiveEventHandler::onCritChance);
        GameEvents.BUS.addListener(DMZEvent.DamageModifyEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.passives.PassiveEventHandler::onDamageModify);
        GameEvents.BUS.addListener(DMZEvent.DamageDealtEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.passives.PassiveEventHandler::onDamageDealt);
        GameEvents.BUS.addListener(DMZEvent.KiAttackFireEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.passives.PassiveEventHandler::onKiAttackFire);
        GameEvents.BUS.addListener(DMZEvent.StrikeAttackCastEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.passives.PassiveEventHandler::onStrikeCast);
        GameEvents.BUS.addListener(DMZEvent.DragonSummonedEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.QuestEvents::onDragonSummoned);
        GameEvents.BUS.addListener(DMZEvent.HealthRegenEvent.class, EventPriority.HIGHEST, false, com.dragonminez.server.events.players.BabaReviveEvents::onHealthRegen);
        GameEvents.BUS.addListener(DMZEvent.EnergyRegenEvent.class, EventPriority.HIGHEST, false, com.dragonminez.server.events.players.BabaReviveEvents::onEnergyRegen);
        GameEvents.BUS.addListener(DMZEvent.StaminaRegenEvent.class, EventPriority.HIGHEST, false, com.dragonminez.server.events.players.BabaReviveEvents::onStaminaRegen);
        GameEvents.BUS.addListener(DMZEvent.EnergyRegenEvent.class, EventPriority.HIGH, false, com.dragonminez.server.events.players.KiShareService::onEnergyRegen);
        GameEvents.BUS.addListener(DMZEvent.TPGainEvent.class, EventPriority.HIGH, false, com.dragonminez.server.events.players.TPGainEvents::onTPGain);
        GameEvents.BUS.addListener(DMZEvent.PlayerDamageMitigatedEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.world.worldboss.WorldBossCombatEvents::onMitigated);
        GameEvents.BUS.addListener(DMZEvent.BarrierAbsorbEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.world.worldboss.WorldBossCombatEvents::onBarrierAbsorb);
        installed = true;
    }
}
