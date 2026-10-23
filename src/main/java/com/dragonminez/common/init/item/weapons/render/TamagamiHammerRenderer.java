package com.dragonminez.common.init.item.weapons.render;

import com.dragonminez.common.init.item.weapons.TamagamiHammerItem;
import com.dragonminez.common.init.item.weapons.model.TamagamiHammerModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.cache.object.BakedGeoModel;
import com.geckolib.renderer.GeoItemRenderer;

public class TamagamiHammerRenderer extends GeoItemRenderer<TamagamiHammerItem> {

	public TamagamiHammerRenderer() {
		super(new TamagamiHammerModel());
	}

	@Override
	public void actuallyRender(PoseStack poseStack, TamagamiHammerItem animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int renderColor) {
        float red = net.minecraft.util.FastColor.ARGB32.red(renderColor) / 255.0F;
        float green = net.minecraft.util.FastColor.ARGB32.green(renderColor) / 255.0F;
        float blue = net.minecraft.util.FastColor.ARGB32.blue(renderColor) / 255.0F;
        float alpha = net.minecraft.util.FastColor.ARGB32.alpha(renderColor) / 255.0F;

		super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, com.dragonminez.client.render.util.RenderColors.pack(red, green, blue, alpha));

	}

	@Override
	public RenderType getRenderType(TamagamiHammerItem animatable, Identifier texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityCutoutNoCull(texture);
	}
}
