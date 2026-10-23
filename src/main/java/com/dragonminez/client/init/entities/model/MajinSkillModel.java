package com.dragonminez.client.init.entities.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.MajinSkillEntity;
import com.dragonminez.common.init.entities.animal.NamekFrogEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import com.geckolib.constant.DataTickets;
import com.geckolib.cache.object.GeoBone;
import com.geckolib.animation.AnimationState;
import com.geckolib.model.GeoModel;
import com.geckolib.model.data.EntityModelData;

public class MajinSkillModel<T extends MajinSkillEntity> extends GeoModel<T> {

    @Override
    public Identifier getModelResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/skills/majinskill.geo.json");
    }

    @Override
    public Identifier getTextureResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser.png");
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/skills/majinskill.animation.json");
    }

}
