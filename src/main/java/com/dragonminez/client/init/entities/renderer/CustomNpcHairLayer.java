package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.render.hair.*;
import com.dragonminez.client.render.layer.DMZHairLayer;
import com.dragonminez.common.hair.*;
import com.dragonminez.common.init.entities.questnpc.CustomNpcEntity;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;
import java.util.*;

final class CustomNpcHairLayer extends GeoRenderLayer<CustomNpcEntity> {
    private static final ResourceLocation HAIR_TEXTURE = ResourceLocation.parse("dragonminez:textures/entity/races/hair.png");
    private final HairMeshBuilder mesh = new HairMeshBuilder();
    private final Map<String, HairEntityState> cache = new HashMap<>();
    private final Matrix4f headPose = new Matrix4f();
    private final Matrix3f headNormal = new Matrix3f();
    private int capturedEntity = -1;

    CustomNpcHairLayer(GeoRenderer<CustomNpcEntity> renderer) { super(renderer); }

    @Override public void renderForBone(PoseStack pose, CustomNpcEntity npc, GeoBone bone, RenderType type,
            MultiBufferSource buffers, VertexConsumer buffer, float partialTick, int light, int overlay) {
        if (!bone.getName().equals("head")) return;
        // Only capture transforms here. Switching buffers in this callback would flush
        // the body's BufferBuilder while GeckoLib still writes the remaining bones.
        pose.pushPose();
        RenderUtil.translateToPivotPoint(pose, bone);
        headPose.set(pose.last().pose());
        headNormal.set(pose.last().normal());
        pose.popPose();
        capturedEntity = npc.getId();
    }

    @Override public void render(PoseStack pose, CustomNpcEntity npc, BakedGeoModel model, RenderType type,
            MultiBufferSource buffers, VertexConsumer buffer, float partialTick, int light, int overlay) {
        if (capturedEntity != npc.getId()) return;
        capturedEntity = -1;
        var definition = npc.definition();
        if (definition == null || definition.hairType().equals("none")) return;
        String key = definition.hairPreset()+"/"+definition.hairType()+"/"+definition.forcedHairCode();
        HairEntityState state = cache.get(key);
        if (state == null) {
            var slot = HairStyleSlot.byHairType(definition.hairType());
            CustomHair hair = definition.forcedHairCode() != null && !definition.forcedHairCode().isBlank()
                    ? HairCodec.fromCode(definition.forcedHairCode()) : HairPresets.get(definition.hairPreset(), slot);
            if (hair == null || hair.isEmpty()) return;
            state = new HairEntityState();
            state.updatePoses(hair, hair, 0, slot, slot);
            if (cache.size() >= 128) cache.clear();
            cache.put(key, state);
        }
        mesh.emit(buffers.getBuffer(RenderType.entityCutoutNoCull(HAIR_TEXTURE)), headPose, headNormal,
                state, false, 0x202020, 0x202020, true, true, light, overlay, 1,
                DMZHairLayer.wantsPixelDetail(npc), null, null, null);
        buffers.getBuffer(type);
    }
}
