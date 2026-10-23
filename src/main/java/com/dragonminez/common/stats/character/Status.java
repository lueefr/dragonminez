package com.dragonminez.common.stats.character;

import lombok.*;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.extras.ActionMode;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class Status {
	public static final int FLIGHT_SEARCH = 0;
	public static final int FLIGHT_COMBAT = 1;
	public static final int GOD_RITUAL_NONE = 0;
	public static final int GOD_RITUAL_DONOR = 1;
	public static final int GOD_RITUAL_RECIPIENT = 2;

	private boolean isAlive;
	private boolean forceHalo;
	private int tempReturnTimer;
	private int tempReturnsUsed;
	private long tempReturnReadyAt;
	private int deathCount;
	private boolean isHasCreatedCharacter;
	private boolean isAuraActive;
	private boolean isActionCharging;
	private boolean isTailVisible;
	private boolean isDescending;
	private boolean isInKaioPlanet;
	private boolean isChargingKi;
	private boolean kiBurstArmed;
	private boolean surgeActive;
	private boolean rageActive;
	private boolean isBlocking;
	private long lastBlockTime;
	private long lastHurtTime;
	private boolean friendlyFistEnabled;
	private boolean stunEffect;
	private boolean isKnockedDown;
	private ActionMode selectedAction;
	private String kiWeaponType;
	private int drainingTargetId;
	private boolean isFused;
	private boolean isFusionLeader;
	private UUID fusionPartnerUUID;
	private int fusionTimer;
	private String fusionType;
	private String fusionName;
	private boolean fusionPartyManaged;
	private UUID fusionPrevPartyId;
	private boolean fusionPrevPartyLeader;
	private int potaraPoseTimer;
	private int evasionLockTicks;
	private UUID potaraPartnerUUID;
	private boolean potaraLeader;
	private boolean potaraBeetle;
	private CompoundTag originalAppearance;
	private boolean androidUpgraded;
	private boolean renderKatana;
	private String backWeapon;
	private String scouterItem;
	private String pothalaColor;
	private boolean isPermanentAura;
	private boolean forcedAura;
	private boolean forcedCharge;
	private boolean isStrikeLocked;
	private boolean matchFrozen;
	private boolean sleepRecovering;
	private int flightMode;
	private boolean flightModeLocked;
	private int lockedFlightMode;
	private final Set<String> visitedDimensions;

	private UUID activeShadowDummyUUID;
	private int shadowDummyPercent;
	private int shadowDummyKillCount;
	private int kiTransferTarget;
	private int kiTransferDonors;
	private int kiDrainStacks;
	private int kiDrainTicks;
	private int godRitualRole;
	private UUID godRitualAnchor;
	private int godRitualTicks;
	private long godRitualGlowStart;

	public Status() {
		this.isAlive = true;
		this.forceHalo = false;
		this.tempReturnTimer = 0;
		this.tempReturnsUsed = 0;
		this.tempReturnReadyAt = 0L;
		this.deathCount = 0;
		this.isHasCreatedCharacter = false;
		this.isAuraActive = false;
		this.isActionCharging = false;
		this.isTailVisible = true;
		this.isDescending = false;
		this.isInKaioPlanet = false;
		this.isChargingKi = false;
		this.kiBurstArmed = false;
		this.surgeActive = false;
		this.rageActive = false;
		this.isBlocking = false;
		this.lastBlockTime = 0;
		this.lastHurtTime = 0;
		this.friendlyFistEnabled = false;
		this.stunEffect = false;
		this.isKnockedDown = false;
		this.selectedAction = ActionMode.FORM;
		this.kiWeaponType = "blade";
		this.drainingTargetId = -1;
		this.isFused = false;
		this.isFusionLeader = false;
		this.fusionPartnerUUID = null;
		this.fusionTimer = 0;
		this.fusionType = "";
		this.fusionName = "";
		this.fusionPartyManaged = false;
		this.fusionPrevPartyId = null;
		this.fusionPrevPartyLeader = false;
		this.potaraPoseTimer = 0;
		this.evasionLockTicks = 0;
		this.potaraPartnerUUID = null;
		this.potaraLeader = false;
		this.potaraBeetle = false;
		this.originalAppearance = new CompoundTag();
		this.androidUpgraded = false;
		this.renderKatana = false;
		this.backWeapon = "";
		this.scouterItem = "";
		this.pothalaColor = "";
		this.isPermanentAura = false;
		this.forcedAura = false;
		this.forcedCharge = false;
		this.isStrikeLocked = false;
		this.matchFrozen = false;
		this.sleepRecovering = false;
		this.flightMode = FLIGHT_SEARCH;
		this.flightModeLocked = false;
		this.lockedFlightMode = FLIGHT_SEARCH;
		this.visitedDimensions = new LinkedHashSet<>();
		this.activeShadowDummyUUID = null;
		this.shadowDummyPercent = 0;
		this.shadowDummyKillCount = 0;
		this.kiTransferTarget = -1;
		this.kiTransferDonors = 0;
		this.kiDrainStacks = 0;
		this.kiDrainTicks = 0;
		this.godRitualRole = GOD_RITUAL_NONE;
		this.godRitualAnchor = null;
		this.godRitualTicks = 0;
		this.godRitualGlowStart = 0L;
	}

	public void reset() {
		this.isAlive = true;
		this.forceHalo = false;
		this.tempReturnTimer = 0;
		this.tempReturnsUsed = 0;
		this.tempReturnReadyAt = 0L;
		this.deathCount = 0;
		this.isHasCreatedCharacter = false;
		this.isAuraActive = false;
		this.isActionCharging = false;
		this.isTailVisible = true;
		this.isDescending = false;
		this.isInKaioPlanet = false;
		this.isChargingKi = false;
		this.kiBurstArmed = false;
		this.surgeActive = false;
		this.rageActive = false;
		this.isBlocking = false;
		this.lastBlockTime = 0;
		this.lastHurtTime = 0;
		this.friendlyFistEnabled = false;
		this.stunEffect = false;
		this.isKnockedDown = false;
		this.selectedAction = ActionMode.FORM;
		this.kiWeaponType = "blade";
		this.drainingTargetId = -1;
		this.isFused = false;
		this.isFusionLeader = false;
		this.fusionPartnerUUID = null;
		this.fusionTimer = 0;
		this.fusionType = "";
		this.fusionName = "";
		this.fusionPartyManaged = false;
		this.fusionPrevPartyId = null;
		this.fusionPrevPartyLeader = false;
		this.potaraPoseTimer = 0;
		this.evasionLockTicks = 0;
		this.potaraPartnerUUID = null;
		this.potaraLeader = false;
		this.potaraBeetle = false;
		this.originalAppearance = new CompoundTag();
		this.androidUpgraded = false;
		this.renderKatana = false;
		this.backWeapon = "";
		this.scouterItem = "";
		this.pothalaColor = "";
		this.isPermanentAura = false;
		this.forcedAura = false;
		this.forcedCharge = false;
		this.isStrikeLocked = false;
		this.matchFrozen = false;
		this.sleepRecovering = false;
		this.flightMode = FLIGHT_SEARCH;
		this.flightModeLocked = false;
		this.lockedFlightMode = FLIGHT_SEARCH;
		this.visitedDimensions.clear();
		this.activeShadowDummyUUID = null;
		this.shadowDummyPercent = 0;
		this.shadowDummyKillCount = 0;
		this.kiTransferTarget = -1;
		this.kiTransferDonors = 0;
		this.kiDrainStacks = 0;
		this.kiDrainTicks = 0;
		this.godRitualRole = GOD_RITUAL_NONE;
		this.godRitualAnchor = null;
		this.godRitualTicks = 0;
		this.godRitualGlowStart = 0L;
	}

	public boolean isStunned() {
		return stunEffect || isKnockedDown || isStrikeLocked || matchFrozen || sleepRecovering || isInGodRitual();
	}

	public boolean isSharingKi() {
		return kiTransferTarget >= 0;
	}

	public boolean isInGodRitual() {
		return godRitualRole != GOD_RITUAL_NONE;
	}

	public boolean isKiShareLocked() {
		return isSharingKi() || isInGodRitual();
	}

	public void validateKiWeaponType() {
		var types = ConfigManager.getCombatConfig().getKiWeaponTypes();
		if (types.isEmpty()) return;
		if (kiWeaponType == null || !types.contains(kiWeaponType.toLowerCase())) kiWeaponType = types.get(0);
	}

	public boolean hasActiveShadowDummy() {
		return activeShadowDummyUUID != null;
	}

	public void markVisitedDimension(String dimensionId) {
		if (dimensionId == null || dimensionId.isBlank() || Identifier.tryParse(dimensionId) == null) return;
		this.visitedDimensions.add(dimensionId);
	}

	public boolean hasVisitedDimension(String dimensionId) {
		return dimensionId != null && this.visitedDimensions.contains(dimensionId);
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("IsAlive", isAlive);
		tag.putBoolean("ForceHalo", forceHalo);
		tag.putInt("TempReturnTimer", tempReturnTimer);
		tag.putInt("TempReturnsUsed", tempReturnsUsed);
		tag.putLong("TempReturnReadyAt", tempReturnReadyAt);
		tag.putInt("DeathCount", deathCount);
		tag.putBoolean("HasCreatedChar", isHasCreatedCharacter);
		tag.putBoolean("AuraActive", isAuraActive);
		tag.putBoolean("Transforming", isActionCharging);
		tag.putBoolean("TailVisible", isTailVisible);
		tag.putBoolean("Descending", isDescending);
		tag.putBoolean("InKaioPlanet", isInKaioPlanet);
		tag.putBoolean("IsChargingKi", isChargingKi);
		tag.putBoolean("KiBurstArmed", kiBurstArmed);
		tag.putBoolean("SurgeActive", surgeActive);
		tag.putBoolean("RageActive", rageActive);
		tag.putBoolean("IsBlocking", isBlocking);
		tag.putLong("LastBlockTime", lastBlockTime);
		tag.putLong("LastHurtTime", lastHurtTime);
		tag.putBoolean("FriendlyFistEnabled", friendlyFistEnabled);
		tag.putBoolean("IsStunned", stunEffect);
		tag.putBoolean("IsKnockedDown", isKnockedDown);
		tag.putInt("SelectedAction", selectedAction.ordinal());
		tag.putString("KiWeaponType", kiWeaponType);
		tag.putInt("DrainingTargetId", drainingTargetId);
		tag.putBoolean("IsFused", isFused);
		tag.putBoolean("IsFusionLeader", isFusionLeader);
		if (fusionPartnerUUID != null) com.dragonminez.platform.nbt.NbtCompat.putUUID(tag, "FusionPartnerUUID", fusionPartnerUUID);
		tag.putInt("FusionTimer", fusionTimer);
		tag.putString("FusionType", fusionType);
		tag.putString("FusionName", fusionName);
		tag.putBoolean("FusionPartyManaged", fusionPartyManaged);
		if (fusionPrevPartyId != null) com.dragonminez.platform.nbt.NbtCompat.putUUID(tag, "FusionPrevPartyId", fusionPrevPartyId);
		tag.putBoolean("FusionPrevPartyLeader", fusionPrevPartyLeader);
		tag.putInt("PotaraPoseTimer", potaraPoseTimer);
		tag.putInt("EvasionLockTicks", evasionLockTicks);
		if (potaraPartnerUUID != null) com.dragonminez.platform.nbt.NbtCompat.putUUID(tag, "PotaraPartnerUUID", potaraPartnerUUID);
		tag.putBoolean("PotaraLeader", potaraLeader);
		tag.putBoolean("PotaraBeetle", potaraBeetle);
		tag.put("OriginalAppearance", originalAppearance);
		tag.putBoolean("AndroidUpgraded", androidUpgraded);
		tag.putBoolean("RenderKatana", renderKatana);
		tag.putString("BackWeapon", backWeapon);
		tag.putString("ScouterItem", scouterItem);
		tag.putString("PothalaColor", pothalaColor);
		tag.putBoolean("IsPermanentAura", isPermanentAura);
		tag.putBoolean("ForcedAura", forcedAura);
		tag.putBoolean("ForcedCharge", forcedCharge);
		tag.putBoolean("IsStrikeLocked", isStrikeLocked);
		tag.putBoolean("MatchFrozen", matchFrozen);
		tag.putBoolean("SleepRecovering", sleepRecovering);
		tag.putInt("FlightMode", flightMode);
		tag.putBoolean("FlightModeLocked", flightModeLocked);
		tag.putInt("LockedFlightMode", lockedFlightMode);

		ListTag visitedDimensionsTag = new ListTag();
		for (String dimensionId : visitedDimensions) visitedDimensionsTag.add(StringTag.valueOf(dimensionId));
		tag.put("VisitedDimensions", visitedDimensionsTag);

		if (activeShadowDummyUUID != null) com.dragonminez.platform.nbt.NbtCompat.putUUID(tag, "ActiveShadowDummyUUID", activeShadowDummyUUID);
		tag.putInt("ShadowDummyPercent", shadowDummyPercent);
		tag.putInt("ShadowDummyKillCount", shadowDummyKillCount);
		tag.putInt("KiTransferTarget", kiTransferTarget);
		tag.putInt("KiTransferDonors", kiTransferDonors);
		tag.putInt("KiDrainStacks", kiDrainStacks);
		tag.putInt("KiDrainTicks", kiDrainTicks);
		tag.putInt("GodRitualRole", godRitualRole);
		if (godRitualAnchor != null) com.dragonminez.platform.nbt.NbtCompat.putUUID(tag, "GodRitualAnchor", godRitualAnchor);
		tag.putInt("GodRitualTicks", godRitualTicks);
		tag.putLong("GodRitualGlowStart", godRitualGlowStart);
		return tag;
	}

	public void load(CompoundTag tag) {
		this.isAlive = tag.getBooleanOr("IsAlive", false);
		this.forceHalo = tag.getBooleanOr("ForceHalo", false);
		this.tempReturnTimer = tag.getIntOr("TempReturnTimer", 0);
		this.tempReturnsUsed = tag.getIntOr("TempReturnsUsed", 0);
		this.tempReturnReadyAt = tag.getLongOr("TempReturnReadyAt", 0L);
		this.deathCount = tag.getIntOr("DeathCount", 0);
		this.isHasCreatedCharacter = tag.getBooleanOr("HasCreatedChar", false);
		this.isAuraActive = tag.getBooleanOr("AuraActive", false);
		this.isActionCharging = tag.getBooleanOr("Transforming", false);
		this.isTailVisible = tag.getBooleanOr("TailVisible", false);
		this.isDescending = tag.getBooleanOr("Descending", false);
		this.isInKaioPlanet = tag.getBooleanOr("InKaioPlanet", false);
		this.isChargingKi = tag.getBooleanOr("IsChargingKi", false);
		this.kiBurstArmed = tag.getBooleanOr("KiBurstArmed", false);
		this.surgeActive = tag.getBooleanOr("SurgeActive", false);
		this.rageActive = tag.getBooleanOr("RageActive", false);
		this.isBlocking = tag.getBooleanOr("IsBlocking", false);
		this.lastBlockTime = tag.getLongOr("LastBlockTime", 0L);
		this.lastHurtTime = tag.getLongOr("LastHurtTime", 0L);
		this.friendlyFistEnabled = tag.getBooleanOr("FriendlyFistEnabled", false);
		this.stunEffect = tag.getBooleanOr("IsStunned", false);
		this.isKnockedDown = tag.getBooleanOr("IsKnockedDown", false);
		if (tag.contains("SelectedAction")) this.selectedAction = ActionMode.values()[tag.getIntOr("SelectedAction", 0)];
		else this.selectedAction = ActionMode.FORM;
		this.kiWeaponType = tag.getStringOr("KiWeaponType", "");
		this.drainingTargetId = tag.getIntOr("DrainingTargetId", 0);
		this.isFused = tag.getBooleanOr("IsFused", false);
		this.isFusionLeader = tag.getBooleanOr("IsFusionLeader", false);
		if (com.dragonminez.platform.nbt.NbtCompat.hasUUID(tag, "FusionPartnerUUID")) this.fusionPartnerUUID = com.dragonminez.platform.nbt.NbtCompat.getUUID(tag, "FusionPartnerUUID");
		else this.fusionPartnerUUID = null;
		this.fusionTimer = tag.getIntOr("FusionTimer", 0);
		this.fusionType = tag.getStringOr("FusionType", "");
		this.fusionName = tag.getStringOr("FusionName", "");
		this.fusionPartyManaged = tag.getBooleanOr("FusionPartyManaged", false);
		this.fusionPrevPartyId = com.dragonminez.platform.nbt.NbtCompat.hasUUID(tag, "FusionPrevPartyId") ? com.dragonminez.platform.nbt.NbtCompat.getUUID(tag, "FusionPrevPartyId") : null;
		this.fusionPrevPartyLeader = tag.getBooleanOr("FusionPrevPartyLeader", false);
		this.potaraPoseTimer = tag.getIntOr("PotaraPoseTimer", 0);
		this.evasionLockTicks = tag.getIntOr("EvasionLockTicks", 0);
		this.potaraPartnerUUID = com.dragonminez.platform.nbt.NbtCompat.hasUUID(tag, "PotaraPartnerUUID") ? com.dragonminez.platform.nbt.NbtCompat.getUUID(tag, "PotaraPartnerUUID") : null;
		this.potaraLeader = tag.getBooleanOr("PotaraLeader", false);
		this.potaraBeetle = tag.getBooleanOr("PotaraBeetle", false);
		if (tag.contains("OriginalAppearance")) this.originalAppearance = tag.getCompoundOrEmpty("OriginalAppearance");
		else this.originalAppearance = new CompoundTag();
		this.androidUpgraded = tag.getBooleanOr("AndroidUpgraded", false);
		this.renderKatana = tag.getBooleanOr("RenderKatana", false);
		this.backWeapon = tag.getStringOr("BackWeapon", "");
		this.scouterItem = tag.getStringOr("ScouterItem", "");
		this.pothalaColor = tag.getStringOr("PothalaColor", "");
		this.isPermanentAura = tag.getBooleanOr("IsPermanentAura", false);
		this.forcedAura = tag.getBooleanOr("ForcedAura", false);
		this.forcedCharge = tag.getBooleanOr("ForcedCharge", false);
		this.isStrikeLocked = tag.getBooleanOr("IsStrikeLocked", false);
		this.matchFrozen = tag.getBooleanOr("MatchFrozen", false);
		this.sleepRecovering = tag.getBooleanOr("SleepRecovering", false);
		this.flightMode = tag.getIntOr("FlightMode", 0);
		this.flightModeLocked = tag.getBooleanOr("FlightModeLocked", false);
		this.lockedFlightMode = tag.getIntOr("LockedFlightMode", 0);
		this.visitedDimensions.clear();
		if (com.dragonminez.platform.nbt.NbtCompat.contains(tag, "VisitedDimensions", Tag.TAG_LIST)) {
			ListTag visitedDimensionsTag = com.dragonminez.platform.nbt.NbtCompat.getList(tag, "VisitedDimensions", Tag.TAG_STRING);
			for (Tag dimensionTag : visitedDimensionsTag) this.markVisitedDimension(dimensionTag.getAsString());
		}

		this.activeShadowDummyUUID = com.dragonminez.platform.nbt.NbtCompat.hasUUID(tag, "ActiveShadowDummyUUID") ? com.dragonminez.platform.nbt.NbtCompat.getUUID(tag, "ActiveShadowDummyUUID") : null;
		this.shadowDummyPercent = tag.getIntOr("ShadowDummyPercent", 0);
		this.shadowDummyKillCount = tag.contains("ShadowDummyKillCount") ? tag.getIntOr("ShadowDummyKillCount", 0) : 0;
		this.kiTransferTarget = tag.contains("KiTransferTarget") ? tag.getIntOr("KiTransferTarget", 0) : -1;
		this.kiTransferDonors = tag.getIntOr("KiTransferDonors", 0);
		this.kiDrainStacks = tag.getIntOr("KiDrainStacks", 0);
		this.kiDrainTicks = tag.getIntOr("KiDrainTicks", 0);
		this.godRitualRole = tag.getIntOr("GodRitualRole", 0);
		this.godRitualAnchor = com.dragonminez.platform.nbt.NbtCompat.hasUUID(tag, "GodRitualAnchor") ? com.dragonminez.platform.nbt.NbtCompat.getUUID(tag, "GodRitualAnchor") : null;
		this.godRitualTicks = tag.getIntOr("GodRitualTicks", 0);
		this.godRitualGlowStart = tag.getLongOr("GodRitualGlowStart", 0L);
	}

	public void copyFrom(Status other) {
		this.isAlive = other.isAlive;
		this.forceHalo = other.forceHalo;
		this.tempReturnTimer = other.tempReturnTimer;
		this.tempReturnsUsed = other.tempReturnsUsed;
		this.tempReturnReadyAt = other.tempReturnReadyAt;
		this.deathCount = other.deathCount;
		this.isHasCreatedCharacter = other.isHasCreatedCharacter;
		this.isAuraActive = other.isAuraActive;
		this.isActionCharging = other.isActionCharging;
		this.isTailVisible = other.isTailVisible;
		this.isDescending = other.isDescending;
		this.isInKaioPlanet = other.isInKaioPlanet;
		this.isChargingKi = other.isChargingKi;
		this.kiBurstArmed = other.kiBurstArmed;
		this.surgeActive = other.surgeActive;
		this.rageActive = other.rageActive;
		this.isBlocking = other.isBlocking;
		this.lastBlockTime = other.lastBlockTime;
		this.lastHurtTime = other.lastHurtTime;
		this.friendlyFistEnabled = other.friendlyFistEnabled;
		this.stunEffect = other.stunEffect;
		this.isKnockedDown = other.isKnockedDown;
		this.selectedAction = other.selectedAction;
		this.kiWeaponType = other.kiWeaponType;
		this.drainingTargetId = other.drainingTargetId;
		this.isFused = other.isFused;
		this.isFusionLeader = other.isFusionLeader;
		this.fusionPartnerUUID = other.fusionPartnerUUID;
		this.fusionTimer = other.fusionTimer;
		this.fusionType = other.fusionType;
		this.fusionName = other.fusionName;
		this.fusionPartyManaged = other.fusionPartyManaged;
		this.fusionPrevPartyId = other.fusionPrevPartyId;
		this.fusionPrevPartyLeader = other.fusionPrevPartyLeader;
		this.potaraPoseTimer = other.potaraPoseTimer;
		this.evasionLockTicks = other.evasionLockTicks;
		this.potaraPartnerUUID = other.potaraPartnerUUID;
		this.potaraLeader = other.potaraLeader;
		this.potaraBeetle = other.potaraBeetle;
		this.originalAppearance = other.originalAppearance.copy();
		this.androidUpgraded = other.androidUpgraded;
		this.renderKatana = other.renderKatana;
		this.backWeapon = other.backWeapon;
		this.pothalaColor = other.pothalaColor;
		this.scouterItem = other.scouterItem;
		this.isPermanentAura = other.isPermanentAura;
		this.forcedAura = other.forcedAura;
		this.forcedCharge = other.forcedCharge;
		this.isStrikeLocked = other.isStrikeLocked;
		this.matchFrozen = other.matchFrozen;
		this.sleepRecovering = other.sleepRecovering;
		this.flightMode = other.flightMode;
		this.flightModeLocked = other.flightModeLocked;
		this.lockedFlightMode = other.lockedFlightMode;
		this.visitedDimensions.clear();
		this.visitedDimensions.addAll(other.visitedDimensions);
		this.activeShadowDummyUUID = other.activeShadowDummyUUID;
		this.shadowDummyPercent = other.shadowDummyPercent;
		this.shadowDummyKillCount = other.shadowDummyKillCount;
		this.kiTransferTarget = other.kiTransferTarget;
		this.kiTransferDonors = other.kiTransferDonors;
		this.kiDrainStacks = other.kiDrainStacks;
		this.kiDrainTicks = other.kiDrainTicks;
		this.godRitualRole = other.godRitualRole;
		this.godRitualAnchor = other.godRitualAnchor;
		this.godRitualTicks = other.godRitualTicks;
		this.godRitualGlowStart = other.godRitualGlowStart;
	}
}
