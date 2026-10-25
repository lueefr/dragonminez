package com.dragonminez.common.worldboss;

import com.dragonminez.client.util.NumberFormattingUtil;
import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.quest.rewards.AttributesReward;
import com.dragonminez.common.quest.rewards.ItemReward;
import com.dragonminez.common.quest.rewards.KiTechniqueReward;
import com.dragonminez.common.quest.rewards.SkillReward;
import com.dragonminez.common.quest.rewards.TPSReward;
import com.dragonminez.common.quest.rewards.TransformationReward;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class WorldBossRewardText {

	private WorldBossRewardText() {}

	public static MutableComponent name(QuestReward reward) { return name(reward, null); }

    public static MutableComponent name(QuestReward reward, com.dragonminez.common.stats.StatsData data) {
		if (reward instanceof ItemReward item) {
			return item.itemName();
		}
		if (reward instanceof TPSReward tps) {
			return Component.translatable("worldboss.dragonminez.results.tps", NumberFormattingUtil.formatLargeNumber(tps.getAmount()));
		}
		if (reward instanceof AttributesReward attributes) {
			return Component.translatable("worldboss.dragonminez.results.attributes", attributes.getAmount());
		}
		if (reward instanceof SkillReward skill) {
			return skill.displayName();
		}
		if (reward instanceof KiTechniqueReward technique) {
			String techniqueName = technique.getTemplate().getName();
			return techniqueName != null && techniqueName.contains(".") ? Component.translatable(techniqueName) : com.dragonminez.api.lang.ConfigTranslations.text(techniqueName == null ? "" : techniqueName);
		}
		if (reward instanceof TransformationReward) {
			return reward.getDescription(1.0, data).copy();
		}
		return reward.getDescription(1.0, data).copy();
	}

	public static MutableComponent describe(QuestReward reward, float amount) { return describe(reward, amount, null); }

    public static MutableComponent describe(QuestReward reward, float amount, com.dragonminez.common.stats.StatsData data) {
		if (reward instanceof ItemReward item) {
			return Component.translatable("worldboss.dragonminez.results.item", Math.max(1, Math.round(amount)), item.itemName());
		}
		if (reward instanceof TPSReward tps) {
			double shown = amount > 0.0f ? amount : tps.getAmount();
			return Component.translatable("worldboss.dragonminez.results.tps", NumberFormattingUtil.formatLargeNumber(shown));
		}
		if (reward instanceof AttributesReward && amount > 0.0f) {
			return Component.translatable("worldboss.dragonminez.results.attributes", Math.round(amount));
		}
		return name(reward, data);
	}
}
