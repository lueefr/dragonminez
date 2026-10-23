package com.dragonminez.client.init.entities.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.animal.DinoFlyEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import com.geckolib.constant.DataTickets;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.animation.state.AnimationTest;
import com.dragonminez.platform.client.model.HeadTrackingGeoModel;

public class DinoFlyModel extends HeadTrackingGeoModel<DinoFlyEntity> {

    @Override
    public Identifier getModelResource(DinoFlyEntity animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/animal/dino3.geo.json");
    }

    @Override
    public Identifier getTextureResource(DinoFlyEntity animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/animal/dino3.png");
    }

    @Override
    public Identifier getAnimationResource(DinoFlyEntity animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/animal/dino3.animation.json");
    }

}
