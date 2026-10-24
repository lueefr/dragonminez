package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.render.hair.*;
import com.dragonminez.common.hair.*;
import com.dragonminez.common.init.entities.questnpc.CustomNpcEntity;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;
import java.util.*;

final class CustomNpcHairLayer extends GeoRenderLayer<CustomNpcEntity> {
    private final HairMeshBuilder mesh=new HairMeshBuilder();
    private final Map<String, HairEntityState> cache=new HashMap<>();
    CustomNpcHairLayer(GeoRenderer<CustomNpcEntity> renderer) { super(renderer); }
    @Override public void renderForBone(PoseStack pose,CustomNpcEntity npc,GeoBone bone,RenderType type,
            MultiBufferSource buffers,VertexConsumer buffer,float partialTick,int light,int overlay) {
        if (!bone.getName().equals("head")) return;
        var definition=npc.definition();
        if (definition==null || definition.hairType().equals("none")) return;
        String key=definition.hairPreset()+"/"+definition.hairType()+"/"+definition.forcedHairCode();
        HairEntityState state=cache.get(key);
        if (state==null) {
            var slot=HairStyleSlot.byHairType(definition.hairType());
            CustomHair hair=definition.forcedHairCode()!=null && !definition.forcedHairCode().isBlank()
                    ? HairCodec.fromCode(definition.forcedHairCode()) : HairPresets.get(definition.hairPreset(),slot);
            if (hair==null || hair.isEmpty()) return;
            state=new HairEntityState();state.updatePoses(hair,hair,0,slot,slot);
            if (cache.size()>=128) cache.clear();
            cache.put(key,state);
        }
        pose.pushPose();
        RenderUtil.translateToPivotPoint(pose,bone);
        mesh.emit(buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.parse("dragonminez:textures/entity/races/hair.png"))),
                pose.last().pose(),pose.last().normal(),state,false,0x202020,0x202020,true,true,light,overlay,1,true,null,null,null);
        pose.popPose();
        buffers.getBuffer(type);
    }
}
