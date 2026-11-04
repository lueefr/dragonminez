package com.dragonminez.common.progression;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.api.rules.Conditions;
import net.minecraft.server.level.ServerPlayer;

public final class RebirthService {
    private RebirthService() {}
    public static boolean allows(StatsData data) {
        var config=ConfigManager.getRebirth();
        return config.enabled&&data.getStatus().isHasCreatedCharacter()&&!data.getStatus().isAlive()
                && !data.getStatus().isFused()&&Conditions.test(new Conditions.Context(data,null),config.requirements);
    }
    public static boolean begin(ServerPlayer player,StatsData data) {
        if(!allows(data)||!player.level().dimension().equals(com.dragonminez.server.world.dimension.OtherworldDimension.OTHERWORLD_KEY))return false;
        var config=ConfigManager.getRebirth();var keep=config.retention;
        if(data.getRebirth().deathLevel()==0)data.getRebirth().record(data.getLevel());
        var saved=data.save();
        data.getRebirth().prepare();
        data.resetPlayerProgress(player,keep.statsPercent,keep.skills,false);
        if(!keep.mastery) {
            data.getCharacter().getFormMasteries().load(new net.minecraft.nbt.CompoundTag());
            data.getCharacter().getStackFormMasteries().load(new net.minecraft.nbt.CompoundTag());
        }
        if(!keep.content)data.getContentProgress().load(new net.minecraft.nbt.CompoundTag());
        if(keep.story)data.getPlayerQuestData().deserializeNBT(saved.getCompound("PlayerQuestData"));
        if(keep.techniques)data.getTechniques().load(saved.getCompound("Techniques"));else data.getTechniques().clearAllTechniques();
        if(keep.effects)data.getEffects().load(saved.getCompound("Effects"));else data.getEffects().removeAllEffects();
        if(keep.cooldowns)data.getCooldowns().load(saved.getCompound("Cooldowns"));
        if(keep.bonusStats)data.getBonusStats().load(saved.getCompound("BonusStats"));
        if(!keep.history)data.getFormHistory().load(new net.minecraft.nbt.CompoundTag());
        data.getResources().setTrainingPointsExact(saved.getCompound("Resources").getDouble("TrainingPointsD")*keep.tpPercent/100.0);
        data.getResources().setPendingAttributePoints((int)((long)saved.getCompound("Resources").getInt("PendingAttributePoints")*keep.pendingPercent/100));
        if(keep.alignment)data.getResources().setAlignment(saved.getCompound("Resources").getInt("Alignment"));
        if(!keep.inventory)player.getInventory().clearContent();
        data.getStatus().setAlive(true);
        data.getStatus().setHasCreatedCharacter(false);
        com.dragonminez.server.util.BabaReviveService.teleportToLivingWorld(player);
        return true;
    }
    public static void finish(StatsData data) {
        if(!data.getRebirth().pending())return;
        var config=ConfigManager.getRebirth();var cap=config.bonus.cap;
        double budget=!cap.enabled?0:switch(cap.mode) {
            case "tp" -> cap.tp;
            case "level" -> data.levelCost(cap.level);
            default -> data.levelCost(data.getRebirth().deathLevel());
        };
        data.getRebirth().begin(config,budget);
        if(!config.retention.raceHistory)data.getRebirth().resetOrigins(data.getCharacter().getRaceName());
    }
}
