package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.init.entities.model.MasterGlobalModel;
import com.dragonminez.common.init.entities.MastersEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.renderer.GeoEntityRenderer;

public class MasterEntityRenderer <T extends MastersEntity> extends com.dragonminez.platform.client.render.ExtractedGeoEntityRenderer<T> {

    public MasterEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MasterGlobalModel<>());
        this.shadowRadius = 0.4f;
        this.addRenderLayer(new HalloweenPumpkinLayer<>(this));
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float sc = entity.getScale();

        this.shadowRadius = 0.4f * sc;

        poseStack.pushPose();
        poseStack.scale(sc, sc, sc);

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }



    @Override
    public RenderType getRenderType(T animatable, Identifier texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return net.minecraft.client.renderer.rendertype.RenderTypes.entityCutoutCull(texture);
    }
}
