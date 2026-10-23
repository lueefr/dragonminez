package com.dragonminez.client.init.entities.renderer.sagas;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.NamekFrogModel;
import com.dragonminez.common.init.entities.animal.NamekFrogEntity;
import com.dragonminez.common.init.entities.animal.NamekFrogGinyuEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.renderer.GeoEntityRenderer;


public class NamekFrogRenderer<T extends NamekFrogEntity> extends com.dragonminez.platform.client.render.ExtractedGeoEntityRenderer<T> {

    private static final Identifier GINYU_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/animal/namekfrog_0.png");

    public NamekFrogRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new NamekFrogModel<>());
        this.shadowRadius = 0.25f;
    }

    @Override
    public Identifier getTextureLocation(T animatable) {
        if (animatable instanceof NamekFrogGinyuEntity) {
            return GINYU_TEXTURE;
        }
        return animatable.getCurrentTexture();
    }

    @Override
    public RenderType getRenderType(T animatable, Identifier texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return net.minecraft.client.renderer.rendertype.RenderTypes.entityCutoutCull(texture);
    }

}
