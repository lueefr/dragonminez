package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.questnpc.QuestNPCEntity;
import com.dragonminez.common.quest.Difficulty;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.QuestService;
import com.dragonminez.common.quest.QuestTextFormatter;
import com.dragonminez.common.quest.Saga;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class StoryCommand {

	private static final int NPC_SEARCH_RADIUS = 16;

	private static final SuggestionProvider<CommandSourceStack> QUEST_SUGGESTIONS = (context, builder) ->
			SharedSuggestionProvider.suggest(suggestionIds(QuestRegistry.getAllQuests().keySet()), builder);

	private static final SuggestionProvider<CommandSourceStack> QUEST_OR_ALL_SUGGESTIONS = (context, builder) -> {
		List<String> suggestions = suggestionIds(QuestRegistry.getAllQuests().keySet());
		suggestions.add("all");
		return SharedSuggestionProvider.suggest(suggestions, builder);
	};

	private static final SuggestionProvider<CommandSourceStack> QUEST_OR_NONE_SUGGESTIONS = (context, builder) -> {
		List<String> suggestions = suggestionIds(QuestRegistry.getAllQuests().keySet());
		suggestions.add("none");
		return SharedSuggestionProvider.suggest(suggestions, builder);
	};

	private static final SuggestionProvider<CommandSourceStack> SAGA_OR_ALL_SUGGESTIONS = (context, builder) -> {
		List<String> suggestions = suggestionIds(QuestRegistry.getAllSagas().keySet());
		suggestions.add("all");
		return SharedSuggestionProvider.suggest(suggestions, builder);
	};

	private static final SuggestionProvider<CommandSourceStack> DIFFICULTY_SUGGESTIONS = (context, builder) ->
			SharedSuggestionProvider.suggest(Arrays.stream(Difficulty.values()).map(d -> d.name().toLowerCase(Locale.ROOT)), builder);

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzquest")
				.requires(source -> DMZPermissions.check(source, DMZPermissions.QUEST_LIST_SELF, DMZPermissions.QUEST_LIST_OTHERS))

				// list [player]
				.then(Commands.literal("list")
						.executes(context -> listQuests(context, null))
						.then(Commands.argument("player", EntityArgument.player())
								.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.QUEST_LIST_OTHERS))
								.executes(context -> listQuests(context, EntityArgument.getPlayer(context, "player")))))

				// info <quest>
				.then(Commands.literal("info")
						.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.QUEST_INFO))
						.then(Commands.argument("quest", StringArgumentType.string())
								.suggests(QUEST_SUGGESTIONS)
								.executes(context -> questInfo(context))))

				// start <quest|all> [player]
				.then(Commands.literal("start")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.QUEST_START_SELF, DMZPermissions.QUEST_START_OTHERS))
						.then(Commands.argument("quest", StringArgumentType.string())
								.suggests(QUEST_OR_ALL_SUGGESTIONS)
								.executes(context -> startQuest(context, null))
								.then(Commands.argument("player", EntityArgument.player())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.QUEST_START_OTHERS))
										.executes(context -> startQuest(context, EntityArgument.getPlayer(context, "player"))))))

				// finish <quest|all> [player]
				.then(Commands.literal("finish")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.QUEST_FINISH_SELF, DMZPermissions.QUEST_FINISH_OTHERS))
						.then(Commands.argument("quest", StringArgumentType.string())
								.suggests(QUEST_OR_ALL_SUGGESTIONS)
								.executes(context -> finishQuest(context, null))
								.then(Commands.argument("player", EntityArgument.player())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.QUEST_FINISH_OTHERS))
										.executes(context -> finishQuest(context, EntityArgument.getPlayer(context, "player"))))))

				// fail <quest|all> [player]
				.then(Commands.literal("fail")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.QUEST_FAIL_SELF, DMZPermissions.QUEST_FAIL_OTHERS))
						.then(Commands.argument("quest", StringArgumentType.string())
								.suggests(QUEST_OR_ALL_SUGGESTIONS)
								.executes(context -> failQuest(context, null))
								.then(Commands.argument("player", EntityArgument.player())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.QUEST_FAIL_OTHERS))
										.executes(context -> failQuest(context, EntityArgument.getPlayer(context, "player"))))))

				// reset <quest|all> [player]
				.then(Commands.literal("reset")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.QUEST_RESET_SELF, DMZPermissions.QUEST_RESET_OTHERS))
						.then(Commands.argument("quest", StringArgumentType.string())
								.suggests(QUEST_OR_ALL_SUGGESTIONS)
								.executes(context -> resetQuest(context, null))
								.then(Commands.argument("player", EntityArgument.player())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.QUEST_RESET_OTHERS))
										.executes(context -> resetQuest(context, EntityArgument.getPlayer(context, "player"))))))

				// track <quest|none> [player]
				.then(Commands.literal("track")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.QUEST_TRACK_SELF, DMZPermissions.QUEST_TRACK_OTHERS))
						.then(Commands.argument("quest", StringArgumentType.string())
								.suggests(QUEST_OR_NONE_SUGGESTIONS)
								.executes(context -> trackQuest(context, null))
								.then(Commands.argument("player", EntityArgument.player())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.QUEST_TRACK_OTHERS))
										.executes(context -> trackQuest(context, EntityArgument.getPlayer(context, "player"))))))

				// startsaga <saga|all> [player]
				.then(Commands.literal("startsaga")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.QUEST_STARTSAGA_SELF, DMZPermissions.QUEST_STARTSAGA_OTHERS))
						.then(Commands.argument("saga", StringArgumentType.string())
								.suggests(SAGA_OR_ALL_SUGGESTIONS)
								.executes(context -> startSaga(context, null))
								.then(Commands.argument("player", EntityArgument.player())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.QUEST_STARTSAGA_OTHERS))
										.executes(context -> startSaga(context, EntityArgument.getPlayer(context, "player"))))))

				// finishsaga <saga|all> [player]
				.then(Commands.literal("finishsaga")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.QUEST_FINISHSAGA_SELF, DMZPermissions.QUEST_FINISHSAGA_OTHERS))
						.then(Commands.argument("saga", StringArgumentType.string())
								.suggests(SAGA_OR_ALL_SUGGESTIONS)
								.executes(context -> finishSaga(context, null))
								.then(Commands.argument("player", EntityArgument.player())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.QUEST_FINISHSAGA_OTHERS))
										.executes(context -> finishSaga(context, EntityArgument.getPlayer(context, "player"))))))

				// resetsaga <saga|all> [player]
				.then(Commands.literal("resetsaga")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.QUEST_RESETSAGA_SELF, DMZPermissions.QUEST_RESETSAGA_OTHERS))
						.then(Commands.argument("saga", StringArgumentType.string())
								.suggests(SAGA_OR_ALL_SUGGESTIONS)
								.executes(context -> resetSaga(context, null))
								.then(Commands.argument("player", EntityArgument.player())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.QUEST_RESETSAGA_OTHERS))
										.executes(context -> resetSaga(context, EntityArgument.getPlayer(context, "player"))))))

				// difficulty [easy|normal|hard] [targets]
				.then(Commands.literal("difficulty")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.QUEST_DIFFICULTY_SELF, DMZPermissions.QUEST_DIFFICULTY_OTHERS))
						.executes(context -> showDifficulty(context, List.of(context.getSource().getPlayerOrException())))
						.then(Commands.argument("difficulty", StringArgumentType.word())
								.suggests(DIFFICULTY_SUGGESTIONS)
								.executes(context -> setDifficulty(context, List.of(context.getSource().getPlayerOrException())))
								.then(Commands.argument("targets", EntityArgument.players())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.QUEST_DIFFICULTY_OTHERS))
										.executes(context -> setDifficulty(context, EntityArgument.getPlayers(context, "targets"))))))

				// questnpc spawn/list/remove
				.then(Commands.literal("questnpc")
						.requires(source -> source.hasPermission(2))
						.then(Commands.literal("spawn")
								.then(Commands.argument("npcId", StringArgumentType.string())
										.executes(context -> spawnQuestNPC(context, null, null))
										.then(Commands.argument("model", StringArgumentType.string())
												.executes(context -> spawnQuestNPC(context, StringArgumentType.getString(context, "model"), null))
												.then(Commands.argument("texture", StringArgumentType.string())
														.executes(context -> spawnQuestNPC(context, StringArgumentType.getString(context, "model"),
																StringArgumentType.getString(context, "texture")))))))
						.then(Commands.literal("list")
								.executes(StoryCommand::listNearbyQuestNPCs))
						.then(Commands.literal("remove")
								.executes(StoryCommand::removeNearestQuestNPC)))
		);
	}

	private static String normalizeId(String input) {
		if (input == null) return null;
		return input.replace('.', ':');
	}

	private static List<String> suggestionIds(Set<String> realIds) {
		List<String> suggestions = new ArrayList<>();
		for (String id : realIds) {
			suggestions.add(id.replace(':', '.'));
		}
		return suggestions;
	}

	// ============================================================
	// list
	// ============================================================

	private static int listQuests(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
		try {
			ServerPlayer player = targetPlayer != null ? targetPlayer : context.getSource().getPlayerOrException();

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				PlayerQuestData pqd = data.getPlayerQuestData();
				List<Map.Entry<String, Quest>> quests = new ArrayList<>(QuestRegistry.getAllQuests().entrySet());
				quests.sort(Map.Entry.comparingByKey(Comparator.naturalOrder()));

				if (quests.isEmpty()) {
					context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.04dff0a8a7"));
					return;
				}

				context.getSource().sendSystemMessage(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.d687730431" , player.getGameProfile().getName()));
				for (Map.Entry<String, Quest> entry : quests) {
					String questKey = entry.getKey();
					Quest quest = entry.getValue();
					Component status = formatQuestStatus(pqd, questKey);
					Component title = Component.translatable(quest.getTitle());
					context.getSource().sendSystemMessage(
							status.copy()
									.append(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.36a9e7f1c9"))
									.append(title)
									.append(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.0f22835e53" , questKey))
					);

					if (pqd.isQuestAccepted(questKey) && !pqd.isQuestCompleted(questKey)) {
						for (int i = 0; i < quest.getObjectives().size(); i++) {
							QuestObjective objective = quest.getObjectives().get(i);
							int progress = pqd.getObjectiveProgress(questKey, i);
							int required = quest.getObjectiveRequired(pqd, questKey, i);
							String progressText = progress >= required ? "[DONE]" : "[" + progress + "/" + required + "]";
							context.getSource().sendSystemMessage(
									net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.17017438fe" , progressText).append(QuestTextFormatter.describeObjective(objective))
							);
						}
					}
				}
			});

			return 1;
		} catch (Exception e) {
			context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.848ee0fd69" , e.getMessage()));
			return 0;
		}
	}

	// ============================================================
	// info
	// ============================================================

	private static int questInfo(CommandContext<CommandSourceStack> context) {
		try {
			String questKey = normalizeId(StringArgumentType.getString(context, "quest"));
			Quest quest = QuestRegistry.getQuest(questKey);
			if (quest == null) {
				context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.d12cf931a6" , questKey));
				return 0;
			}

			QuestService.ResolvedQuest resolved = QuestService.resolveQuest(questKey);
			String sagaLabel = (resolved != null && resolved.saga() != null) ? resolved.saga().getId() : "none";

			context.getSource().sendSystemMessage(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.264b956fd4" , questKey));
			context.getSource().sendSystemMessage(
					net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.1f534b0657").append(Component.translatable(quest.getTitle())));
			context.getSource().sendSystemMessage(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.348efa5dd2" , quest.getType().name()));
			context.getSource().sendSystemMessage(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.4107ca5434" , sagaLabel));
			if (quest.getQuestGiver() != null && !quest.getQuestGiver().isBlank()) {
				context.getSource().sendSystemMessage(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.67ec3282c9" , quest.getQuestGiver()));
			}
			if (quest.getTurnIn() != null && !quest.getTurnIn().isBlank()) {
				context.getSource().sendSystemMessage(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.5df8c6f3ed" , quest.getTurnIn()));
			}
			context.getSource().sendSystemMessage(
					net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.d07d2d1cc2" , quest.getObjectives().size()));
			for (int i = 0; i < quest.getObjectives().size(); i++) {
				context.getSource().sendSystemMessage(
						net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.2a46a7684f" , i).append(
								QuestTextFormatter.describeObjective(quest.getObjectives().get(i))));
			}
			context.getSource().sendSystemMessage(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.367b64cfa5" , quest.getRewards().size()));
			context.getSource().sendSystemMessage(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.32ca81e04e" , quest.isSecret()));
			context.getSource().sendSystemMessage(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.d2c36bc9a9" , quest.getClaimMode().name()));
			if (quest.hasPrerequisites()) {
				context.getSource().sendSystemMessage(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.097fd2f54f"));
			}
			if (quest.hasStartRequirements()) {
				context.getSource().sendSystemMessage(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.7bd96fea42"));
			}

			return 1;
		} catch (Exception e) {
			context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.f385746059" , e.getMessage()));
			return 0;
		}
	}

	// ============================================================
	// start
	// ============================================================

	private static int startQuest(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		try {
			String questKey = normalizeId(StringArgumentType.getString(context, "quest"));
			List<ServerPlayer> targets = getTargetPlayers(context, targetPlayer);
			if (targets.isEmpty()) {
				context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.914969e0d1"));
				return 0;
			}

			List<Map.Entry<String, Quest>> questsToStart = new ArrayList<>();
			if ("all".equalsIgnoreCase(questKey)) {
				questsToStart.addAll(QuestRegistry.getAllQuests().entrySet());
			} else {
				Quest quest = QuestRegistry.getQuest(questKey);
				if (quest == null) {
					context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.d12cf931a6" , questKey));
					return 0;
				}
				questsToStart.add(Map.entry(questKey, quest));
			}

			int successCount = 0;
			for (ServerPlayer player : targets) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
					PlayerQuestData pqd = stats.getPlayerQuestData();
					int partySize = PartyManager.getAllPartyMembers(player).size();
					for (Map.Entry<String, Quest> entry : questsToStart) {
						String key = entry.getKey();
						Quest quest = entry.getValue();
						if (pqd.isQuestAccepted(key) || pqd.isQuestCompleted(key)) continue;
						pqd.acceptQuest(key);
						quest.initializeObjectiveRequirements(pqd, key, partySize);
					}
					QuestService.syncQuestState(player);
				});
				successCount++;
			}

			int affected = successCount;
			context.getSource().sendSuccess(() ->
					net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.57ce2b0506" , ("all".equalsIgnoreCase(questKey) ? "Started all quests" : "Started quest " + questKey), affected), log);
			return successCount;
		} catch (Exception e) {
			context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.32617202ab" , e.getMessage()));
			return 0;
		}
	}

	// ============================================================
	// finish
	// ============================================================

	private static int finishQuest(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		try {
			String questKey = normalizeId(StringArgumentType.getString(context, "quest"));
			List<ServerPlayer> targets = getTargetPlayers(context, targetPlayer);
			if (targets.isEmpty()) {
				context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.914969e0d1"));
				return 0;
			}

			List<Map.Entry<String, Quest>> questsToFinish = new ArrayList<>();
			if ("all".equalsIgnoreCase(questKey)) {
				questsToFinish.addAll(QuestRegistry.getAllQuests().entrySet());
			} else {
				Quest quest = QuestRegistry.getQuest(questKey);
				if (quest == null) {
					context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.d12cf931a6" , questKey));
					return 0;
				}
				questsToFinish.add(Map.entry(questKey, quest));
			}

			int successCount = 0;
			for (ServerPlayer player : targets) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
					PlayerQuestData pqd = stats.getPlayerQuestData();
					for (Map.Entry<String, Quest> entry : questsToFinish) {
						completeQuest(pqd, entry.getKey(), entry.getValue(), player);
					}
					QuestService.syncQuestState(player);
				});
				successCount++;
			}

			int affected = successCount;
			context.getSource().sendSuccess(() ->
					net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.57ce2b0506" , ("all".equalsIgnoreCase(questKey) ? "Finished all quests" : "Finished quest " + questKey), affected), log);
			return successCount;
		} catch (Exception e) {
			context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.b2088d9ee8" , e.getMessage()));
			return 0;
		}
	}

	// ============================================================
	// fail
	// ============================================================

	private static int failQuest(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		try {
			String questKey = normalizeId(StringArgumentType.getString(context, "quest"));
			List<ServerPlayer> targets = getTargetPlayers(context, targetPlayer);
			if (targets.isEmpty()) {
				context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.914969e0d1"));
				return 0;
			}

			if (!"all".equalsIgnoreCase(questKey) && QuestRegistry.getQuest(questKey) == null) {
				context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.d12cf931a6" , questKey));
				return 0;
			}

			int successCount = 0;
			for (ServerPlayer player : targets) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
					PlayerQuestData pqd = stats.getPlayerQuestData();
					if ("all".equalsIgnoreCase(questKey)) {
						for (String key : new ArrayList<>(pqd.getAcceptedQuestIds())) {
							Quest quest = QuestRegistry.getQuest(key);
							if (quest != null && postForcedFailEvent(player, pqd, key, quest)) {
								pqd.failQuest(key);
								if (key.equals(pqd.getTrackedQuestId())) pqd.setTrackedQuestId(null);
							}
						}
					} else {
						Quest quest = QuestRegistry.getQuest(questKey);
						if (quest != null && pqd.isQuestAccepted(questKey)
								&& postForcedFailEvent(player, pqd, questKey, quest)) {
							pqd.failQuest(questKey);
							if (questKey.equals(pqd.getTrackedQuestId())) pqd.setTrackedQuestId(null);
						}
					}
					QuestService.syncQuestState(player);
				});
				successCount++;
			}

			int affected = successCount;
			context.getSource().sendSuccess(() ->
					net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.57ce2b0506" , ("all".equalsIgnoreCase(questKey) ? "Failed all active quests" : "Failed quest " + questKey), affected), log);
			return successCount;
		} catch (Exception e) {
			context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.93ba1c2358" , e.getMessage()));
			return 0;
		}
	}

	// ============================================================
	// reset
	// ============================================================

	private static int resetQuest(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		try {
			String questKey = normalizeId(StringArgumentType.getString(context, "quest"));
			List<ServerPlayer> targets = getTargetPlayers(context, targetPlayer);
			if (targets.isEmpty()) {
				context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.914969e0d1"));
				return 0;
			}

			if (!"all".equalsIgnoreCase(questKey) && QuestRegistry.getQuest(questKey) == null) {
				context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.d12cf931a6" , questKey));
				return 0;
			}

			int successCount = 0;
			for (ServerPlayer player : targets) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
					PlayerQuestData pqd = stats.getPlayerQuestData();
					if ("all".equalsIgnoreCase(questKey)) {
						for (String acceptedQuestKey : pqd.getAcceptedQuestIds()) {
							Quest acceptedQuest = QuestRegistry.getQuest(acceptedQuestKey);
							if (acceptedQuest != null) {
								postForcedResetFailEvent(player, pqd, acceptedQuestKey, acceptedQuest);
							}
						}
						pqd.resetAll();
					} else {
						Quest quest = QuestRegistry.getQuest(questKey);
						if (quest != null && postForcedResetFailEvent(player, pqd, questKey, quest)) {
							resetQuestEntry(pqd, questKey);
						}
					}
					QuestService.syncQuestState(player);
				});
				successCount++;
			}

			int affected = successCount;
			context.getSource().sendSuccess(() ->
					net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.57ce2b0506" , ("all".equalsIgnoreCase(questKey) ? "Reset all quests" : "Reset quest " + questKey), affected), log);
			return successCount;
		} catch (Exception e) {
			context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.7847abf30f" , e.getMessage()));
			return 0;
		}
	}

	// ============================================================
	// track
	// ============================================================

	private static int trackQuest(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		try {
			String raw = StringArgumentType.getString(context, "quest");
			String questKey = "none".equalsIgnoreCase(raw) ? null : normalizeId(raw);
			List<ServerPlayer> targets = getTargetPlayers(context, targetPlayer);
			if (targets.isEmpty()) {
				context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.914969e0d1"));
				return 0;
			}

			if (questKey != null && QuestRegistry.getQuest(questKey) == null) {
				context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.d12cf931a6" , questKey));
				return 0;
			}

			int successCount = 0;
			for (ServerPlayer player : targets) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
					stats.getPlayerQuestData().setTrackedQuestId(questKey);
					QuestService.syncQuestState(player);
				});
				successCount++;
			}

			String label = questKey == null ? "none" : questKey;
			int affected = successCount;
			context.getSource().sendSuccess(() ->
					net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.21b9d2460f" , label, affected), log);
			return successCount;
		} catch (Exception e) {
			context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.5d458d1c98" , e.getMessage()));
			return 0;
		}
	}

	// ============================================================
	// startsaga
	// ============================================================

	private static int startSaga(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		try {
			String sagaId = normalizeId(StringArgumentType.getString(context, "saga"));
			List<ServerPlayer> targets = getTargetPlayers(context, targetPlayer);
			if (targets.isEmpty()) {
				context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.914969e0d1"));
				return 0;
			}

			if (!"all".equalsIgnoreCase(sagaId) && QuestRegistry.getSaga(sagaId) == null) {
				context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.922a3390dc" , sagaId));
				return 0;
			}

			int successCount = 0;
			for (ServerPlayer player : targets) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
					PlayerQuestData pqd = stats.getPlayerQuestData();
					int partySize = PartyManager.getAllPartyMembers(player).size();
					String prefix = "all".equalsIgnoreCase(sagaId) ? null : sagaId + ":";
					for (Map.Entry<String, Quest> entry : QuestRegistry.getAllQuests().entrySet()) {
						String key = entry.getKey();
						Quest quest = entry.getValue();
						if (!quest.isSagaQuest()) continue;
						if (prefix != null && !key.startsWith(prefix)) continue;
						if (pqd.isQuestAccepted(key) || pqd.isQuestCompleted(key)) continue;
						pqd.acceptQuest(key);
						quest.initializeObjectiveRequirements(pqd, key, partySize);
					}
					QuestService.syncQuestState(player);
				});
				successCount++;
			}

			int affected = successCount;
			context.getSource().sendSuccess(() ->
					net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.57ce2b0506" , ("all".equalsIgnoreCase(sagaId) ? "Started all sagas" : "Started saga " + sagaId), affected), log);
			return successCount;
		} catch (Exception e) {
			context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.bf808d7c21" , e.getMessage()));
			return 0;
		}
	}

	// ============================================================
	// finishsaga
	// ============================================================

	private static int finishSaga(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		try {
			String sagaId = normalizeId(StringArgumentType.getString(context, "saga"));
			List<ServerPlayer> targets = getTargetPlayers(context, targetPlayer);
			if (targets.isEmpty()) {
				context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.914969e0d1"));
				return 0;
			}

			if (!"all".equalsIgnoreCase(sagaId) && QuestRegistry.getSaga(sagaId) == null) {
				context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.922a3390dc" , sagaId));
				return 0;
			}

			int successCount = 0;
			for (ServerPlayer player : targets) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
					PlayerQuestData pqd = stats.getPlayerQuestData();
					String prefix = "all".equalsIgnoreCase(sagaId) ? null : sagaId + ":";
					for (Map.Entry<String, Quest> entry : QuestRegistry.getAllQuests().entrySet()) {
						String key = entry.getKey();
						Quest quest = entry.getValue();
						if (!quest.isSagaQuest()) continue;
						if (prefix != null && !key.startsWith(prefix)) continue;
						completeQuest(pqd, key, quest, player);
					}
					QuestService.syncQuestState(player);
				});
				successCount++;
			}

			int affected = successCount;
			context.getSource().sendSuccess(() ->
					net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.57ce2b0506" , ("all".equalsIgnoreCase(sagaId) ? "Finished all sagas" : "Finished saga " + sagaId), affected), log);
			return successCount;
		} catch (Exception e) {
			context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.96755eb4f3" , e.getMessage()));
			return 0;
		}
	}

	// ============================================================
	// resetsaga
	// ============================================================

	private static int resetSaga(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		try {
			String sagaId = normalizeId(StringArgumentType.getString(context, "saga"));
			List<ServerPlayer> targets = getTargetPlayers(context, targetPlayer);
			if (targets.isEmpty()) {
				context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.914969e0d1"));
				return 0;
			}

			if (!"all".equalsIgnoreCase(sagaId) && QuestRegistry.getSaga(sagaId) == null) {
				context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.922a3390dc" , sagaId));
				return 0;
			}

			int successCount = 0;
			for (ServerPlayer player : targets) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
					PlayerQuestData pqd = stats.getPlayerQuestData();
					if ("all".equalsIgnoreCase(sagaId)) {
						for (String acceptedQuestKey : pqd.getAcceptedQuestIds()) {
							Quest acceptedQuest = QuestRegistry.getQuest(acceptedQuestKey);
							if (acceptedQuest != null) {
								postForcedResetFailEvent(player, pqd, acceptedQuestKey, acceptedQuest);
							}
						}
						pqd.resetAll();
					} else {
						String prefix = sagaId + ":";
						for (String questKey : collectKnownQuestKeys(pqd)) {
							if (!questKey.startsWith(prefix)) continue;
							Quest quest = QuestRegistry.getQuest(questKey);
							if (quest == null || postForcedResetFailEvent(player, pqd, questKey, quest)) {
								resetQuestEntry(pqd, questKey);
							}
						}
					}
					QuestService.syncQuestState(player);
				});
				successCount++;
			}

			int affected = successCount;
			context.getSource().sendSuccess(() ->
					net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.57ce2b0506" , ("all".equalsIgnoreCase(sagaId) ? "Reset all sagas" : "Reset saga " + sagaId), affected), log);
			return successCount;
		} catch (Exception e) {
			context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.cb7d9a9bb1" , e.getMessage()));
			return 0;
		}
	}

	// ============================================================
	// difficulty
	// ============================================================

	private static int showDifficulty(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, PartyManager.resolveDifficultyOwner(player)).ifPresent(data ->
					context.getSource().sendSystemMessage(Component.translatable("command.dragonminez.quest.difficulty.current",
							player.getName(), difficultyName(data.getPlayerQuestData().getDifficulty()))));
		}
		return targets.size();
	}

	private static int setDifficulty(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		String raw = StringArgumentType.getString(context, "difficulty");
		Difficulty difficulty = parseDifficulty(raw);
		if (difficulty == null) {
			context.getSource().sendFailure(Component.translatable("command.dragonminez.quest.difficulty.unknown", raw));
			return 0;
		}

		Set<UUID> handledControllers = new HashSet<>();
		int changed = 0;
		for (ServerPlayer player : targets) {
			ServerPlayer controller = PartyManager.resolveDifficultyOwner(player);
			if (!handledControllers.add(controller.getUUID())) continue;
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, controller).orElse(null);
			if (data == null) continue;

			PlayerQuestData pqd = data.getPlayerQuestData();
			pqd.setDifficulty(difficulty);
			pqd.setDifficultyChosen(true);
			PartyManager.syncPartyDifficulty(controller);
			changed++;

			List<ServerPlayer> members = PartyManager.getAllPartyMembers(controller);
			for (ServerPlayer member : members) {
				member.sendSystemMessage(Component.translatable("command.dragonminez.quest.difficulty.notify", difficultyName(difficulty)));
			}
			if (members.size() > 1) {
				context.getSource().sendSystemMessage(Component.translatable("command.dragonminez.quest.difficulty.party_note",
						controller.getName(), members.size(), difficultyName(difficulty)));
			}
		}

		if (targets.size() == 1) {
			ServerPlayer target = targets.iterator().next();
			context.getSource().sendSuccess(() -> Component.translatable("command.dragonminez.quest.difficulty.set",
					difficultyName(difficulty), target.getName()), log);
		} else {
			int affected = targets.size();
			context.getSource().sendSuccess(() -> Component.translatable("command.dragonminez.quest.difficulty.set_multiple",
					difficultyName(difficulty), affected), log);
		}
		return changed;
	}

	private static Difficulty parseDifficulty(String raw) {
		if (raw == null) return null;
		for (Difficulty difficulty : Difficulty.values()) {
			if (difficulty.name().equalsIgnoreCase(raw.trim())) return difficulty;
		}
		return null;
	}

	private static Component difficultyName(Difficulty difficulty) {
		return Component.translatable("gui.dragonminez.quest_tree.difficulty." + difficulty.name().toLowerCase(Locale.ROOT));
	}

	// ============================================================
	// questnpc commands
	// ============================================================

	private static int spawnQuestNPC(CommandContext<CommandSourceStack> context, String modelOverride, String textureOverride) {
		try {
			String npcId = StringArgumentType.getString(context, "npcId").toLowerCase();
			ServerPlayer player = context.getSource().getPlayerOrException();

			QuestNPCEntity npc = MainEntities.QUEST_NPC.get().create(player.level());
			if (npc == null) {
				context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.ef2e31013a"));
				return 0;
			}

			npc.setNpcId(npcId.toLowerCase());
			if (modelOverride != null && !modelOverride.isEmpty()) {
				npc.setNpcModel(modelOverride.toLowerCase());
			}
			if (textureOverride != null && !textureOverride.isEmpty()) {
				npc.setNpcTexture(textureOverride.toLowerCase());
			}

			// Make command-spawned quest NPCs durable so they don't vanish.
			// setPersistenceRequired() is also set in the constructor, but we re-assert it here so the
			// NPC is guaranteed to survive chunk unloads, the mob cap, and world reloads (it is saved to
			// disk and excluded from despawn). setInvulnerable(true) blocks foe/environmental damage —
			// fall, fire, explosions, GENERIC, mob attacks — that could otherwise destroy the NPC. The
			// out-of-world and /kill damage types still bypass invulnerability, so admins keep control
			// (and the 'questnpc remove' command uses discard(), which is unaffected).
			npc.setPersistenceRequired();
			npc.setInvulnerable(true);
			npc.setHealth(npc.getMaxHealth());

			npc.setPos(player.getX(), player.getY(), player.getZ());
			// Anchor the NPC to its spawn spot so it returns here if it ever gets shoved away
			// (Y is ignored, so changes to the blocks beneath it won't fight the anchor).
			npc.setHomePosition(player.getX(), player.getZ());
			player.serverLevel().addFreshEntity(npc);

			context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.727370d795" , npcId), true);
			return 1;
		} catch (Exception e) {
			context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.052434bb2e" , e.getMessage()));
			return 0;
		}
	}

	private static int listNearbyQuestNPCs(CommandContext<CommandSourceStack> context) {
		try {
			ServerPlayer player = context.getSource().getPlayerOrException();
			AABB searchBox = player.getBoundingBox().inflate(NPC_SEARCH_RADIUS);
			List<QuestNPCEntity> npcs = player.serverLevel().getEntitiesOfClass(QuestNPCEntity.class, searchBox);

			if (npcs.isEmpty()) {
				context.getSource().sendSystemMessage(
						net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.b634e77c12" , NPC_SEARCH_RADIUS));
				return 0;
			}

			context.getSource().sendSystemMessage(
					net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.14f228f36d" , NPC_SEARCH_RADIUS, npcs.size()));
			npcs.sort(Comparator.comparingDouble(npc -> npc.distanceToSqr(player)));
			for (QuestNPCEntity npc : npcs) {
				int dist = (int) npc.distanceTo(player);
				context.getSource().sendSystemMessage(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.1e14e51cd7" , npc.getNpcId(), (int) npc.getX(), (int) npc.getY(), (int) npc.getZ(), dist));
			}
			return npcs.size();
		} catch (Exception e) {
			context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.e12ebc85d2" , e.getMessage()));
			return 0;
		}
	}

	private static int removeNearestQuestNPC(CommandContext<CommandSourceStack> context) {
		try {
			ServerPlayer player = context.getSource().getPlayerOrException();
			AABB searchBox = player.getBoundingBox().inflate(NPC_SEARCH_RADIUS);
			List<QuestNPCEntity> npcs = player.serverLevel().getEntitiesOfClass(QuestNPCEntity.class, searchBox);

			if (npcs.isEmpty()) {
				context.getSource().sendFailure(
						net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.b634e77c12" , NPC_SEARCH_RADIUS));
				return 0;
			}

			QuestNPCEntity nearest = npcs.stream()
					.min(Comparator.comparingDouble(npc -> npc.distanceToSqr(player)))
					.orElse(null);
			if (nearest == null) return 0;

			String npcId = nearest.getNpcId();
			nearest.discard();
			context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.ed5302aed8" , npcId), true);
			return 1;
		} catch (Exception e) {
			context.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.247181550a" , e.getMessage()));
			return 0;
		}
	}

	// ============================================================
	// Shared helpers
	// ============================================================

	private static Component formatQuestStatus(PlayerQuestData pqd, String questKey) {
		if (pqd.isQuestCompleted(questKey)) return net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.c3565869ed");
		if (pqd.isQuestAccepted(questKey)) return net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.77fd24765d");
		if (pqd.getQuestStatus(questKey) == PlayerQuestData.QuestStatus.FAILED) return net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.2e06265b07");
		return net.minecraft.network.chat.Component.translatable("text.dragonminez.storycommand.97b629f8aa");
	}

	private static void completeQuest(PlayerQuestData pqd, String questKey, Quest quest, ServerPlayer player) {
		if (!pqd.isQuestAccepted(questKey)) {
			pqd.acceptQuest(questKey);
		}

		quest.initializeObjectiveRequirements(pqd, questKey, PartyManager.getAllPartyMembers(player).size());
		for (int i = 0; i < quest.getObjectives().size(); i++) {
			pqd.setObjectiveProgress(questKey, i, quest.getObjectiveRequired(pqd, questKey, i));
		}

		QuestService.ResolvedQuest resolved = QuestService.resolveQuest(questKey);
		Saga saga = resolved != null ? resolved.saga() : null;
		DMZEvent.QuestCompletedEvent completeEvent = new DMZEvent.QuestCompletedEvent(
				player, questKey, saga, quest, PartyManager.getAllPartyMembers(player));
		NeoForge.EVENT_BUS.post(completeEvent);

		pqd.completeQuest(questKey);
		if (questKey.equals(pqd.getTrackedQuestId())) {
			pqd.setTrackedQuestId(null);
		}
	}

	/** Posts a fail event for an in-progress quest and returns true if not cancelled (i.e. the fail should proceed). */
	private static boolean postForcedFailEvent(ServerPlayer player, PlayerQuestData pqd, String questKey, Quest quest) {
		if (!pqd.isQuestAccepted(questKey)) return false;
		QuestService.ResolvedQuest resolved = QuestService.resolveQuest(questKey);
		DMZEvent.QuestFailEvent failEvent = new DMZEvent.QuestFailEvent(
				player, questKey, resolved != null ? resolved.saga() : null,
				quest, PartyManager.getAllPartyMembers(player),
				DMZEvent.QuestFailEvent.FailureReason.FORCED_RESET);
		return !NeoForge.EVENT_BUS.post(failEvent).isCanceled();
	}

	/** Posts a fail event as part of a full data reset; returns true when the quest entry can be wiped. */
	private static boolean postForcedResetFailEvent(ServerPlayer player, PlayerQuestData pqd, String questKey, Quest quest) {
		if (!pqd.isQuestAccepted(questKey) || pqd.isQuestCompleted(questKey)) return true;
		QuestService.ResolvedQuest resolved = QuestService.resolveQuest(questKey);
		DMZEvent.QuestFailEvent failEvent = new DMZEvent.QuestFailEvent(
				player, questKey, resolved != null ? resolved.saga() : null,
				quest, PartyManager.getAllPartyMembers(player),
				DMZEvent.QuestFailEvent.FailureReason.FORCED_RESET);
		return !NeoForge.EVENT_BUS.post(failEvent).isCanceled();
	}

	private static Set<String> collectKnownQuestKeys(PlayerQuestData pqd) {
		Set<String> questKeys = new LinkedHashSet<>();
		questKeys.addAll(pqd.getAcceptedQuestIds());
		questKeys.addAll(pqd.getCompletedQuestIds());
		questKeys.addAll(pqd.getFailedQuestIds());
		return questKeys;
	}

	private static void resetQuestEntry(PlayerQuestData pqd, String questKey) {
		pqd.resetQuest(questKey);
		if (questKey.equals(pqd.getTrackedQuestId())) {
			pqd.setTrackedQuestId(null);
		}
	}

	private static List<ServerPlayer> getTargetPlayers(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
		List<ServerPlayer> players = new ArrayList<>();
		if (targetPlayer != null) {
			players.add(targetPlayer);
			return players;
		}
		try {
			players.add(context.getSource().getPlayerOrException());
		} catch (Exception ignored) {
		}
		return players;
	}
}
