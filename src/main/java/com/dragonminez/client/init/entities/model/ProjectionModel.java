package com.dragonminez.client.init.entities.model;

import com.dragonminez.common.init.entities.ki.EnergyProjection;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.animation.AnimationState;

public final class ProjectionModel extends GeoModel<EnergyProjection> {
    private static ResourceLocation resource(String id,String fallback){var key=ResourceLocation.parse(id);return Minecraft.getInstance().getResourceManager().getResource(key).isPresent()?key:ResourceLocation.parse(fallback);}
    @Override public ResourceLocation getModelResource(EnergyProjection entity){var definition=entity.definition();return resource(definition==null?"dragonminez:geo/entity/races/human.geo.json":definition.appearance().model(),"dragonminez:geo/entity/races/human.geo.json");}
    @Override public ResourceLocation getTextureResource(EnergyProjection entity){var definition=entity.definition();return resource(definition==null?"minecraft:textures/block/white_wool.png":definition.appearance().texture(),"minecraft:textures/block/white_wool.png");}
    @Override public ResourceLocation getAnimationResource(EnergyProjection entity){var definition=entity.definition();return resource(definition==null?"dragonminez:animations/entity/races/movement.animation.json":definition.appearance().animation(),"dragonminez:animations/entity/races/movement.animation.json");}
    @Override public void setCustomAnimations(EnergyProjection entity,long id,AnimationState<EnergyProjection> state){
        var owner=entity.owner();var head=getAnimationProcessor().getBone("head");
        if(owner!=null&&head!=null){head.setRotX(owner.getXRot()*net.minecraft.util.Mth.DEG_TO_RAD);head.setRotY(net.minecraft.util.Mth.wrapDegrees(owner.getYHeadRot()-entity.getYRot())*net.minecraft.util.Mth.DEG_TO_RAD);}
    }
}
