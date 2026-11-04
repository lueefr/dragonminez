package com.dragonminez.common.progression;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsData;
import com.google.gson.*;
import net.minecraft.server.level.ServerPlayer;

public final class RaceChange {
    private RaceChange() {}
    public static void validate(JsonElement value) {
        var object=value.getAsJsonObject();com.dragonminez.api.rules.Conditions.id(object.get("id").getAsString());
        for(String key:object.keySet()) {
            if(!java.util.Set.of("id","characterClass","keepStats","keepSkills","keepMastery","keepRacial","keepTp").contains(key))throw new IllegalArgumentException("Unknown race-change field: "+key);
            if(key.startsWith("keep")&&(!object.get(key).isJsonPrimitive()||!object.get(key).getAsJsonPrimitive().isBoolean()))throw new IllegalArgumentException(key+" must be boolean");
        }
    }
    private static boolean keep(JsonObject object,String key) { return !object.has(key)||object.get(key).getAsBoolean(); }
    public static void apply(ServerPlayer player,StatsData data,JsonElement value) {
        var object=value.getAsJsonObject();String race=object.get("id").getAsString();
        var config=ConfigManager.getRaceStats(race);var looks=ConfigManager.getRaceCharacter(race);
        if(config==null||looks==null||data.getStatus().isFused()||!data.getStatus().isHasCreatedCharacter())throw new IllegalArgumentException("Race change unavailable: "+race);
        var character=data.getCharacter();String clazz=object.has("characterClass")?object.get("characterClass").getAsString():character.getCharacterClass();
        if(!config.hasClass(clazz)) {
            if(object.has("characterClass"))throw new IllegalArgumentException("Unknown character class: "+clazz);
            clazz=config.hasClass("warrior")?"warrior":config.getClasses().keySet().stream().sorted().findFirst().orElseThrow();
        }
        character.clearActiveForm(player);character.clearActiveStackForm(player);
        com.dragonminez.server.events.players.statuseffect.TransformStatusHandler.clearAllPersistentFormEffects(player);
        if(!keep(object,"keepStats")) {
            var stats=data.getStats();stats.setStrength(0);stats.setStrikePower(0);stats.setResistance(0);stats.setVitality(0);stats.setKiPower(0);stats.setEnergy(0);
            data.getDynamicGrowth().clearGrowthStats();
        }
        if(!keep(object,"keepSkills"))data.getSkills().removeAllSkills();
        if(!keep(object,"keepMastery")){character.getFormMasteries().clear();character.getStackFormMasteries().clear();}
        if(!keep(object,"keepRacial")){data.getRacialData().copyFrom(new com.dragonminez.common.racial.RacialData());data.getStatus().setAndroidUpgraded(false);}
        if(!keep(object,"keepTp"))data.getResources().setTrainingPointsExact(0);
        int alignment=data.getResources().getAlignment();
        data.initializeWithRaceAndClass(race,clazz,character.getGender(),character.getHairId(),character.getHairBase(),character.getBodyType(),
                character.getEyesType(),character.getNoseType(),character.getMouthType(),character.getTattooType(),character.getBoobScale(),character.getActiveHeadBone(),
                character.getHairColor(),character.getBodyColor(),character.getBodyColor2(),character.getBodyColor3(),character.getEye1Color(),character.getEye2Color(),character.getAuraColor());
        data.getResources().setAlignment(alignment);
        character.setSelectedFormGroup(com.dragonminez.common.util.TransformationsHelper.getGroupWithFirstAvailableForm(data));
        character.setSelectedForm(com.dragonminez.common.util.TransformationsHelper.getFirstAvailableForm(data));
        character.setSelectedStackFormGroup(com.dragonminez.common.util.TransformationsHelper.getGroupWithFirstAvailableStackForm(data));
        character.setSelectedStackForm(com.dragonminez.common.util.TransformationsHelper.getFirstAvailableStackForm(data));
        com.dragonminez.server.events.players.StatsEvents.applyHealthBonus(player);player.refreshDimensions();
    }
}
