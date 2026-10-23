package com.dragonminez.platform.event.player;
import com.dragonminez.platform.event.*;
public final class InteractionSubscribers {
    private static boolean installed;
    private InteractionSubscribers() {}
    public static synchronized void register() {
        if (installed) return;
        GameEvents.BUS.addListener(PlayerInteractEvent.EntityInteract.class, EventPriority.NORMAL, false, com.dragonminez.common.events.ForgeCommonEvents::onMerchantInteract);
        GameEvents.BUS.addListener(AttackEntityEvent.class, EventPriority.NORMAL, false, com.dragonminez.common.events.ForgeCommonEvents::onPlayerAttack);
        GameEvents.BUS.addListener(AttackEntityEvent.class, EventPriority.NORMAL, true, com.dragonminez.common.training.MinigameSessionManager::onAttackEntity);
        GameEvents.BUS.addListener(PlayerInteractEvent.RightClickItem.class, EventPriority.NORMAL, true, com.dragonminez.common.training.MinigameSessionManager::onRightClickItem);
        GameEvents.BUS.addListener(PlayerInteractEvent.LeftClickBlock.class, EventPriority.NORMAL, false, com.dragonminez.server.events.ArenaProtectionHandler::onInteractLeftClickBlock);
        GameEvents.BUS.addListener(PlayerInteractEvent.RightClickBlock.class, EventPriority.NORMAL, false, com.dragonminez.server.events.ArenaProtectionHandler::onInteractRightClickBlock);
        GameEvents.BUS.addListener(PlayerInteractEvent.RightClickItem.class, EventPriority.NORMAL, false, com.dragonminez.server.events.ArenaProtectionHandler::onInteractRightClickItem);
        GameEvents.BUS.addListener(PlayerInteractEvent.EntityInteract.class, EventPriority.NORMAL, false, com.dragonminez.server.events.ArenaProtectionHandler::onInteractEntityInteract);
        GameEvents.BUS.addListener(PlayerInteractEvent.EntityInteractSpecific.class, EventPriority.NORMAL, false, com.dragonminez.server.events.ArenaProtectionHandler::onInteractEntityInteractSpecific);
        GameEvents.BUS.addListener(PlayerInteractEvent.EntityInteract.class, EventPriority.NORMAL, false, com.dragonminez.server.events.EntitiesEvents::onEntityInteract);
        GameEvents.BUS.addListener(PlayerInteractEvent.EntityInteract.class, EventPriority.NORMAL, false, com.dragonminez.server.events.QuestEvents::onEntityInteract);
        GameEvents.BUS.addListener(AttackEntityEvent.class, EventPriority.HIGH, false, com.dragonminez.server.events.players.KiShareService::onAttackEntity);
        GameEvents.BUS.addListener(PlayerInteractEvent.RightClickItem.class, EventPriority.HIGH, false, com.dragonminez.server.events.players.KiShareService::onRightClickItem);
        GameEvents.BUS.addListener(PlayerInteractEvent.RightClickBlock.class, EventPriority.HIGH, false, com.dragonminez.server.events.players.KiShareService::onRightClickBlock);
        GameEvents.BUS.addListener(PlayerInteractEvent.EntityInteract.class, EventPriority.HIGH, false, com.dragonminez.server.events.players.KiShareService::onEntityInteract);
        GameEvents.BUS.addListener(PlayerInteractEvent.EntityInteractSpecific.class, EventPriority.HIGH, false, com.dragonminez.server.events.players.KiShareService::onEntityInteractSpecific);
        GameEvents.BUS.addListener(PlayerInteractEvent.LeftClickBlock.class, EventPriority.HIGH, false, com.dragonminez.server.events.players.KiShareService::onLeftClickBlock);
        GameEvents.BUS.addListener(PlayerInteractEvent.RightClickItem.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.StatsEvents::onItemRightClick);
        GameEvents.BUS.addListener(AttackEntityEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.StatsEvents::onPlayerAttack);
        GameEvents.BUS.addListener(PlayerInteractEvent.RightClickBlock.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.StatsEvents::onRightClickBlock);
        GameEvents.BUS.addListener(PlayerInteractEvent.RightClickItem.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.StatsEvents::onRightClickItem);
        GameEvents.BUS.addListener(PlayerInteractEvent.EntityInteract.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.StatsEvents::onEntityInteract);
        GameEvents.BUS.addListener(PlayerInteractEvent.EntityInteractSpecific.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.StatsEvents::onEntityInteractSpecific);
        GameEvents.BUS.addListener(PlayerInteractEvent.LeftClickBlock.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.StatsEvents::onLeftClickBlock);
        GameEvents.BUS.addListener(AttackEntityEvent.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.combat.EvasionAttackHandler::onAttackEntity);
        GameEvents.BUS.addListener(PlayerInteractEvent.RightClickBlock.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.combat.EvasionAttackHandler::onRightClickBlock);
        GameEvents.BUS.addListener(PlayerInteractEvent.RightClickItem.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.combat.EvasionAttackHandler::onRightClickItem);
        GameEvents.BUS.addListener(PlayerInteractEvent.LeftClickBlock.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.combat.EvasionAttackHandler::onLeftClickBlock);
        GameEvents.BUS.addListener(PlayerInteractEvent.EntityInteract.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.combat.EvasionAttackHandler::onEntityInteract);
        GameEvents.BUS.addListener(PlayerInteractEvent.EntityInteractSpecific.class, EventPriority.NORMAL, false, com.dragonminez.server.events.players.combat.EvasionAttackHandler::onEntityInteractSpecific);
        installed = true;
    }
}
