package com.dragonminez.client.init.entities.renderer.rr;

import com.dragonminez.client.render.layer.RedRibbonOutfitLayer;
import com.dragonminez.client.util.SkinCacheManager;
import com.dragonminez.common.init.entities.redribbon.RedRibbonSoldierEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class RedRibbonSoldierRenderer<T extends RedRibbonSoldierEntity> extends RedRibbonRenderer<T> {

    public RedRibbonSoldierRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
        this.shadowRadius = 0.4f;

        addRenderLayer(new RedRibbonOutfitLayer<>(this));
    }

    @Override
    public Identifier getTextureLocation(T animatable) {
        return SkinCacheManager.resolveTexture(animatable.getSkinOwner());
    }

    @Override
    public RenderType getRenderType(T animatable, Identifier texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return net.minecraft.client.renderer.rendertype.RenderTypes.entityCutoutCull(texture);
    }
}
