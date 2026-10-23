package com.dragonminez.common.init.armor.client.render;

import com.dragonminez.Reference;
import com.dragonminez.common.init.armor.client.model.WeightCapeModel;
import com.dragonminez.common.init.item.WeightItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.renderer.GeoArmorRenderer;

public class WeightCapeRenderer extends GeoArmorRenderer<WeightItem> {
    public WeightCapeRenderer() {
        super(new WeightCapeModel());
    }

    @Override
    public Identifier getTextureLocation(WeightItem animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/weighted_items.png");
    }

    @Override
    public RenderType getRenderType(WeightItem animatable, Identifier texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.armorCutoutNoCull(texture);
    }
}
