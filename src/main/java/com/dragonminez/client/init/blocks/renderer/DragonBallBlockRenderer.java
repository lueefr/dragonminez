package com.dragonminez.client.init.blocks.renderer;

import com.dragonminez.client.init.blocks.model.DragonBallBlockModel;
import com.dragonminez.common.init.block.custom.DragonBallBlock;
import com.dragonminez.common.init.block.entity.DragonBallBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class DragonBallBlockRenderer extends GeoBlockRenderer<DragonBallBlockEntity> {
	public DragonBallBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new DragonBallBlockModel());
    }

	@Override
	public void actuallyRender(PoseStack poseStack, DragonBallBlockEntity animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int renderColor) {
        float red = net.minecraft.util.FastColor.ARGB32.red(renderColor) / 255.0F;
        float green = net.minecraft.util.FastColor.ARGB32.green(renderColor) / 255.0F;
        float blue = net.minecraft.util.FastColor.ARGB32.blue(renderColor) / 255.0F;
        float alpha = net.minecraft.util.FastColor.ARGB32.alpha(renderColor) / 255.0F;
        // One depth-writing textured pass: an inflated additive copy exposed all the
        // overlapping component cubes and flickered as their draw order changed.
        boolean gathered = isGathered(animatable);
        if (gathered && !isReRender) {
            double time = animatable.getLevel() == null ? 0 : animatable.getLevel().getGameTime() + partialTick;
            float pulse = 0.9F + 0.1F * Mth.sin((float) (time * 0.15));
            red *= pulse;
            green *= pulse;
            blue *= pulse;
        }
        int light = gathered ? LightTexture.FULL_BRIGHT : packedLight;
		super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, light, packedOverlay, com.dragonminez.client.render.util.RenderColors.pack(red, green, blue, alpha));
	}

	private static boolean isGathered(DragonBallBlockEntity animatable) {
		BlockState state = animatable.getBlockState();
		return state.hasProperty(DragonBallBlock.GATHERED) && state.getValue(DragonBallBlock.GATHERED);
	}

}
