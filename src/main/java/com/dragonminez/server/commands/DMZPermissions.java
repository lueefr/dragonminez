package com.dragonminez.server.commands;

import com.dragonminez.Reference;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class DMZPermissions {

	private static final List<PermissionNode<Boolean>> NODES = new ArrayList<>();

	// Admin (All)
	public static final PermissionNode<Boolean> ADMIN = register("admin", "permission.dragonminez.admin.description", (player, uuid, context) -> false);

	// Stats
	public static final PermissionNode<Boolean> STATS_SET_SELF = register("dmzstats.set.self", "permission.dragonminez.dmzstats.set.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> STATS_SET_OTHERS = register("dmzstats.set.others", "permission.dragonminez.dmzstats.set.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> STATS_ADD_SELF = register("dmzstats.add.self", "permission.dragonminez.dmzstats.add.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> STATS_ADD_OTHERS = register("dmzstats.add.others", "permission.dragonminez.dmzstats.add.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> STATS_INFO_SELF = register("dmzstats.info.self", "permission.dragonminez.dmzstats.info.self.description", (player, uuid, context) -> true);
	public static final PermissionNode<Boolean> STATS_INFO_OTHERS = register("dmzstats.info.others", "permission.dragonminez.dmzstats.info.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> STATS_RESET_SELF = register("dmzstats.reset.self", "permission.dragonminez.dmzstats.reset.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> STATS_RESET_OTHERS = register("dmzstats.reset.others", "permission.dragonminez.dmzstats.reset.others.description", (player, uuid, context) -> false);

	// Skills
	public static final PermissionNode<Boolean> SKILLS_SET_SELF = register("dmzskill.set.self", "permission.dragonminez.dmzskill.set.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> SKILLS_SET_OTHERS = register("dmzskill.set.others", "permission.dragonminez.dmzskill.set.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> SKILLS_ADD_SELF = register("dmzskill.add.self", "permission.dragonminez.dmzskill.add.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> SKILLS_ADD_OTHERS = register("dmzskill.add.others", "permission.dragonminez.dmzskill.add.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> SKILLS_REMOVE_SELF = register("dmzskill.remove.self", "permission.dragonminez.dmzskill.remove.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> SKILLS_REMOVE_OTHERS = register("dmzskill.remove.others", "permission.dragonminez.dmzskill.remove.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> SKILLS_LIST_SELF = register("dmzskill.list.self", "permission.dragonminez.dmzskill.list.self.description", (player, uuid, context) -> true);
	public static final PermissionNode<Boolean> SKILLS_LIST_OTHERS = register("dmzskill.list.others", "permission.dragonminez.dmzskill.list.others.description", (player, uuid, context) -> false);

	// Techniques
	public static final PermissionNode<Boolean> TECH_ADD_SELF = register("dmztech.add.self", "permission.dragonminez.dmztech.add.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> TECH_ADD_OTHERS = register("dmztech.add.others", "permission.dragonminez.dmztech.add.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> TECH_REMOVE_SELF = register("dmztech.remove.self", "permission.dragonminez.dmztech.remove.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> TECH_REMOVE_OTHERS = register("dmztech.remove.others", "permission.dragonminez.dmztech.remove.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> TECH_LIST_SELF = register("dmztech.list.self", "permission.dragonminez.dmztech.list.self.description", (player, uuid, context) -> true);
	public static final PermissionNode<Boolean> TECH_LIST_OTHERS = register("dmztech.list.others", "permission.dragonminez.dmztech.list.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> TECH_EXP_SELF = register("dmztech.experience.self", "permission.dragonminez.dmztech.experience.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> TECH_EXP_OTHERS = register("dmztech.experience.others", "permission.dragonminez.dmztech.experience.others.description", (player, uuid, context) -> false);

	// Technique cooldowns
	public static final PermissionNode<Boolean> COOLDOWNS_SELF = register("dmzcooldowns.self", "permission.dragonminez.dmzcooldowns.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> COOLDOWNS_OTHERS = register("dmzcooldowns.others", "permission.dragonminez.dmzcooldowns.others.description", (player, uuid, context) -> false);

	// Bonus
	public static final PermissionNode<Boolean> BONUS_ADD_SELF = register("dmzbonus.add.self", "permission.dragonminez.dmzbonus.add.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> BONUS_ADD_OTHERS = register("dmzbonus.add.others", "permission.dragonminez.dmzbonus.add.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> BONUS_CLEAR_SELF = register("dmzbonus.clear.self", "permission.dragonminez.dmzbonus.clear.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> BONUS_CLEAR_OTHERS = register("dmzbonus.clear.others", "permission.dragonminez.dmzbonus.clear.others.description", (player, uuid, context) -> false);

	// Effects
	public static final PermissionNode<Boolean> EFFECTS_GIVE_SELF = register("dmzeffect.give.self", "permission.dragonminez.dmzeffect.give.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> EFFECTS_GIVE_OTHERS = register("dmzeffect.give.others", "permission.dragonminez.dmzeffect.give.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> EFFECTS_REMOVE_SELF = register("dmzeffect.remove.self", "permission.dragonminez.dmzeffect.remove.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> EFFECTS_REMOVE_OTHERS = register("dmzeffect.remove.others", "permission.dragonminez.dmzeffect.remove.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> EFFECTS_CLEAR_SELF = register("dmzeffect.clear.self", "permission.dragonminez.dmzeffect.clear.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> EFFECTS_CLEAR_OTHERS = register("dmzeffect.clear.others", "permission.dragonminez.dmzeffect.clear.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> EFFECTS_LIST_SELF = register("dmzeffect.list.self", "permission.dragonminez.dmzeffect.list.self.description", (player, uuid, context) -> true);
	public static final PermissionNode<Boolean> EFFECTS_LIST_OTHERS = register("dmzeffect.list.others", "permission.dragonminez.dmzeffect.list.others.description", (player, uuid, context) -> false);

	// Points
	public static final PermissionNode<Boolean> POINTS_SET_SELF = register("dmzpoints.set.self", "permission.dragonminez.dmzpoints.set.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> POINTS_SET_OTHERS = register("dmzpoints.set.others", "permission.dragonminez.dmzpoints.set.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> POINTS_ADD_SELF = register("dmzpoints.add.self", "permission.dragonminez.dmzpoints.add.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> POINTS_ADD_OTHERS = register("dmzpoints.add.others", "permission.dragonminez.dmzpoints.add.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> POINTS_REMOVE_SELF = register("dmzpoints.remove.self", "permission.dragonminez.dmzpoints.remove.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> POINTS_REMOVE_OTHERS = register("dmzpoints.remove.others", "permission.dragonminez.dmzpoints.remove.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> POINTS_INFO_SELF = register("dmzpoints.info.self", "permission.dragonminez.dmzpoints.info.self.description", (player, uuid, context) -> true);
	public static final PermissionNode<Boolean> POINTS_INFO_OTHERS = register("dmzpoints.info.others", "permission.dragonminez.dmzpoints.info.others.description", (player, uuid, context) -> false);

	// Quests
	public static final PermissionNode<Boolean> QUEST_LIST_SELF = register("dmzquest.list.self", "permission.dragonminez.dmzquest.list.self.description", (player, uuid, context) -> true);
	public static final PermissionNode<Boolean> QUEST_LIST_OTHERS = register("dmzquest.list.others", "permission.dragonminez.dmzquest.list.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_INFO = register("dmzquest.info", "permission.dragonminez.dmzquest.info.description", (player, uuid, context) -> true);
	public static final PermissionNode<Boolean> QUEST_START_SELF = register("dmzquest.start.self", "permission.dragonminez.dmzquest.start.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_START_OTHERS = register("dmzquest.start.others", "permission.dragonminez.dmzquest.start.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_FINISH_SELF = register("dmzquest.finish.self", "permission.dragonminez.dmzquest.finish.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_FINISH_OTHERS = register("dmzquest.finish.others", "permission.dragonminez.dmzquest.finish.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_FAIL_SELF = register("dmzquest.fail.self", "permission.dragonminez.dmzquest.fail.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_FAIL_OTHERS = register("dmzquest.fail.others", "permission.dragonminez.dmzquest.fail.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_RESET_SELF = register("dmzquest.reset.self", "permission.dragonminez.dmzquest.reset.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_RESET_OTHERS = register("dmzquest.reset.others", "permission.dragonminez.dmzquest.reset.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_TRACK_SELF = register("dmzquest.track.self", "permission.dragonminez.dmzquest.track.self.description", (player, uuid, context) -> true);
	public static final PermissionNode<Boolean> QUEST_TRACK_OTHERS = register("dmzquest.track.others", "permission.dragonminez.dmzquest.track.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_STARTSAGA_SELF = register("dmzquest.startsaga.self", "permission.dragonminez.dmzquest.startsaga.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_STARTSAGA_OTHERS = register("dmzquest.startsaga.others", "permission.dragonminez.dmzquest.startsaga.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_FINISHSAGA_SELF = register("dmzquest.finishsaga.self", "permission.dragonminez.dmzquest.finishsaga.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_FINISHSAGA_OTHERS = register("dmzquest.finishsaga.others", "permission.dragonminez.dmzquest.finishsaga.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_RESETSAGA_SELF = register("dmzquest.resetsaga.self", "permission.dragonminez.dmzquest.resetsaga.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_RESETSAGA_OTHERS = register("dmzquest.resetsaga.others", "permission.dragonminez.dmzquest.resetsaga.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_DIFFICULTY_SELF = register("dmzquest.difficulty.self", "permission.dragonminez.dmzquest.difficulty.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_DIFFICULTY_OTHERS = register("dmzquest.difficulty.others", "permission.dragonminez.dmzquest.difficulty.others.description", (player, uuid, context) -> false);

	// Locate
	public static final PermissionNode<Boolean> LOCATE = register("dmzlocate", "permission.dragonminez.dmzlocate.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> STRUCTURE = register("dmzstructure", "permission.dragonminez.dmzstructure.description", (player, uuid, context) -> false);

	// Dragon Balls
	public static final PermissionNode<Boolean> DRAGONBALLS_STATUS = register("dmzdragonballs.status", "permission.dragonminez.dmzdragonballs.status.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> DRAGONBALLS_REGENERATE = register("dmzdragonballs.regenerate", "permission.dragonminez.dmzdragonballs.regenerate.description", (player, uuid, context) -> false);

	// Revive
	public static final PermissionNode<Boolean> REVIVE_SELF = register("dmzrevive.self", "permission.dragonminez.dmzrevive.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> REVIVE_OTHERS = register("dmzrevive.others", "permission.dragonminez.dmzrevive.others.description", (player, uuid, context) -> false);

	// Party
	public static final PermissionNode<Boolean> PARTY_USE = register("dmzparty.use", "permission.dragonminez.dmzparty.use.description", (player, uuid, context) -> true);

	// Mastery
	public static final PermissionNode<Boolean> MASTERY_SET = register("dmzmastery.set", "permission.dragonminez.dmzmastery.set.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> MASTERY_ADD = register("dmzmastery.add", "permission.dragonminez.dmzmastery.add.description", (player, uuid, context) -> false);

	// Reload
	public static final PermissionNode<Boolean> RELOAD = register("dmz.reload", "permission.dragonminez.dmz.reload.description", (player, uuid, context) -> false);

	// Forms
	public static final PermissionNode<Boolean> FORMS_SET_SELF = register("dmzform.set.self", "permission.dragonminez.dmzform.set.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> FORMS_SET_OTHERS = register("dmzform.set.others", "permission.dragonminez.dmzform.set.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> FORMS_ADD_SELF = register("dmzform.add.self", "permission.dragonminez.dmzform.add.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> FORMS_ADD_OTHERS = register("dmzform.add.others", "permission.dragonminez.dmzform.add.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> FORMS_REMOVE_SELF = register("dmzform.remove.self", "permission.dragonminez.dmzform.remove.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> FORMS_REMOVE_OTHERS = register("dmzform.remove.others", "permission.dragonminez.dmzform.remove.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> FORMS_LIST_SELF = register("dmzform.list.self", "permission.dragonminez.dmzform.list.self.description", (player, uuid, context) -> true);
	public static final PermissionNode<Boolean> FORMS_LIST_OTHERS = register("dmzform.list.others", "permission.dragonminez.dmzform.list.others.description", (player, uuid, context) -> false);

	// Racial Skill reset
	public static final PermissionNode<Boolean> RACIAL_RESET_SELF = register("dmzracial.reset.self", "permission.dragonminez.dmzracial.reset.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> RACIAL_RESET_OTHERS = register("dmzracial.reset.others", "permission.dragonminez.dmzracial.reset.others.description", (player, uuid, context) -> false);

	// Weights
	public static final PermissionNode<Boolean> WEIGHT_GIVE = register("dmzweight.give", "permission.dragonminez.dmzweight.give.description", (player, uuid, context) -> false);

	// Alignment
	public static final PermissionNode<Boolean> ALIGNMENT_SET_SELF = register("dmzalignment.set.self", "permission.dragonminez.dmzalignment.set.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> ALIGNMENT_SET_OTHERS = register("dmzalignment.set.others", "permission.dragonminez.dmzalignment.set.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> ALIGNMENT_ADD_SELF = register("dmzalignment.add.self", "permission.dragonminez.dmzalignment.add.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> ALIGNMENT_ADD_OTHERS = register("dmzalignment.add.others", "permission.dragonminez.dmzalignment.add.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> ALIGNMENT_REMOVE_SELF = register("dmzalignment.remove.self", "permission.dragonminez.dmzalignment.remove.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> ALIGNMENT_REMOVE_OTHERS = register("dmzalignment.remove.others", "permission.dragonminez.dmzalignment.remove.others.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> ALIGNMENT_INFO_SELF = register("dmzalignment.info.self", "permission.dragonminez.dmzalignment.info.self.description", (player, uuid, context) -> true);
	public static final PermissionNode<Boolean> ALIGNMENT_INFO_OTHERS = register("dmzalignment.info.others", "permission.dragonminez.dmzalignment.info.others.description", (player, uuid, context) -> false);

	// Tail
	public static final PermissionNode<Boolean> TAIL_SELF = register("dmztail.self", "permission.dragonminez.dmztail.self.description", (player, uuid, context) -> true);
	public static final PermissionNode<Boolean> TAIL_OTHERS = register("dmztail.others", "permission.dragonminez.dmztail.others.description", (player, uuid, context) -> false);

	// Minigames
	public static final PermissionNode<Boolean> MINIGAME_SELF = register("dmzminigame.self", "permission.dragonminez.dmzminigame.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> MINIGAME_OTHERS = register("dmzminigame.others", "permission.dragonminez.dmzminigame.others.description", (player, uuid, context) -> false);

	// Halo
	public static final PermissionNode<Boolean> HALO_SELF = register("dmzhalo.self", "permission.dragonminez.dmzhalo.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> HALO_OTHERS = register("dmzhalo.others", "permission.dragonminez.dmzhalo.others.description", (player, uuid, context) -> false);

	// Hair
	public static final PermissionNode<Boolean> HAIR_SELF = register("dmzhair.self", "permission.dragonminez.dmzhair.self.description", (player, uuid, context) -> true);
	public static final PermissionNode<Boolean> HAIR_OTHERS = register("dmzhair.others", "permission.dragonminez.dmzhair.others.description", (player, uuid, context) -> false);

	// Class
	public static final PermissionNode<Boolean> CLASS_SET_SELF = register("dmzclass.set.self", "permission.dragonminez.dmzclass.set.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> CLASS_SET_OTHERS = register("dmzclass.set.others", "permission.dragonminez.dmzclass.set.others.description", (player, uuid, context) -> false);

	// Trigger
	public static final PermissionNode<Boolean> TRIGGER_SELF = register("dmztrigger.self", "permission.dragonminez.dmztrigger.self.description", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> TRIGGER_OTHERS = register("dmztrigger.others", "permission.dragonminez.dmztrigger.others.description", (player, uuid, context) -> false);

	public static void init() {}

	private static PermissionNode<Boolean> register(String node, String description, PermissionNode.PermissionResolver<Boolean> defaultResolver) {
		PermissionNode<Boolean> permissionNode = new PermissionNode<>(Reference.MOD_ID, node, PermissionTypes.BOOLEAN, defaultResolver);
		permissionNode.setInformation(com.dragonminez.api.lang.ConfigTranslations.text(description), com.dragonminez.api.lang.ConfigTranslations.text(description));
		NODES.add(permissionNode);
		return permissionNode;
	}

	@SubscribeEvent
	public static void onPermissionGather(PermissionGatherEvent.Nodes event) {
		NODES.forEach(event::addNodes);
	}

	public static boolean hasPermission(CommandSourceStack source, PermissionNode<Boolean> node) {
		if (source.getEntity() instanceof ServerPlayer player) {
			return PermissionAPI.getPermission(player, node) || hasRegularPermission(player);
		}
		return true;
	}

	private static boolean hasRegularPermission(ServerPlayer player) {
		return PermissionAPI.getPermission(player, ADMIN) || player.hasPermissions(2);
	}

	public static boolean check(CommandSourceStack source, PermissionNode<Boolean> selfNode, PermissionNode<Boolean> othersNode) {
		return hasPermission(source, selfNode) || hasPermission(source, othersNode);
	}
}
