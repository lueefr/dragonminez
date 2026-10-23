package com.dragonminez.client.init.entities.renderer.sagas;

import com.dragonminez.client.init.entities.model.sagas.NamekianModel;
import com.dragonminez.common.init.entities.namek.NamekTraderEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.renderer.GeoEntityRenderer;

public class NamekianRenderer extends com.dragonminez.platform.client.render.ExtractedGeoEntityRenderer<NamekTraderEntity> {

    public NamekianRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new NamekianModel<>());
        this.shadowRadius = 0.4f;
    }

    @Override
    public Identifier getTextureLocation(NamekTraderEntity animatable) {
        return animatable.getCurrentTexture();
    }

    @Override
    public RenderType getRenderType(NamekTraderEntity animatable, Identifier texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return net.minecraft.client.renderer.rendertype.RenderTypes.entityCutoutCull(texture);
    }
}
