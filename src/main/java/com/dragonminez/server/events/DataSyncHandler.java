package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.server.storage.StorageManager;
import net.minecraft.server.level.ServerPlayer;
import com.dragonminez.platform.event.player.PlayerEvent;
import com.dragonminez.platform.event.SubscribeEvent;
import com.dragonminez.platform.event.EventBusSubscriber;

@EventBusSubscriber(modid = Reference.MOD_ID)
public class DataSyncHandler {

	@SubscribeEvent(priority = com.dragonminez.platform.event.EventPriority.HIGHEST)
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity().level().isClientSide()) return;
		if (event.getEntity() instanceof ServerPlayer player) {
			StorageManager.loadPlayer(player);
			com.dragonminez.server.world.worldboss.WorldBossManager.syncTo(player);
		}
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity().level().isClientSide()) return;
		if (event.getEntity() instanceof ServerPlayer player) {
			StorageManager.savePlayer(player);
		}
	}
}
