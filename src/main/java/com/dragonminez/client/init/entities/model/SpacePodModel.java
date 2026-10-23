package com.dragonminez.client.init.entities.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.SpacePodEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;

public class SpacePodModel<T extends SpacePodEntity> extends GeoModel<T> {

    @Override
    public Identifier getModelResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/spacepod.geo.json");
    }

    @Override
    public Identifier getTextureResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/spacepod.png");
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/spacepod.animation.json");
    }

}
