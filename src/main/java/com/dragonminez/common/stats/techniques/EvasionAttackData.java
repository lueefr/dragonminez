package com.dragonminez.common.stats.techniques;

import lombok.*;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.TechniqueConfig;
import com.dragonminez.common.stats.StatsData;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;

@Getter
@Setter
public class EvasionAttackData extends TechniqueData {
	public static final float LEVEL_MODIFIER_STEP = 0.025f;
	public static final float MAX_MODIFIER_LIMIT = 0.5f;
	public static final int SLEEP_RECOVERY_MAX_TICKS = 140;

	private float damageMultiplier;
	private int durationTicks;
	private int durationLevel;
	private int cooldownLevel;
	private String animationId;
	private boolean useStrikePower;

	public EvasionAttackData() { super(); }

	@Override
	public TechniqueType getType() { return TechniqueType.EVASION_ATTACK; }

	public float getActualDamageMultiplier() { return damageMultiplier; }

	public int getActualDurationTicks() {
		if ("sleep_recovery".equals(this.id)) return SLEEP_RECOVERY_MAX_TICKS;
		float bonus = Math.min(MAX_MODIFIER_LIMIT, Math.max(0, durationLevel) * LEVEL_MODIFIER_STEP);
		return Math.max(1, Math.round(durationTicks * (1.0f + bonus)));
	}

	public int getActualCooldown() {
		TechniqueConfig.EvasionAttackConfig cfg = ConfigManager.getTechniqueConfig().getEvasionConfig(this.id);
		int base = cfg.getCooldownTicks() > 0 ? cfg.getCooldownTicks() : this.cooldown;
		float reduction = Math.max(1.0f - MAX_MODIFIER_LIMIT, 1.0f - Math.max(0, cooldownLevel) * LEVEL_MODIFIER_STEP);
		return Math.max(1, Math.round(base * reduction));
	}

	public boolean canUpgradeStat(String statName) {
		return ("duration".equals(statName) && !"sleep_recovery".equals(this.id)) || "cooldown".equals(statName);
	}

	public int getUpgradeXpCost(String statName) {
		TechniqueConfig.EvasionAttackConfig cfg = ConfigManager.getTechniqueConfig().getEvasionConfig(this.id);
		int baseMin = Math.max(0, cfg.getMinXPCost());
		double multiplier = Math.max(0.0, cfg.getXpCostMultiplier());
		int totalUpgrades = Math.max(0, durationLevel) + Math.max(0, cooldownLevel);
		int scaledBase = (int) Math.round(baseMin * multiplier);
		int upgradeExtra = (int) Math.round(totalUpgrades * Math.max(0.0, baseMin * (multiplier - 1.0)));
		int computed = Math.max(0, scaledBase + upgradeExtra);
		int max = cfg.getMaxXPCost();
		if (max >= 0) computed = Math.min(computed, max);
		return Math.max(0, computed);
	}

	public int getXpGainPerHit() {
		TechniqueConfig.EvasionAttackConfig cfg = ConfigManager.getTechniqueConfig().getEvasionConfig(this.id);
		double gain = Math.max(0.0, cfg.getXpGainPerHit() * cfg.getXpGainMultiplier());
		return Math.max(0, (int) Math.round(gain));
	}

	public double getActualHitDamage(StatsData statsData) {
		TechniqueConfig.EvasionAttackConfig cfg = ConfigManager.getTechniqueConfig().getEvasionConfig(this.id);
		double statValue = useStrikePower ? statsData.getStats().getStrikePower() : statsData.getStats().getKiPower();
		return 0.10 * statValue * getActualDamageMultiplier() * Math.max(0.0, cfg.getDamageMultiplier());
	}

	@Override
	public double getCalculatedCost(StatsData statsData) {
		TechniqueConfig.EvasionAttackConfig cfg = ConfigManager.getTechniqueConfig().getEvasionConfig(this.id);
		double base = getActualDurationTicks() * 0.5 + getActualHitDamage(statsData) * 2.0;
		return Math.max(5.0, base * Math.max(0.0, cfg.getKiCostMultiplier()));
	}

	@Override
	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putString("Id", this.id);
		tag.putString("Name", this.name);
		tag.putString("Author", this.author);
		tag.putInt("Experience", this.experience);
		tag.putDouble("BaseCost", this.baseCost);
		tag.putFloat("TpCost", this.tpCost);
		tag.putFloat("DamageMultiplier", this.damageMultiplier);
		tag.putInt("DurationTicks", this.durationTicks);
		tag.putInt("DurationLevel", this.durationLevel);
		tag.putInt("CooldownLevel", this.cooldownLevel);
		tag.putString("AnimationId", this.animationId != null ? this.animationId : "");
		tag.putBoolean("UseStrikePower", this.useStrikePower);
		tag.putInt("CastTime", this.castTime);
		tag.putInt("Cooldown", this.cooldown);
		return tag;
	}

	@Override
	public void load(CompoundTag tag) {
		this.id = tag.getStringOr("Id", "");
		this.name = tag.getStringOr("Name", "");
		this.author = tag.getStringOr("Author", "");
		this.experience = tag.getIntOr("Experience", 0);
		this.baseCost = tag.getDoubleOr("BaseCost", 0D);
		this.tpCost = tag.contains("TpCost") ? tag.getFloatOr("TpCost", 0F) : 0;
		this.damageMultiplier = tag.getFloatOr("DamageMultiplier", 0F);
		this.durationTicks = tag.contains("DurationTicks") ? tag.getIntOr("DurationTicks", 0) : 60;
		if (this.durationTicks <= 0) this.durationTicks = 60;
		this.durationLevel = tag.getIntOr("DurationLevel", 0);
		this.cooldownLevel = tag.getIntOr("CooldownLevel", 0);
		this.animationId = tag.getStringOr("AnimationId", "");
		this.useStrikePower = tag.getBooleanOr("UseStrikePower", false);
		this.castTime = tag.getIntOr("CastTime", 0);
		this.cooldown = tag.getIntOr("Cooldown", 0);
	}
}
