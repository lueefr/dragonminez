package com.dragonminez.client.init.entities.model;

import com.dragonminez.api.npc.CustomNpcVisuals;
import com.dragonminez.common.init.entities.questnpc.CustomNpcEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;

public final class CustomNpcModel extends GeoModel<CustomNpcEntity> {
    private CustomNpcVisuals.Base base(CustomNpcEntity npc) {
        var definition=npc.definition();
        return CustomNpcVisuals.get(definition==null ? "master_male" : definition.baseTexture());
    }
    @Override public ResourceLocation getModelResource(CustomNpcEntity npc) { return base(npc).model(); }
    @Override public ResourceLocation getTextureResource(CustomNpcEntity npc) { return base(npc).texture(); }
    @Override public ResourceLocation getAnimationResource(CustomNpcEntity npc) {
        return ResourceLocation.parse("dragonminez:animations/entity/races/movement.animation.json");
    }
    @Override public void setCustomAnimations(CustomNpcEntity npc,long id,AnimationState<CustomNpcEntity> state) {
        var head=getAnimationProcessor().getBone("head");
        var data=state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (head!=null && data!=null) { head.setRotX(data.headPitch()*net.minecraft.util.Mth.DEG_TO_RAD);head.setRotY(data.netHeadYaw()*net.minecraft.util.Mth.DEG_TO_RAD); }
    }
}
