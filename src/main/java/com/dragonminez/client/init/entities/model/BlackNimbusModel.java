package com.dragonminez.client.init.entities.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.BlackNimbusEntity;
import com.dragonminez.common.init.entities.FlyingNimbusEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;

public class BlackNimbusModel<T extends BlackNimbusEntity> extends GeoModel<T> {

    @Override
    public Identifier getModelResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/kinton.geo.json");
    }

    @Override
    public Identifier getTextureResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/black_kinton.png");
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/kinton.animation.json");
    }

}
