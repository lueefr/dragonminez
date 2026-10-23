package com.dragonminez.client.init.entities.model.ki;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.ki.OzaruFistEntity;
import com.dragonminez.common.init.entities.ki.SPDragonFistEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import com.geckolib.cache.object.GeoBone;
import com.geckolib.animation.AnimationState;
import com.geckolib.model.GeoModel;

public class SPOzaruFistModel<T extends OzaruFistEntity> extends GeoModel<T> {

    @Override
    public Identifier getModelResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/skills/sp_ozarufist.geo.json");
    }

    @Override
    public Identifier getTextureResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/skills/sp_ozarufist.png");
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/skills/sp_ozarufist.animation.json");
    }

}
