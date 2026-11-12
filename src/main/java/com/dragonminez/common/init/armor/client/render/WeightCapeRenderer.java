package com.dragonminez.common.init.armor.client.render;

import com.dragonminez.Reference;
import com.dragonminez.common.init.armor.client.model.WeightCapeModel;
import com.dragonminez.common.init.item.WeightItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class WeightCapeRenderer extends GeoArmorRenderer<WeightItem> {
    public WeightCapeRenderer() {
        super(new WeightCapeModel());
    }

    private float clothPartial, clothTextureU;

    @Override
    public void renderRecursively(com.mojang.blaze3d.vertex.PoseStack stack, WeightItem item,
            software.bernie.geckolib.cache.object.GeoBone bone, RenderType type, MultiBufferSource source,
            com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean rerender, float partial, int light, int overlay, int color) {
        if (bone.getName().startsWith("cape")) return;
        clothPartial = partial;
        clothTextureU = com.dragonminez.client.render.cloth.CapeCloth.textureU(getTextureLocation(item));
        if (source instanceof com.dragonminez.client.render.shader.TransformationMaskBufferSource mask) {
            mask.setMaskCaptureEnabled(true);
            mask.setMaskCaptureBlocked(false);
            buffer = source.getBuffer(type);
        }
        super.renderRecursively(stack, item, bone, type, source, buffer, rerender, partial, light, overlay, color);
    }

    @Override
    public void renderCubesOfBone(com.mojang.blaze3d.vertex.PoseStack stack, software.bernie.geckolib.cache.object.GeoBone bone,
            com.mojang.blaze3d.vertex.VertexConsumer buffer, int light, int overlay, int color) {
        if ("armorBody".equals(bone.getName()) && getCurrentEntity() instanceof net.minecraft.world.entity.LivingEntity wearer)
            com.dragonminez.client.render.cloth.CapeCloth.render(wearer, true, clothPartial, stack, buffer, light, overlay, color, clothTextureU);
    }

    @Override
    public ResourceLocation getTextureLocation(WeightItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/weighted_items.png");
    }

    @Override
    public RenderType getRenderType(WeightItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.armorCutoutNoCull(texture);
    }
}
