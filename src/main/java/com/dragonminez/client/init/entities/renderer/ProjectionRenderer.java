package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.common.init.entities.ki.EnergyProjection;
import com.dragonminez.client.init.entities.model.ProjectionModel;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public final class ProjectionRenderer extends GeoEntityRenderer<EnergyProjection> {
    public ProjectionRenderer(EntityRendererProvider.Context context){super(context,new ProjectionModel());shadowRadius=0;}
    @Override public RenderType getRenderType(EnergyProjection entity,ResourceLocation texture,MultiBufferSource buffers,float tick){return RenderType.entityTranslucent(texture);}
    @Override public Color getRenderColor(EnergyProjection entity,float tick,int light){
        var definition=entity.definition();if(definition==null)return Color.ofRGBA(1f,1f,1f,0f);
        var visual=definition.appearance();int color=visual.color();return Color.ofRGBA((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,visual.opacity()*entity.fade());
    }
    @Override public void preRender(PoseStack pose,EnergyProjection entity,BakedGeoModel model,MultiBufferSource buffers,VertexConsumer vertices,boolean repeated,float tick,int light,int overlay,int color){
        super.preRender(pose,entity,model,buffers,vertices,repeated,tick,light,overlay,color);
        var definition=entity.definition();float scale=definition==null?1:definition.appearance().scale();pose.scale(scale,scale,scale);
    }
    @Override public void actuallyRender(PoseStack pose,EnergyProjection entity,BakedGeoModel model,RenderType type,MultiBufferSource buffers,VertexConsumer vertices,boolean repeated,float tick,int light,int overlay,int color){
        var definition=entity.definition();super.actuallyRender(pose,entity,model,type,buffers,vertices,repeated,tick,definition!=null&&definition.appearance().bright()?15728880:light,overlay,color);
    }
}
