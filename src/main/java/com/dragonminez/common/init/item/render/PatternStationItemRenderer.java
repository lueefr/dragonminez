package com.dragonminez.common.init.item.render;

import com.dragonminez.common.init.item.PatternStationItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.renderer.GeoItemRenderer;

public class PatternStationItemRenderer extends GeoItemRenderer<PatternStationItem> {

	public PatternStationItemRenderer() {
		super(new PatternStationItemModel());
	}

	@Override
	public void preRender(PoseStack poseStack, PatternStationItem animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer,
						  boolean isReRender, float partialTick, int packedLight, int packedOverlay, int renderColor) {
        float red = net.minecraft.util.FastColor.ARGB32.red(renderColor) / 255.0F;
        float green = net.minecraft.util.FastColor.ARGB32.green(renderColor) / 255.0F;
        float blue = net.minecraft.util.FastColor.ARGB32.blue(renderColor) / 255.0F;
        float alpha = net.minecraft.util.FastColor.ARGB32.alpha(renderColor) / 255.0F;
		super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, com.dragonminez.client.render.util.RenderColors.pack(red, green, blue, alpha));

		if (!isReRender)
			poseStack.translate(0, -0.51f, 0);
	}
}
