package com.dragonminez.server.world.raid;

import com.dragonminez.platform.event.EventBusSubscriber;
import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.config.RaidDefinition;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.RaidMusicS2C;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import com.dragonminez.platform.event.entity.EntityJoinLevelEvent;
import com.dragonminez.platform.event.living.LivingDeathEvent;
import com.dragonminez.platform.event.living.MobEffectEvent;
import com.dragonminez.platform.event.SubscribeEvent;

import java.util.UUID;

@EventBusSubscriber(modid = Reference.MOD_ID)
public class RaidTriggerEvents {

	public static final String PENDING_RAID_TAG = DBSagasEntity.RAID_SUPERVILLAIN_SOURCE_TAG;

	private static final double DEFAULT_SPAWN_CHANCE = 0.05D;

	@SubscribeEvent
	public static void onNaturalSpawn(net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent event) {
		if (event.getSpawnType() != EntitySpawnReason.NATURAL) return;
		if (!(event.getEntity() instanceof DBSagasEntity saga)) return;
		ServerLevel level = event.getLevel().getLevel();
		// Already marked (a re-fired finalize, or a mob restored from disk).
		if (com.dragonminez.platform.entity.EntityData.get(saga).contains(PENDING_RAID_TAG)) return;

		for (RaidType type : RaidTypes.triggerable()) {
			RaidDefinition.Trigger trigger = type.getTrigger();
			if (!matchesDimension(level, trigger)) continue;
			if (!RaidEntityResolver.matches(saga.getType(), trigger.getEntityId())) continue;
			if (level.getRandom().nextDouble() >= trigger.spawnChanceOr(DEFAULT_SPAWN_CHANCE)) continue;

			com.dragonminez.platform.entity.EntityData.get(saga).putString(PENDING_RAID_TAG, type.getId());
			saga.setSupervillain(trigger.isSupervillain());
			return;
		}
	}

	@SubscribeEvent
	public static void onTransformedMobJoin(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide()) return;
		if (!(event.getEntity() instanceof DBSagasEntity saga)) return;

		CompoundTag data = com.dragonminez.platform.entity.EntityData.get(saga);
		if (!data.contains(DBSagasEntity.RAID_REPLACES_TAG)) return;

		UUID oldId = com.dragonminez.platform.nbt.NbtCompat.getUUID(data, DBSagasEntity.RAID_REPLACES_TAG);
		data.remove(DBSagasEntity.RAID_REPLACES_TAG);

		String rawRaidId = data.getStringOr(DBSagasEntity.RAID_ID_TAG, "");
		if (rawRaidId.isBlank()) return;

		Raid raid = RaidSavedData.get(event.getLevel().getServer()).getRaid(UUID.fromString(rawRaidId));
		if (raid == null || raid.isFinished()) return;

		if (raid.replaceMob(oldId, saga)) {
			LogUtil.info(Env.SERVER, "Raid {} tracked a transform: {} -> {}", rawRaidId, oldId, saga.getUUID());
		}
	}

	private static boolean matchesDimension(ServerLevel level, RaidDefinition.Trigger trigger) {
		String dimension = trigger.getDimension();
		if (dimension == null || dimension.isBlank()) return true;

		Identifier location = Identifier.tryParse(dimension);
		if (location == null) return false;
		return level.dimension().equals(ResourceKey.create(Registries.DIMENSION, location));
	}

	@SubscribeEvent
	public static void onMarkedMobKilled(LivingDeathEvent event) {
		LivingEntity dead = event.getEntity();
		if (dead.level().isClientSide()) return;
		if (!com.dragonminez.platform.entity.EntityData.get(dead).contains(PENDING_RAID_TAG)) return;

		String raidId = com.dragonminez.platform.entity.EntityData.get(dead).getStringOr(PENDING_RAID_TAG, "");
		RaidType type = RaidTypes.get(raidId);
		if (type == null) return;

		ServerPlayer killer = resolveCredit(dead, event, type);
		if (killer == null) return;

		// Already counting down for something; don't stack omens.
		if (killer.hasEffect(MainEffects.RAID_WRATH)) return;

		com.dragonminez.platform.entity.EntityData.get(killer).putString(PENDING_RAID_TAG, raidId);
		killer.addEffect(new MobEffectInstance(MainEffects.RAID_WRATH,
				type.preparationTicks(), 0, false, true, true));
		NetworkHandler.sendToPlayer(new RaidMusicS2C(type.preparationMusic()), killer);

		RaidFeedback.announce(killer, type.getTrigger().getOmenMessage());

		LogUtil.info(Env.SERVER, "{} killed a raid trigger for '{}'; omen running for {} ticks",
				killer.getGameProfile().name(), raidId, type.preparationTicks());
	}

	private static ServerPlayer resolveCredit(LivingEntity dead, LivingDeathEvent event, RaidType type) {
		if (event.getSource().getEntity() instanceof ServerPlayer direct) return direct;
		if (dead.getKillCredit() instanceof ServerPlayer credited) return credited;

		if (!(dead.level() instanceof ServerLevel level)) return null;
		Player nearest = level.getNearestPlayer(dead, type.getActivationRadius());
		return nearest instanceof ServerPlayer server && !server.isSpectator() ? server : null;
	}

	@SubscribeEvent
	public static void onOmenExpired(MobEffectEvent.Expired event) {
		if (event.getEffectInstance() == null) return;
		if (event.getEffectInstance().getEffect() != MainEffects.RAID_WRATH) return;
		if (!(event.getEntity() instanceof ServerPlayer player)) return;

		String raidId = com.dragonminez.platform.entity.EntityData.get(player).getStringOr(PENDING_RAID_TAG, "");
		com.dragonminez.platform.entity.EntityData.get(player).remove(PENDING_RAID_TAG);
		if (raidId.isBlank()) return;

		RaidType type = RaidTypes.get(raidId);
		if (type == null) return;
		if (!(player.level() instanceof ServerLevel level)) return;

		Raid raid = RaidManager.startRaid(level, player.blockPosition(), player, raidId);
		if (raid == null) {
			LogUtil.warn(Env.SERVER, "Omen for '{}' expired but the raid could not start (one is already nearby?)", raidId);
			return;
		}

		String message = type.hasTrigger() ? type.getTrigger().getStartMessage() : null;
		for (UUID id : raid.getParticipants()) {
			ServerPlayer participant = level.getServer().getPlayerList().getPlayer(id);
			if (participant != null) RaidFeedback.announce(participant, message);
		}
	}

	@SubscribeEvent
	public static void onOmenRemoved(MobEffectEvent.Remove event) {
		if (event.getEffect() != MainEffects.RAID_WRATH) return;
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		com.dragonminez.platform.entity.EntityData.get(player).remove(PENDING_RAID_TAG);
		NetworkHandler.sendToPlayer(new RaidMusicS2C(""), player);
	}
}
