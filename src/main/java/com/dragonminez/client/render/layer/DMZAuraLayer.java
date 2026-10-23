package com.dragonminez.client.render.layer;

import com.dragonminez.client.render.effects.AuraFxState;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.GeoRenderLayer;

public class DMZAuraLayer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoRenderLayer<T, Void, net.minecraft.client.renderer.entity.state.LivingEntityRenderState> {
	public DMZAuraLayer(GeoRenderer<T, Void, net.minecraft.client.renderer.entity.state.LivingEntityRenderState> entityRendererIn) {
		super(entityRendererIn);
	}

	@Override
	public void render(PoseStack poseStack, T animatable, BakedGeoModel playerModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
		if (animatable.isSpectator()) return;
		if (com.dragonminez.client.render.effects.AfterimageEffect.isRendering()) return;
		var stats = StatsProvider.get(StatsCapability.INSTANCE, animatable).orElse(null);
		if (stats == null) return;

		boolean isAuraActive = stats.getStatus().isAuraActive() || stats.getStatus().isPermanentAura() || stats.getStatus().isForcedAura();
		boolean isAndroidChargingForm = stats.getStatus().isAndroidUpgraded() && stats.getStatus().isActionCharging() && stats.getStatus().getSelectedAction() == ActionMode.FORM;

		boolean hasLightning = AuraFxState.hasLightning(stats);

		if (stats.getStatus().isAndroidUpgraded() && !isAndroidChargingForm && !hasLightning) return;

		if (isAuraActive && !isAndroidChargingForm) PlayerEffectQueue.addAura(animatable, playerModel, poseStack, partialTick, packedLight);

		if (isAuraActive || hasLightning) {
			PlayerEffectQueue.addSpark(animatable, playerModel, poseStack, partialTick, packedLight);
		}
	}
}
