package com.dragonminez.client.render;

import net.neoforged.fml.common.EventBusSubscriber;
import com.dragonminez.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import com.dragonminez.client.render.util.IrisCompat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class CameraProjectionCapture {
	private static final Matrix4f VIEW_PROJECTION = new Matrix4f();
	private static Vec3 cameraPosition = Vec3.ZERO;
	private static long capturedFrame = -1L;
	private static long frameCounter;

	private CameraProjectionCapture() {}

	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
		if (IrisCompat.isRenderingShadowPass()) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		// AFTER_SKY is dispatched with a null PoseStack, which NeoForge replaces
		// with an identity stack. The event's model-view matrix is the camera view
		// used by LevelRenderer for the scene.
		VIEW_PROJECTION.set(event.getProjectionMatrix()).mul(event.getModelViewMatrix());
		cameraPosition = event.getCamera().getPosition();
		capturedFrame = ++frameCounter;
	}

	public static boolean isAvailable() {
		return capturedFrame >= 0L;
	}

	public static boolean project(Vec3 worldPosition, float screenWidth, float screenHeight, float[] out) {
		if (capturedFrame < 0L) return false;
		Vector4f clip = new Vector4f(
				(float) (worldPosition.x - cameraPosition.x),
				(float) (worldPosition.y - cameraPosition.y),
				(float) (worldPosition.z - cameraPosition.z), 1.0f);
		VIEW_PROJECTION.transform(clip);
		if (clip.w <= 1.0e-4f) return false;
		float ndcX = clip.x / clip.w;
		float ndcY = clip.y / clip.w;
		out[0] = (ndcX + 1.0f) * 0.5f * screenWidth;
		out[1] = (1.0f - ndcY) * 0.5f * screenHeight;
		out[2] = ndcX;
		out[3] = ndcY;
		return true;
	}
}
