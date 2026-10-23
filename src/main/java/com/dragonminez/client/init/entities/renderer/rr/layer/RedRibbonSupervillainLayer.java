package com.dragonminez.client.init.entities.renderer.rr.layer;

import com.dragonminez.client.init.entities.renderer.sagas.layer.SagaSupervillainLayer;
import com.dragonminez.client.render.effects.AuraBorderRenderer;
import com.dragonminez.client.render.shader.TransformationMaskBufferSource;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.common.init.entities.redribbon.RedRibbonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.GeoRenderLayer;

public class RedRibbonSupervillainLayer<T extends RedRibbonEntity> extends GeoRenderLayer<T, Void, net.minecraft.client.renderer.entity.state.LivingEntityRenderState> {

	public RedRibbonSupervillainLayer(GeoRenderer<T, Void, net.minecraft.client.renderer.entity.state.LivingEntityRenderState> geoRenderer) {
		super(geoRenderer);
	}

	public static MultiBufferSource beginBorder(RedRibbonEntity entity, MultiBufferSource source, float partialTick) {
		boolean shown = entity.isSupervillain() && !entity.isInvisible();
		return AuraBorderRenderer.begin(entity, source, shown ? SagaSupervillainLayer.BORDER_COLOR : null, partialTick);
	}

	@Override
	public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType,
					   MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
					   int packedLight, int packedOverlay) {
		if (!animatable.isSupervillain() || animatable.isInvisible()) return;

		float[] tint = SagaSupervillainLayer.TINT;
		RenderType tinted = ModRenderTypes.skinOverlayTranslucent(getRenderer().getTextureLocation(animatable));
		TransformationMaskBufferSource maskBuffer = bufferSource instanceof TransformationMaskBufferSource mask ? mask : null;

		if (maskBuffer != null) maskBuffer.setMaskCaptureBlocked(true);
		try {
			getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, tinted, bufferSource.getBuffer(tinted), partialTick, packedLight, packedOverlay, com.dragonminez.client.render.util.RenderColors.pack(tint[0], tint[1], tint[2], SagaSupervillainLayer.TINT_ALPHA));
		} finally {
			if (maskBuffer != null) maskBuffer.setMaskCaptureBlocked(false);
		}
	}
}
