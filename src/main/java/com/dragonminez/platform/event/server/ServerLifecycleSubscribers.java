package com.dragonminez.platform.event.server;

import com.dragonminez.platform.event.*;
import com.dragonminez.platform.event.level.*;

/** Explicit server and world lifecycle subscriptions retaining original priorities. */
public final class ServerLifecycleSubscribers {
    private static boolean installed;
    private ServerLifecycleSubscribers() {}
    public static synchronized void register() {
        if (installed) return;
        GameEvents.BUS.addListener(LevelEvent.Unload.class, EventPriority.NORMAL, false, com.dragonminez.common.combat.clash.StrikeClash::onLevelUnload);
        GameEvents.BUS.addListener(LevelEvent.Unload.class, EventPriority.NORMAL, false, com.dragonminez.common.combat.util.KiTerrainDestruction::onLevelUnload);
        GameEvents.BUS.addListener(ServerStoppedEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.combat.util.KiTerrainDestruction::onServerStopped);
        GameEvents.BUS.addListener(com.dragonminez.platform.event.server.ServerAboutToStartEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.events.ForgeCommonEvents::onServerAboutToStart);
        GameEvents.BUS.addListener(ServerStartingEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.events.ForgeCommonEvents::onServerStarting);
        GameEvents.BUS.addListener(ServerStartedEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.events.ForgeCommonEvents::onServerStarted);
        GameEvents.BUS.addListener(LevelEvent.Unload.class, EventPriority.NORMAL, false, com.dragonminez.common.events.ForgeCommonEvents::onLevelUnload);
        GameEvents.BUS.addListener(ChunkEvent.Load.class, EventPriority.NORMAL, false, com.dragonminez.common.events.ForgeCommonEvents::onChunkLoad);
        GameEvents.BUS.addListener(LevelEvent.Load.class, EventPriority.NORMAL, false, com.dragonminez.common.events.ForgeCommonEvents::onLevelLoad);
        GameEvents.BUS.addListener(ServerStoppingEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.events.ForgeCommonEvents::onServerStopping);
        GameEvents.BUS.addListener(ServerStartedEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.quest.PartyManager::onServerStarted);
        GameEvents.BUS.addListener(ServerStoppedEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.quest.PartyManager::onServerStopped);
        GameEvents.BUS.addListener(ServerStoppingEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.training.MinigameSessionManager::onServerStopping);
        GameEvents.BUS.addListener(ChunkEvent.Load.class, EventPriority.NORMAL, false, com.dragonminez.server.events.DragonBallsHandler::onChunkLoad);
        GameEvents.BUS.addListener(ServerStoppedEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.GodRitualService::onServerStopped);
        GameEvents.BUS.addListener(ServerStoppedEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.KiShareService::onServerStopped);
        GameEvents.BUS.addListener(ServerStoppedEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.combat.KiTechniqueHandler::onServerStopped);
        GameEvents.BUS.addListener(ServerStartingEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.world.structure.helper.QuestStructureHints::onServerStarting);
        GameEvents.BUS.addListener(ServerStoppedEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.world.structure.helper.QuestStructureHints::onServerStopped);
        installed = true;
    }
}
