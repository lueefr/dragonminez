package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.ClientGravityState;
import com.dragonminez.client.render.shader.GravityRedManager;
import com.dragonminez.client.render.util.IrisCompat;
import net.minecraft.client.Minecraft;
import net.fabricmc.api.EnvType;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import com.dragonminez.platform.event.EventPriority;
import com.dragonminez.platform.event.SubscribeEvent;
import com.dragonminez.platform.event.EventBusSubscriber;

@EventBusSubscriber(modid = Reference.MOD_ID, value = EnvType.CLIENT)
public final class GravityRedRenderer {

	private GravityRedRenderer() {}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;

		boolean iris = IrisCompat.isShaderPackInUse(mc.level.getGameTime());
		RenderLevelStageEvent.Stage targetStage = iris ? RenderLevelStageEvent.Stage.AFTER_LEVEL : RenderLevelStageEvent.Stage.AFTER_WEATHER;
		if (event.getStage() != targetStage) return;

		float intensity = ClientGravityState.getShaderIntensity();
		if (intensity <= 0.0f) {
			GravityRedManager.reset();
			return;
		}

		if (iris) {
			mc.getMainRenderTarget().bindWrite(false);
			GravityRedManager.process(event.getPartialTick().getGameTimeDeltaPartialTick(false), intensity, false);
		} else {
			GravityRedManager.process(event.getPartialTick().getGameTimeDeltaPartialTick(false), intensity);
		}
	}
}
