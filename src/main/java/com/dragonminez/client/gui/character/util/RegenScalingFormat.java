package com.dragonminez.client.gui.character.util;

import com.dragonminez.common.config.RaceStatsConfig;

import java.util.Locale;

public final class RegenScalingFormat {
	private RegenScalingFormat() {
	}

	public static String stamina(RaceStatsConfig.ClassStats classStats) {
		double perRes = classStats.resolveSp5ResScaling() * 0.2;
		double perVit = classStats.resolveSp5VitScaling() * 0.2;
		StringBuilder sb = new StringBuilder(" (");
		if (perRes > 0) sb.append(net.minecraft.network.chat.Component.translatable("text.dragonminez.format.0e458c4f9b", String.format(Locale.ROOT, "%.3f", perRes)).getString());
		if (perVit > 0) {
			if (perRes > 0) sb.append(net.minecraft.network.chat.Component.translatable("text.dragonminez.fragment.0a07f65946").getString());
			sb.append(net.minecraft.network.chat.Component.translatable("text.dragonminez.format.d654a2c132", String.format(Locale.ROOT, "%.3f", perVit)).getString());
		}
		if (perRes <= 0 && perVit <= 0) return "";
		return sb.append(')').toString();
	}
}
