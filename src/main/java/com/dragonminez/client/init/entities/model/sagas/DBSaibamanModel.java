package com.dragonminez.client.init.entities.model.sagas;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.core.registries.BuiltInRegistries;
import com.geckolib.constant.DataTickets;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.animation.state.AnimationTest;
import com.dragonminez.platform.client.model.HeadTrackingGeoModel;

public class DBSaibamanModel<T extends DBSagasEntity> extends HeadTrackingGeoModel<T> {

    private static final Identifier[] ANIMATION_FALLBACKS = {
            Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/races/movement.animation.json")
    };

    @Override
    public Identifier getModelResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/sagas/" + animatable.getGeckolibModelName() + ".geo.json");
    }

    @Override
    public Identifier getTextureResource(T animatable) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(animatable.getType()).getPath();
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/sagas/" + name + ".png");
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(animatable.getType()).getPath();
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/sagas/saga_saibaman.animation.json");
    }

    @Override
    public Identifier[] getAnimationResourceFallbacks(T animatable) {
        return ANIMATION_FALLBACKS;
    }


}
