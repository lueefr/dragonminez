package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.init.entities.model.DinoGlobalModel;
import com.dragonminez.common.init.entities.animal.DinoGlobalEntity;
import com.dragonminez.common.init.entities.animal.SabertoothEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.renderer.GeoEntityRenderer;

public class DinosRenderer<T extends DinoGlobalEntity> extends com.dragonminez.platform.client.render.ExtractedGeoEntityRenderer<T> {

    public DinosRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DinoGlobalModel<>());
        this.shadowRadius = 0.8f;
    }

    @Override
    public RenderType getRenderType(T animatable, Identifier texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return net.minecraft.client.renderer.rendertype.RenderTypes.entityCutoutCull(texture);
    }

    @Override
    public Identifier getTextureLocation(T animatable) {
        if (animatable instanceof SabertoothEntity tigre) {
            return tigre.getCurrentTexture();
        }

        return super.getTextureLocation(animatable);
    }
}
