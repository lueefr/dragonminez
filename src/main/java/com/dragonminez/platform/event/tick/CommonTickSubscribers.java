package com.dragonminez.platform.event.tick;

import com.dragonminez.platform.event.*;

/** Explicit subscriptions generated from the original annotated gameplay handlers. */
public final class CommonTickSubscribers {
    private static boolean installed;
    private CommonTickSubscribers() {}
    public static synchronized void register() {
        if (installed) return;
        GameEvents.BUS.addListener(LevelTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.common.combat.clash.BeamClashManager::onLevelTick);
        GameEvents.BUS.addListener(LevelTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.common.combat.clash.StrikeClash::onLevelTick);
        GameEvents.BUS.addListener(LevelTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.common.combat.util.KiTerrainDestruction::onLevelTick);
        GameEvents.BUS.addListener(LevelTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.common.combat.util.SwordSlashManager::onLevelTick);
        GameEvents.BUS.addListener(LevelTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.common.events.ForgeCommonEvents::onLevelTick);
        GameEvents.BUS.addListener(ServerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.common.network.StatsDeltaSync::onServerTick);
        GameEvents.BUS.addListener(PlayerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.common.passives.PassiveEventHandler::onPlayerTick);
        GameEvents.BUS.addListener(ServerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.common.training.MinigameSessionManager::onServerTick);
        GameEvents.BUS.addListener(LevelTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.DragonBallsHandler::onLevelTick);
        GameEvents.BUS.addListener(ServerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.DragonBallsHandler::onServerTick);
        GameEvents.BUS.addListener(EntityTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.EntitiesEvents::onStunnedEntityTick);
        GameEvents.BUS.addListener(EntityTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.EntitiesEvents::onEntityTick);
        GameEvents.BUS.addListener(EntityTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.EntitiesEvents::onQuestCombatTick);
        GameEvents.BUS.addListener(LevelTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.GeteSpawnEvents::onLevelTick);
        GameEvents.BUS.addListener(LevelTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.GomahPatrolEvents::onLevelTick);
        GameEvents.BUS.addListener(PlayerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.HellStampedeEvents::onPlayerTick);
        GameEvents.BUS.addListener(PlayerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.PlayerAnimationsSyncHandler::onPlayerTick);
        GameEvents.BUS.addListener(PlayerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.QuestEvents::onPlayerTick);
        GameEvents.BUS.addListener(PlayerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.FalseSuperSaiyanEvents::onPlayerTick);
        GameEvents.BUS.addListener(ServerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.GodRitualService::onServerTick);
        GameEvents.BUS.addListener(PlayerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.GodRitualService::onPlayerTick);
        GameEvents.BUS.addListener(ServerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.KiShareService::onServerTick);
        GameEvents.BUS.addListener(ServerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.MutantEvents::onServerTick);
        GameEvents.BUS.addListener(PlayerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.RageEvents::onPlayerTick);
        GameEvents.BUS.addListener(PlayerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.StatsEvents::onPlayerTick);
        GameEvents.BUS.addListener(PlayerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.StatsEvents::onLivingTick);
        GameEvents.BUS.addListener(EntityTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.StatsEvents::onLivingUpdate);
        GameEvents.BUS.addListener(PlayerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.TPGainEvents::onPlayerTick);
        GameEvents.BUS.addListener(PlayerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.TickHandler::onPlayerTick);
        GameEvents.BUS.addListener(PlayerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.combat.CombatEvent::stopParriedAnimation);
        GameEvents.BUS.addListener(EntityTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.combat.CombatEvent::stopParriedStun);
        GameEvents.BUS.addListener(LevelTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.combat.EvasionAttackHandler::onLevelTick);
        GameEvents.BUS.addListener(ServerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.combat.EvasionAttackHandler::onServerTick);
        GameEvents.BUS.addListener(PlayerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.combat.EvasionAttackHandler::onPlayerTick);
        GameEvents.BUS.addListener(ServerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.combat.KiTechniqueHandler::onServerTick);
        GameEvents.BUS.addListener(PlayerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.combat.MomentumImpactHandler::onPlayerTick);
        GameEvents.BUS.addListener(EntityTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.combat.MomentumImpactHandler::onLivingTick);
        GameEvents.BUS.addListener(PlayerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.combat.StrikeAttackHandler::onPlayerTick);
        GameEvents.BUS.addListener(ServerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.combat.StrikeCinematics::onServerTick);
        GameEvents.BUS.addListener(LevelTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.world.raid.RaidEvents::onLevelTick);
        GameEvents.BUS.addListener(LevelTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.world.raid.RaidSiteManager::onLevelTick);
        GameEvents.BUS.addListener(ServerTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.server.world.structure.helper.QuestStructureHints::onServerTick);
        installed = true;
    }
}
