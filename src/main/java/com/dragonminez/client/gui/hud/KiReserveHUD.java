package com.dragonminez.client.gui.hud;

import com.dragonminez.client.gui.hud.layout.HudElement;
import com.dragonminez.client.gui.hud.layout.HudLayout;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.client.gui.LayeredDraw;

public class KiReserveHUD {
	private static final HudBar RESERVE_BAR = new HudBar(HudStatNumberAnimator.StatKind.KI).flowing();
	private static final HudSmoother VISIBILITY = new HudSmoother(0.18f, 0.01f);
	private static final HudSmoother ACTIVE = new HudSmoother(0.15f);

	public static final LayeredDraw.Layer HUD_KI_RESERVE = (guiGraphics, deltaTracker) -> {
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(true);
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
		if (!HudLayout.isPreview()) render(guiGraphics, partialTicks, width, height);
	};

	public static void render(GuiGraphicsExtractor guiGraphics, float partialTicks, int width, int height) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getDebugOverlay().showDebugScreen() || mc.player == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;
			boolean applicable = "frostdemon".equalsIgnoreCase(data.getCharacter().getRaceName());

			float reserve = applicable ? Math.max(0.0f, data.getRacialData().getEnergyReserve()) : 0.0f;
			boolean feeding = applicable && data.getRacialData().isReserveActive();
			double ratio = ConfigManager.getServerConfig().getRacialSkills().getFrostdemon().getReserveMaxRatio();
			float maxReserve = Math.max(1.0f, (float) (data.getMaxEnergy() * ratio));

			RESERVE_BAR.update(Math.min(reserve, maxReserve), maxReserve);
			float visibility = HudSideMeters.update(HudElement.RESERVE, VISIBILITY, applicable, reserve > 0.0f || feeding, width, height);
			float active = ACTIVE.update(feeding ? 1.0f : 0.0f);

			float seconds = HudBar.time();
			float pulse = active * (0.5f + 0.5f * Mth.sin(seconds * 8.0f));
			int kiColor = HudPlayerState.of(mc.player, data).auraColor();
			int color = HudRender.mix(kiColor, 0xFFFFFF, active * 0.35f);

			HudSideMeters.draw(guiGraphics, HudElement.RESERVE, width, height, RESERVE_BAR, HudSprites.RESERVE_ICON, visibility, color, pulse);
		});
	}
}
