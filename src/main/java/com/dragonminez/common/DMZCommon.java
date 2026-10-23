package com.dragonminez.common;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.events.ModCommonEvents;
import com.dragonminez.common.init.*;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.racial.RacialRegistry;
import com.dragonminez.common.racial.impl.BioAndroidEvolution;
import com.dragonminez.common.racial.impl.FrostDemonReserve;
import com.dragonminez.common.racial.impl.GlindDivinity;
import com.dragonminez.common.racial.impl.HumanAdaptation;
import com.dragonminez.common.racial.impl.MajinAbsorption;
import com.dragonminez.common.racial.impl.NamekAssimilation;
import com.dragonminez.common.racial.impl.SaiyanZenkai;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.wish.WishManager;
import com.dragonminez.server.world.feature.DemonRealmFeatures;
import com.dragonminez.server.world.feature.OtherworldFeatures;
import com.dragonminez.server.world.feature.OverworldFeatures;
import com.dragonminez.server.world.feature.SacredKaiFeatures;
import com.dragonminez.server.world.structure.placement.MainStructurePlacements;
import com.dragonminez.server.world.structure.processor.MainStructureProcessors;
import com.dragonminez.server.world.structure.helper.MainStructureTypes;

public class DMZCommon {

    public static void init() {
		LogUtil.info(Env.COMMON, "Initializing DragonMineZ Common...");
		com.dragonminez.platform.ServerLifecycleHooks.initialize();
        com.dragonminez.platform.event.server.ServerLifecycleSubscribers.register();
        com.dragonminez.platform.event.server.ServerLifecycleBridge.register();
        com.dragonminez.platform.event.tick.CommonTickSubscribers.register();
        com.dragonminez.platform.event.tick.ServerTickBridge.register();
		registerRacialAbilities();
        com.dragonminez.platform.event.CommonCustomEventSubscribers.register();
        ConfigManager.initialize();
        com.dragonminez.platform.event.GameEvents.BUS.addListener(
                com.dragonminez.platform.resource.AddPackFindersEvent.class, com.dragonminez.platform.event.EventPriority.NORMAL, false,
                ModCommonEvents::onAddPackFinders);
        QuestRegistry.init();
		WishManager.init();
        NetworkHandler.register();
        StatsCapability.register();
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
                (dispatcher, registries, environment) -> com.dragonminez.server.DMZServer.registerCommands(dispatcher));
        com.dragonminez.platform.event.player.PlayerLifecycleSubscribers.register();
        com.dragonminez.platform.event.player.PlayerLifecycleBridge.register();
        com.dragonminez.platform.event.player.InteractionSubscribers.register();
        com.dragonminez.platform.event.player.InteractionBridge.register();
        com.dragonminez.platform.event.living.LivingSubscribers.register();
        com.dragonminez.platform.event.living.LivingLifecycleBridge.register();
        // Fabric initializes GeckoLib through its own loader entrypoint.

		MainAttributes.ATTRIBUTES.register();
		EntityAttributes.ATTRIBUTES.register();
		MainSounds.register();
        MainEffects.register();
		MainFluids.register();
		MainBlocks.register();
		MainBlockEntities.register();
		MainItems.register();
		MainTabs.register();
        MainEntities.register();
        MainEntities.registerSpawnPlacements();
        MainVillagers.register();
        MainParticles.register();
		MainRecipes.register();
		MainMenus.register();
		MainPotions.register();
		MainEnchants.register();
		MainLootModifiers.register();
        MainStructurePlacements.register();
		MainStructureProcessors.register();
		MainStructureTypes.register();
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) ->
                com.dragonminez.platform.event.GameEvents.BUS.post(new com.dragonminez.platform.event.entity.EntityLeaveLevelEvent(entity)));
        ModCommonEvents.registerMachineStorage();
		ModCommonEvents.registerAttributes();
        ModCommonEvents.registerPlayerAttributes();
        ModCommonEvents.commonSetup();
		OtherworldFeatures.register();
		OverworldFeatures.register();
		SacredKaiFeatures.register();
		DemonRealmFeatures.register();

		MainGameRules.register();
		MainDamageTypes.register();

    }

	private static void registerRacialAbilities() {
		RacialRegistry.register(new SaiyanZenkai());
		RacialRegistry.register(new NamekAssimilation());
		RacialRegistry.register(new MajinAbsorption());
		RacialRegistry.register(new HumanAdaptation());
		RacialRegistry.register(new FrostDemonReserve());
		RacialRegistry.register(new BioAndroidEvolution());
		RacialRegistry.register(new GlindDivinity());
	}
}
