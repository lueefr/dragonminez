package com.dragonminez.platform.client.render;

import com.dragonminez.platform.client.model.BoneTransformModel;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;

public class ExtractedGeoEntityRenderer<T extends Entity & GeoAnimatable> extends GeoEntityRenderer<T, LivingEntityRenderState> {
    public ExtractedGeoEntityRenderer(EntityRendererProvider.Context context, GeoModel<T> model) { super(context, model); }
    @Override public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> pass, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(pass, snapshots);
        if (getGeoModel() instanceof BoneTransformModel model) model.adjustBones(pass, snapshots);
    }
}
