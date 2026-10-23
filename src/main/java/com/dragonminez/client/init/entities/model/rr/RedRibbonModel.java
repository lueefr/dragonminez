package com.dragonminez.client.init.entities.model.rr;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.redribbon.RedRibbonEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.core.registries.BuiltInRegistries;
import com.geckolib.constant.DataTickets;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.animation.state.AnimationTest;
import com.dragonminez.platform.client.model.HeadTrackingGeoModel;

public class RedRibbonModel<T extends RedRibbonEntity> extends HeadTrackingGeoModel<T> {

    @Override
    public Identifier getModelResource(T animatable) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(animatable.getType()).getPath();
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/enemies/" + name + ".geo.json");
    }

    @Override
    public Identifier getTextureResource(T animatable) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(animatable.getType()).getPath();
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/enemies/" + name + ".png");
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(animatable.getType()).getPath();
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/enemies/" + name + ".animation.json");
    }


}
