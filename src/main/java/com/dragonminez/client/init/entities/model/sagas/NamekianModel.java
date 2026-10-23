package com.dragonminez.client.init.entities.model.sagas;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.animal.NamekFrogEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import com.geckolib.constant.DataTickets;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.cache.object.GeoBone;
import com.geckolib.animation.AnimationState;
import com.geckolib.model.GeoModel;
import com.geckolib.model.data.EntityModelData;

public class NamekianModel<T extends PathfinderMob & GeoAnimatable> extends GeoModel<T> {

    @Override
    public Identifier getModelResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/enemies/namekian.geo.json");
    }

    @Override
    public Identifier getTextureResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/enemies/namek_trader_0.png");
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/enemies/namekian.animation.json");
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        GeoBone head = getAnimationProcessor().getBone("head");

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

            head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
            head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
        }    }
}
