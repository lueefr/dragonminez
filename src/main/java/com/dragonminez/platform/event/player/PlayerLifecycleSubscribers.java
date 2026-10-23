package com.dragonminez.platform.event.player;

import com.dragonminez.platform.event.*;

/** Explicit player lifecycle subscriptions retaining original priorities. */
public final class PlayerLifecycleSubscribers {
    private static boolean installed;
    private PlayerLifecycleSubscribers() {}
    public static synchronized void register() {
        if (installed) return;
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.events.ForgeCommonEvents::onPlayerLogin);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.events.ForgeCommonEvents::onPlayerLogout);
        GameEvents.BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.events.ForgeCommonEvents::onPlayerRespawn);
        GameEvents.BUS.addListener(PlayerEvent.PlayerChangeGameModeEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.events.ForgeCommonEvents::onGamemodeChange);
        GameEvents.BUS.addListener(PlayerEvent.NameFormat.class, EventPriority.NORMAL, false, com.dragonminez.common.events.ForgeCommonEvents::onPlayerNameFormat);
        GameEvents.BUS.addListener(PlayerEvent.TabListNameFormat.class, EventPriority.NORMAL, false, com.dragonminez.common.events.ForgeCommonEvents::onPlayerTabListNameFormat);
        GameEvents.BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.events.ForgeCommonEvents::onPlayerChangeDimension);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.passives.PassiveEventHandler::onLogout);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.quest.PartyManager::onPlayerLogout);
        GameEvents.BUS.addListener(PlayerEvent.ItemCraftedEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.training.MinigameSessionManager::onItemCrafted);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.training.MinigameSessionManager::onPlayerLogout);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.DataReportEvents::onPlayerLogin);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, EventPriority.HIGHEST, false, com.dragonminez.server.events.DataSyncHandler::onPlayerLogin);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.DataSyncHandler::onPlayerLogout);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.DragonBallsHandler::onPlayerLogin);
        GameEvents.BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.DragonBallsHandler::onPlayerRespawn);
        GameEvents.BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.DragonBallsHandler::onPlayerChangedDimension);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.EntitiesEvents::onPlayerLoggedOut);
        GameEvents.BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.EntitiesEvents::onPlayerChangedDimension);
        GameEvents.BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.EntitiesEvents::onPlayerDeath);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.HellStampedeEvents::onPlayerLogout);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, EventPriority.LOWEST, false, com.dragonminez.server.events.QuestEvents::onPlayerLogin);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.QuestEvents::onPlayerLogout);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.FalseSuperSaiyanEvents::onLogout);
        GameEvents.BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.GodRitualService::onDimensionChange);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.GodRitualService::onLogout);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.GodRitualService::onLogin);
        GameEvents.BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.KiShareService::onDimensionChange);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.KiShareService::onLogin);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.KiShareService::onLogout);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.RageEvents::onLogout);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.StatsEvents::onPlayerLoggedOut);
        GameEvents.BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.StatsEvents::onPlayerRespawn);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.TPGainEvents::onPlayerLoggedOut);
        GameEvents.BUS.addListener(PlayerEvent.ItemCraftedEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.TPGainEvents::onItemCrafted);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.TickHandler::onPlayerLogin);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.TickHandler::onPlayerLogout);
        GameEvents.BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.TickHandler::onPlayerRespawn);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.combat.CombatEvent::clearParriedAnimation);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.combat.EvasionAttackHandler::onPlayerLoggedOut);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.combat.MomentumImpactHandler::onPlayerLoggedOut);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.combat.SpeedDodgeHandler::onPlayerLogout);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.combat.StrikeAttackHandler::onPlayerLoggedOut);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.combat.StrikeAttackHandler::onPlayerLoggedIn);
        GameEvents.BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.combat.StrikeCinematics::onPlayerLoggedOut);
        installed = true;
    }
}
