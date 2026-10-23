package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.systems.kisense.CombatIndicators;
import com.dragonminez.client.systems.kisense.KiSenseScan;
import com.dragonminez.client.systems.kisense.KiSenseState;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix4f;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class KiSenseEvent {

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null || mc.level == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) {
				KiSenseScan.clear();
				CombatIndicators.clear();
				return;
			}
			if (KiSenseState.isActive() && (!data.getSkills().isSkillActive("kisense") || data.getSkills().getSkillLevel("kisense") <= 0)) {
				KiSenseState.reset();
			}
			if (KiSenseState.isActive() && KiSenseScan.hasScouter(player)) {
				KiSenseState.set(KiSenseState.Mode.NONE);
				return;
			}
			KiSenseScan.tick(player, data, KiSenseState.getMode());
			if (KiSenseState.isCombat()) CombatIndicators.tick();
			else CombatIndicators.clear();
		});
	}
}
