package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.diagnostics.JsonLoadReport;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.List;

@EventBusSubscriber(modid = Reference.MOD_ID)
public class DataReportEvents {

	private static final int MAX_LINES = 12;

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		if (JsonLoadReport.isEmpty()) return;
		if (!ConfigManager.getServerConfig().getDeveloper().isReportJsonProblemsInChat()) return;

		List<JsonLoadReport.Entry> entries = JsonLoadReport.entries();
		long errors = JsonLoadReport.count(JsonLoadReport.Kind.ERROR);
		long updates = JsonLoadReport.count(JsonLoadReport.Kind.UPDATE);

		player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("text.dragonminez.datareportevents.d6d74eb2bc" , errors, updates));

		int shown = 0;
		shown = appendLines(player, entries, JsonLoadReport.Kind.ERROR, "§c [!] §f", shown);
		shown = appendLines(player, entries, JsonLoadReport.Kind.UPDATE, "§e [~] §f", shown);
		if (entries.size() > shown) {
			player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("text.dragonminez.datareportevents.b664cc3f30" , (entries.size() - shown)));
		}
		player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("text.dragonminez.datareportevents.cf910ad0b5"));
	}

	private static int appendLines(ServerPlayer player, List<JsonLoadReport.Entry> entries,
			JsonLoadReport.Kind kind, String prefix, int shown) {
		for (JsonLoadReport.Entry entry : entries) {
			if (entry.kind() != kind) continue;
			if (shown >= MAX_LINES) break;
			player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("text.dragonminez.datareportevents.cb2a0af13a" , prefix + entry.file(), entry.message()));
			shown++;
		}
		return shown;
	}
}
