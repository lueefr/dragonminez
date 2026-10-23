package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.server.util.MutantManager;
import com.dragonminez.server.world.data.MutantSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import com.dragonminez.platform.event.tick.ServerTickEvent;
import com.dragonminez.platform.event.SubscribeEvent;
import com.dragonminez.platform.event.EventBusSubscriber;
import com.dragonminez.platform.ServerLifecycleHooks;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class MutantEvents {

	private static final int CHECK_INTERVAL_TICKS = 100;

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {

		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) return;
		if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) return;

		GeneralServerConfig.MutantConfig cfg = ConfigManager.getServerConfig() != null
				? ConfigManager.getServerConfig().getMutant() : null;
		if (cfg == null || !cfg.getEnabled()) return;

		var overworld = server.getLevel(Level.OVERWORLD);
		if (overworld == null) return;

		MutantSavedData saved = MutantSavedData.get(server);
		long now = overworld.getGameTime();
		long intervalTicks = (long) cfg.getRollIntervalMinutes() * 60L * 20L;

		if (saved.getNextRollTick() < 0) {
			saved.setNextRollTick(now + intervalTicks);
			return;
		}

		if (now >= saved.getNextRollTick()) {
			MutantManager.runLottery(server);
			saved.setNextRollTick(now + intervalTicks);
		}
	}
}
