package com.dragonminez.common.init.armor.client.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.WeightItem;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;

public class WeightCapeModel extends GeoModel<WeightItem> {
    @Override
    public Identifier getModelResource(WeightItem animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/armor/weighted_cape.geo.json");
    }

    @Override
    public Identifier getTextureResource(WeightItem animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/weighted_items.png");
    }

    @Override
    public Identifier getAnimationResource(WeightItem animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/armorcape.animation.json");
    }
}
