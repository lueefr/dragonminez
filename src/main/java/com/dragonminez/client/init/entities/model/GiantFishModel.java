package com.dragonminez.client.init.entities.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.animal.GiantFishEntity;
import net.minecraft.resources.Identifier;
import com.dragonminez.platform.client.model.ExtractedGeoModel;

public class GiantFishModel<T extends GiantFishEntity> extends ExtractedGeoModel<T> {

    @Override
    public Identifier getModelResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/animal/giantfish.geo.json");
    }

    @Override
    public Identifier getTextureResource(T animatable) {
        return animatable.getCurrentTexture();
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/animal/giantfish.animation.json");
    }
}
