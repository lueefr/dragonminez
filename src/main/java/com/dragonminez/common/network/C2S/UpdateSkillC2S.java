package com.dragonminez.common.network.C2S;

import com.dragonminez.common.alignment.NpcDispositionService;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.init.entities.questnpc.QuestNPCEntity;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.skills.Skill;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.EvasionAttackData;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import com.dragonminez.common.util.FormRequisites;
import com.dragonminez.server.storage.StorageManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import com.dragonminez.compat.network.NetworkEvent;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Map;
import java.util.function.Supplier;

public class UpdateSkillC2S {

	private static final double MASTER_SHOP_RANGE = 10.0;
	private static final Set<String> TOGGLEABLE_SKILLS = Set.of("kiprotection", "ki_infusion", "jump", "sprint", "kisense");

	public enum SkillAction {
		TOGGLE, UPGRADE, PURCHASE
	}

	private final String skillName;
	private final SkillAction action;
	private final int cost;

	public UpdateSkillC2S(SkillAction action, String skillName, int cost) {
		this.skillName = skillName;
		this.action = action;
		this.cost = cost;
	}

	public UpdateSkillC2S(FriendlyByteBuf buf) {
		this.skillName = buf.readUtf(128);
		this.action = buf.readEnum(SkillAction.class);
		this.cost = buf.readInt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeUtf(this.skillName, 128);
		buf.writeEnum(this.action);
		buf.writeInt(this.cost);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player != null && !StorageManager.isLoadPending(player)) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                    Component failure=apply(player,data);
                    if(failure!=null)com.dragonminez.api.forms.FormAccess.notice(player,failure,null);
					StatsSyncS2C.sendRequested(player);
				});
			}
		});
		ctx.get().setPacketHandled(true);
	}

    /** Server-authoritative transaction; a failure never debits TP or grants a level. */
    public Component apply(ServerPlayer player,StatsData data) {
        if(skillName==null||skillName.isBlank())return failure("unavailable");
        String id=skillName.toLowerCase(Locale.ROOT);
        Skill skill=data.getSkills().getSkill(id);
        if(action==SkillAction.TOGGLE){
            if(TOGGLEABLE_SKILLS.contains(id)&&!data.getStatus().isStunned()&&skill!=null&&skill.getLevel()>0)skill.setActive(!skill.isActive());
            return null;
        }
        if(data.getStatus().isFused()||data.getStatus().getFusionPartnerUUID()!=null)
            return Component.translatable("message.dragonminez.fusion.action_blocked");
        boolean form=ConfigManager.getSkillsConfig().getFormSkills().contains(id);
        boolean owned=skill!=null&&skill.getLevel()>0;
        if(!owned&&!isSkillAllowedForPlayerRace(data,id)&&!ConfigManager.getSkillsConfig().isTechniqueSkill(id))return failure("race");
        int current=action==SkillAction.PURCHASE?0:skill==null?0:skill.getLevel();
        if(action==SkillAction.PURCHASE){
            if(owned)return failure("owned");
            Component master=nearbyMasterFailure(player,id);
            if(master!=null)return master;
        }else{
            if(skill==null)return failure("unavailable");
            if(current<=0&&(ConfigManager.getSkillsConfig().getStackSkills().contains(id)||isMasterOnlyFormSkill(data,id)))return failure("master");
            refreshRuntimeMaxLevel(data,id,skill);
            if(skill.isMaxLevel())return failure("maximum");
            if(id.equals("potentialunlock")&&current==10)return failure("grant_only");
        }
        FormRequisites.Lock lock=FormRequisites.checkSkillLevel(data,id,current+1,false);
        if(lock.isLocked())return FormRequisites.describeInline(lock,false);
        int price=computeTpCost(data,id,current);
        if(price<0)return failure("grant_only");
        if(data.getResources().getTrainingPointsExact()<price)
            return Component.translatable("rules.dragonminez.purchase.tp",price,data.getResources().getTrainingPointsExact());
        data.getResources().removeTrainingPoints(price);
        if(action==SkillAction.PURCHASE){
            data.getSkills().setSkillLevel(id,1);
            Skill purchased=data.getSkills().getSkill(id);
            if(purchased!=null)refreshRuntimeMaxLevel(data,id,purchased);
        }else skill.addLevel(1);
        if(current==0)unlockTechniqueIfPresent(data,id);
        return null;
    }
    private static Component failure(String reason){return Component.translatable("rules.dragonminez.purchase."+reason);}

    private static Component nearbyMasterFailure(ServerPlayer player,String skillName){
        AABB range=player.getBoundingBox().inflate(MASTER_SHOP_RANGE);
        Component blocker=null;
        for(MastersEntity master:player.serverLevel().getEntitiesOfClass(MastersEntity.class,range)){
            if(master instanceof com.dragonminez.common.init.entities.questnpc.CustomNpcEntity
                    ||!master.isAlive()||master.distanceToSqr(player)>MASTER_SHOP_RANGE*MASTER_SHOP_RANGE)continue;
            String npc=master.getMasterName();
            if(!declaresSkill(npc,skillName))continue;
            Component reason=NpcDispositionService.getServiceBlocker(player,npc);
            if(reason==null)return null;
            if(blocker==null)blocker=reason;
        }
        for(QuestNPCEntity npc:player.serverLevel().getEntitiesOfClass(QuestNPCEntity.class,range)){
            if(!npc.isAlive()||npc.distanceToSqr(player)>MASTER_SHOP_RANGE*MASTER_SHOP_RANGE)continue;
            if(npc instanceof com.dragonminez.common.init.entities.questnpc.CustomNpcEntity
                    &&com.dragonminez.api.npc.CustomNpcRegistry.INSTANCE.get(npc.getNpcId()).isEmpty())continue;
            if(!declaresSkill(npc.getNpcId(),skillName))continue;
            Component reason=NpcDispositionService.getServiceBlocker(player,npc.getNpcId());
            if(reason==null)return null;
            if(blocker==null)blocker=reason;
        }
        return blocker==null?failure("master"):blocker;
    }

	private static int computeTpCost(StatsData data, String skillName, int currentLevel) {
		if (skillName == null || currentLevel < 0) return -1;
		var skillsConfig = ConfigManager.getSkillsConfig();
		if (skillsConfig.getFormSkills().contains(skillName.toLowerCase())) {
			String raceName = data.getCharacter() != null ? data.getCharacter().getRaceName() : "";
			var charConfig = ConfigManager.getRaceCharacter(raceName);
			if (charConfig == null) return -1;
			Integer[] prices = charConfig.getFormSkillTpCosts(skillName);
			if (prices == null || currentLevel >= prices.length || prices[currentLevel] == null) return -1;
			return prices[currentLevel] < 0 ? -1 : prices[currentLevel];
		}
		var skillCosts = skillsConfig.getSkillCosts(skillName);
		if (skillCosts == null || skillCosts.getCosts() == null) return -1;
		java.util.List<Integer> costs = skillCosts.getCosts();
		if (currentLevel >= costs.size() || costs.get(currentLevel) == null) return -1;
		return costs.get(currentLevel) < 0 ? -1 : costs.get(currentLevel);
	}

    private static boolean declaresSkill(String npcName,String skillName){
        if(npcName==null||npcName.isBlank())return false;
        var custom=com.dragonminez.api.npc.CustomNpcRegistry.INSTANCE.get(npcName);
        if(custom.isPresent())return custom.get().offers().skills().contains(skillName.toLowerCase(Locale.ROOT));
        Map<String,List<String>> offerings=ConfigManager.getSkillsConfig().getSkillOfferings();
        List<String> offered=offerings.getOrDefault(npcName.toLowerCase(Locale.ROOT),offerings.get("default"));
        return offered!=null&&(offered.contains(skillName)||offered.contains(skillName.toLowerCase(Locale.ROOT)));
    }

	private static boolean isMasterOnlyFormSkill(StatsData data, String skillName) {
		if (data == null || skillName == null) return false;
		if (!ConfigManager.getSkillsConfig().getFormSkills().contains(skillName.toLowerCase())) return false;
		String raceName = data.getCharacter() != null ? data.getCharacter().getRaceName() : "";
		var charConfig = ConfigManager.getRaceCharacter(raceName);
		return charConfig != null && charConfig.isFormSkillBuyFromMaster(skillName);
	}

	private static boolean isSkillAllowedForPlayerRace(StatsData data, String skillName) {
		if (data == null || skillName == null || skillName.isEmpty()) return false;

		String raceName = data.getCharacter() != null ? data.getCharacter().getRaceName() : "";
		return ConfigManager.getSkillsConfig().isSkillAllowedForRace(skillName, raceName, data.getStatus().isAndroidUpgraded());
	}

	private void unlockTechniqueIfPresent(com.dragonminez.common.stats.StatsData data, String techId) {
		if (PredefinedTechniques.REGISTRY.containsKey(techId)) {
			KiAttackData template = PredefinedTechniques.REGISTRY.get(techId);
			KiAttackData clone = new KiAttackData();
			clone.load(template.save());
			data.getTechniques().unlockTechnique(clone);
		} else if (PredefinedTechniques.STRIKE_REGISTRY.containsKey(techId)) {
			StrikeAttackData template = PredefinedTechniques.STRIKE_REGISTRY.get(techId);
			StrikeAttackData clone = new StrikeAttackData();
			clone.load(template.save());
			data.getTechniques().unlockTechnique(clone);
		} else if (PredefinedTechniques.EVASION_REGISTRY.containsKey(techId)) {
			EvasionAttackData template = PredefinedTechniques.EVASION_REGISTRY.get(techId);
			EvasionAttackData clone = new EvasionAttackData();
			clone.load(template.save());
			data.getTechniques().unlockTechnique(clone);
		}
	}

	private static void refreshRuntimeMaxLevel(StatsData data, String skillName, Skill skill) {
		String normalizedSkill = skillName.toLowerCase();
		var skillsConfig = ConfigManager.getSkillsConfig();

		if (skillsConfig.getFormSkills().contains(normalizedSkill)) {
			String raceName = data.getCharacter().getRaceName();
			if (raceName == null || raceName.isEmpty()) return;

			var charConfig = ConfigManager.getRaceCharacter(raceName);
			int maxLevel = charConfig.getFormSkillTpCosts(normalizedSkill).length;
			skill.setMaxLevel(maxLevel);
			return;
		}

		int maxLevel = 0;
		var skillCosts = skillsConfig.getSkillCosts(normalizedSkill);
		if (skillCosts != null && skillCosts.getCosts() != null) maxLevel = skillCosts.getCosts().size();

		if ("potentialunlock".equalsIgnoreCase(normalizedSkill)) maxLevel = Math.min(maxLevel, 30);
		else maxLevel = Math.min(maxLevel, 50);

		skill.setMaxLevel(maxLevel);
	}
}