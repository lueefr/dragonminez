package com.dragonminez.client.init.entities.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.animal.GiantFishEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import com.geckolib.constant.DataTickets;
import com.geckolib.cache.object.GeoBone;
import com.geckolib.animation.AnimationState;
import com.geckolib.model.GeoModel;
import com.geckolib.model.data.EntityModelData;

public class GiantFishModel<T extends GiantFishEntity> extends GeoModel<T> {

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
