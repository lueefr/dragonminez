package com.dragonminez.client.init.entities.renderer.sagas.layer;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import com.geckolib.cache.GeckoLibCache;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.GeoRenderLayer;

public class SagaDragonBallLayer<T extends DBSagasEntity> extends GeoRenderLayer<T, Void, net.minecraft.client.renderer.entity.state.LivingEntityRenderState> {

	private static final Identifier NAMEK_BALL_MODEL = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/block/dballnamek.geo.json");
	private static final String ANCHOR_BONE = "dball";
	private static final String BALL_BONE = "esfera";
	private static final float BALL_SCALE = 0.6f;
	private static final float BALL_CENTER_Y = 5.625f / 16.0f;

	public SagaDragonBallLayer(GeoRenderer<T, Void, net.minecraft.client.renderer.entity.state.LivingEntityRenderState> geoRenderer) {
		super(geoRenderer);
	}

	@Override
	public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
							  MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
							  int packedLight, int packedOverlay) {
		if (!ANCHOR_BONE.equals(bone.getName())) return;
		int stars = animatable.getNamekDragonBallStars();
		if (stars < 1 || stars > 7) return;

		BakedGeoModel ballModel = GeckoLibCache.getBakedModels().get(NAMEK_BALL_MODEL);
		if (ballModel == null) return;
		GeoBone ballBone = ballModel.getBone(BALL_BONE).orElse(null);
		if (ballBone == null) return;

		Identifier texture = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/block/custom/" + animatable.getDragonBallTexturePrefix() + stars + ".png");
		RenderType ballType = net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(texture);

		poseStack.pushPose();
		poseStack.translate(bone.getPivotX() / 16.0f, bone.getPivotY() / 16.0f, bone.getPivotZ() / 16.0f);
		poseStack.scale(BALL_SCALE, BALL_SCALE, BALL_SCALE);
		poseStack.translate(0.0f, -BALL_CENTER_Y, 0.0f);
		getRenderer().renderRecursively(poseStack, animatable, ballBone, ballType, bufferSource, bufferSource.getBuffer(ballType), true, partialTick, packedLight, OverlayTexture.NO_OVERLAY, com.dragonminez.client.render.util.RenderColors.pack(1.0f, 1.0f, 1.0f, 1.0f));
		poseStack.popPose();

		bufferSource.getBuffer(renderType);
	}
}
