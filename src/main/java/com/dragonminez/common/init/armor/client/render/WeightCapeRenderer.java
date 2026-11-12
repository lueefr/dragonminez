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

    @Override
    public void renderRecursively(com.mojang.blaze3d.vertex.PoseStack stack, WeightItem item,
            software.bernie.geckolib.cache.object.GeoBone bone, RenderType type, MultiBufferSource source,
            com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean rerender, float partial, int light, int overlay, int color) {
        String name = bone.getName();
        int segment = "cape".equals(name) ? 0 : "cape2".equals(name) ? 1 : "cape3".equals(name) ? 2 : -1;
        float x = bone.getRotX(), z = bone.getRotZ();
        if (segment >= 0) {
            float[] motion = com.dragonminez.client.render.cloth.CapeMotion.sample(getCurrentEntity(), partial, true, segment);
            bone.setRotX(motion[0]);
            bone.setRotZ(motion[1]);
        }
        try { super.renderRecursively(stack, item, bone, type, source, buffer, rerender, partial, light, overlay, color); }
        finally { bone.setRotX(x); bone.setRotZ(z); }
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
