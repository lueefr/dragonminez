package com.dragonminez.server.world.raid;

import com.dragonminez.Reference;
import net.minecraft.server.level.ServerLevel;
import com.dragonminez.platform.event.tick.LevelTickEvent;
import com.dragonminez.platform.event.SubscribeEvent;
import com.dragonminez.platform.event.EventBusSubscriber;

@EventBusSubscriber(modid = Reference.MOD_ID)
public class RaidEvents {

	@SubscribeEvent
	public static void onLevelTick(LevelTickEvent.Post event) {
		if (event.getLevel().isClientSide()) return;
		if (event.getLevel() instanceof ServerLevel level) {
			RaidManager.tick(level);
		}
	}
}
