package com.dragonminez.common.stats.character;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

public final class EntityStatDebuffs {

	private EntityStatDebuffs() {}

	public static final Set<String> SUPPORTED = Set.of(
			SecondaryStatEffects.STR,
			SecondaryStatEffects.PWR,
			SecondaryStatEffects.DEF,
			SecondaryStatEffects.HP_REGEN
	);

	public static boolean isSupported(String stat) {
		return stat != null && SUPPORTED.contains(stat.toUpperCase());
	}

	private static String factorKey(String stat) { return "dmz_debuff_" + stat + "_factor"; }
	private static String untilKey(String stat) { return "dmz_debuff_" + stat + "_until"; }

	public static void applyDebuff(LivingEntity entity, String stat, double factor, int durationTicks) {
		if (entity == null || entity.level().isClientSide()) return;
		if (stat == null || factor >= 0.0 || durationTicks <= 0) return;
		String key = stat.toUpperCase();
		if (!SUPPORTED.contains(key)) return;

		CompoundTag data = com.dragonminez.platform.entity.EntityData.get(entity);
		long now = entity.level().getGameTime();
		boolean active = data.getLongOr(untilKey(key), 0L) > now;

		if (!active || Math.abs(factor) >= Math.abs(data.getDoubleOr(factorKey(key), 0D))) data.putDouble(factorKey(key), factor);
		long newUntil = now + durationTicks;
		data.putLong(untilKey(key), active ? Math.max(data.getLongOr(untilKey(key), 0L), newUntil) : newUntil);
	}

	public static double getMultiplier(LivingEntity entity, String stat) {
		if (entity == null || stat == null) return 1.0;
		String key = stat.toUpperCase();
		CompoundTag data = com.dragonminez.platform.entity.EntityData.get(entity);
		if (data.getLongOr(untilKey(key), 0L) <= entity.level().getGameTime()) return 1.0;
		return Math.max(0.0, 1.0 + data.getDoubleOr(factorKey(key), 0D));
	}

	public static boolean hasDebuff(LivingEntity entity, String stat) {
		if (entity == null || stat == null) return false;
		return com.dragonminez.platform.entity.EntityData.get(entity).getLongOr(untilKey(stat.toUpperCase()), 0L) > entity.level().getGameTime();
	}
}
