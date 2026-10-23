package com.dragonminez.common.stats;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.StatsDeltaSync;
import com.dragonminez.common.racial.RacialContext;
import com.dragonminez.common.racial.RacialRegistry;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.server.events.players.TickHandler;
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
import com.dragonminez.common.network.S2C.SkinPixelsSyncS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.SyncQuestRegistryS2C;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.server.world.structure.helper.QuestStructureHints;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import com.dragonminez.compat.capabilities.Capability;
import com.dragonminez.compat.capabilities.CapabilityManager;
import com.dragonminez.compat.capabilities.CapabilityToken;

import java.util.Map;
import java.util.UUID;

public class StatsCapability {
	public static final Capability<StatsData> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {
	});
    public static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<StatsProvider> PLAYER_STATS =
            net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.<StatsProvider>create(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(Reference.MOD_ID, "player_stats"),
                builder -> builder.initializer(StatsProvider::new).persistent(StatsProvider.CODEC).copyOnDeath());
    private static boolean registered;

	private static StatsData CLIENT_CACHE;

	public static void clearClientCache() {
		CLIENT_CACHE = null;
	}

    public static synchronized void register() {
        if (registered) return;
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> onPlayerClone(newPlayer, oldPlayer));
        com.dragonminez.platform.event.GameEvents.BUS.addListener(
                com.dragonminez.platform.event.player.PlayerEvent.PlayerRespawnEvent.class,
                com.dragonminez.platform.event.EventPriority.LOWEST, false, event -> onPlayerRespawn(event.getEntity()));
        com.dragonminez.platform.event.GameEvents.BUS.addListener(
                com.dragonminez.platform.event.player.PlayerEvent.PlayerLoggedInEvent.class,
                com.dragonminez.platform.event.EventPriority.HIGHEST, false, event -> sendLoginConfiguration(event.getEntity()));
        com.dragonminez.platform.event.GameEvents.BUS.addListener(
                com.dragonminez.platform.event.player.PlayerEvent.PlayerLoggedInEvent.class,
                com.dragonminez.platform.event.EventPriority.LOWEST, false, event -> onPlayerLogin(event.getEntity()));
        com.dragonminez.platform.event.GameEvents.BUS.addListener(
                com.dragonminez.platform.event.player.PlayerEvent.PlayerChangedDimensionEvent.class,
                com.dragonminez.platform.event.EventPriority.LOWEST, false, event -> onPlayerChangedDimension(event.getEntity()));
        net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents.START_TRACKING.register((target, viewer) -> onStartTracking(viewer, target));
        registered = true;
    }

	public static void onPlayerClone(Player player, Player original) {

		TickHandler.registerForceKillGrace(player.getUUID());
		StatsProvider.get(INSTANCE, player).ifPresent(newData -> {
			StatsProvider.get(INSTANCE, original).ifPresent(oldData -> {
				newData.copyFrom(oldData);

				if (player.level().isClientSide()) {
					if (oldData.getStatus().isHasCreatedCharacter()) CLIENT_CACHE = oldData;
					else if (CLIENT_CACHE != null) newData.copyFrom(CLIENT_CACHE);
				}
			});
		});
		// Drop original provider mapping after clone copy.
		StatsProvider.remove(original);
	}

    private static void sendLoginConfiguration(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ConfigManager.sendConfigSync(serverPlayer);
            SyncQuestRegistryS2C.sendTo(serverPlayer);
        }
    }

	public static void onPlayerLogin(Player player) {
		if (player instanceof ServerPlayer serverPlayer) {

			MinecraftServer server = serverPlayer.level().getServer();
			if (server != null && !QuestStructureHints.isResolved()) {
				UUID playerId = serverPlayer.getUUID();
				QuestStructureHints.ensureResolvedAsync(server).thenRun(() -> server.execute(() -> {
					ServerPlayer online = server.getPlayerList().getPlayer(playerId);
					if (online != null) SyncQuestRegistryS2C.sendTo(online);
				}));
			}

			StatsProvider.get(INSTANCE, serverPlayer).ifPresent(data -> {
				markCurrentDimensionVisited(serverPlayer, data);
				PlayerQuestData questData = data.getPlayerQuestData();
				if (questData.isSagaLocked("classic_saga")) questData.setSagaUnlocked("classic_saga", true);
				TransformationsHelper.ensureSelectedFormDefault(data);
				TransformationsHelper.ensureSelectedStackFormDefault(data);

				data.getStatus().setStrikeLocked(false);
				data.getStatus().setStunEffect(false);
				data.getStatus().setKnockedDown(false);
				data.getStatus().setMatchFrozen(false);
				data.getCooldowns().removeCooldown(Cooldowns.KNOCKDOWN_DURATION);
				com.dragonminez.server.world.worldboss.WorldBossManager.onPlayerLogin(serverPlayer, data);

				Map<String, String> repairedSkills = data.getSkills().repairSkillNames();
				if (!repairedSkills.isEmpty()) {
					repairedSkills.forEach((oldName, newName) -> LogUtil.info(Env.SERVER, "Repaired skill for {}: '{}' -> '{}'", serverPlayer.getGameProfile().name(), oldName, newName));
				}
				data.getSkills().setSkillActive("kisense", false);
				RacialRegistry.forPlayer(data).ifPresent(ability -> ability.onLogin(new RacialContext(serverPlayer, data)));
				StatsDeltaSync.reset(serverPlayer);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(serverPlayer), serverPlayer);
				syncSkinPixels(serverPlayer, data);
			});
		}
		player.refreshDimensions();
	}

	public static void onPlayerRespawn(Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			StatsProvider.get(INSTANCE, serverPlayer).ifPresent(data -> {
				data.getResources().setCurrentEnergy(data.getMaxEnergy());
				data.getResources().setCurrentStamina(data.getMaxStamina());
				data.getStatus().setStrikeLocked(false);
				data.getStatus().setStunEffect(false);
				data.getStatus().setKnockedDown(false);
				data.getStatus().setMatchFrozen(false);
				data.getCooldowns().removeCooldown(Cooldowns.KNOCKDOWN_DURATION);
				RacialRegistry.forPlayer(data).ifPresent(ability -> ability.onRespawn(new RacialContext(serverPlayer, data)));
				StatsDeltaSync.reset(serverPlayer);
				NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(serverPlayer), serverPlayer);
			});
		}
	}

	public static void onPlayerChangedDimension(Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			StatsProvider.get(INSTANCE, serverPlayer).ifPresent(data -> {
				markCurrentDimensionVisited(serverPlayer, data);
				data.getSkills().setSkillActive("kisense", false);

				data.getStatus().setStrikeLocked(false);
				data.getStatus().setStunEffect(false);
				data.getStatus().setKnockedDown(false);
				data.getStatus().setMatchFrozen(false);
				data.getCooldowns().removeCooldown(Cooldowns.KNOCKDOWN_DURATION);
				com.dragonminez.server.world.worldboss.WorldBossManager.onPlayerChangedDimension(serverPlayer, data);
				RacialRegistry.forPlayer(data).ifPresent(ability -> ability.onDimensionChange(new RacialContext(serverPlayer, data)));
				StatsDeltaSync.reset(serverPlayer);

				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(serverPlayer), serverPlayer);
				syncSkinPixels(serverPlayer, data);
			});
		}
	}

	public static void onStartTracking(Player viewerPlayer, Entity targetEntity) {
		if (!(targetEntity instanceof ServerPlayer target) || !(viewerPlayer instanceof ServerPlayer viewer)) return;
		StatsSyncS2C snapshot = StatsDeltaSync.trackerSnapshot(target);
		if (snapshot != null) NetworkHandler.sendToPlayer(snapshot, viewer);
		StatsProvider.get(INSTANCE, target).ifPresent(data -> {
			if (!data.getCharacter().getSkinPixels().isEmpty()) NetworkHandler.sendToPlayer(new SkinPixelsSyncS2C(target), viewer);
		});
	}

	public static void syncSkinPixels(ServerPlayer player, StatsData data) {
		if (!data.getCharacter().getSkinPixels().isEmpty()) NetworkHandler.sendToTrackingEntityAndSelf(new SkinPixelsSyncS2C(player), player);
	}

	private static void markCurrentDimensionVisited(ServerPlayer player, StatsData data) {
		data.getStatus().markVisitedDimension(player.level().dimension().identifier().toString());
	}
}
