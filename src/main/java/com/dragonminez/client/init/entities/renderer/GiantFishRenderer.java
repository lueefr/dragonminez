package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.init.entities.model.GiantFishModel;
import com.dragonminez.common.init.entities.animal.GiantFishEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.renderer.GeoEntityRenderer;

public class GiantFishRenderer<T extends GiantFishEntity> extends com.dragonminez.platform.client.render.ExtractedGeoEntityRenderer<T> {

    public GiantFishRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GiantFishModel<>());
        this.shadowRadius = 1.1f;
    }

    @Override
    public Identifier getTextureLocation(T animatable) {
        return animatable.getCurrentTexture();
    }

    @Override
    public RenderType getRenderType(T animatable, Identifier texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(texture);
    }
}
