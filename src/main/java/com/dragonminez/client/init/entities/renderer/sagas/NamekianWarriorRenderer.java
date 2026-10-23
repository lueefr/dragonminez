package com.dragonminez.client.init.entities.renderer.sagas;

import com.dragonminez.client.init.entities.model.sagas.NamekianModel;
import com.dragonminez.common.init.entities.namek.NamekTraderEntity;
import com.dragonminez.common.init.entities.namek.NamekWarriorEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.renderer.GeoEntityRenderer;

public class NamekianWarriorRenderer extends GeoEntityRenderer<NamekWarriorEntity> {

    public NamekianWarriorRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new NamekianModel<>());
        this.shadowRadius = 0.4f;
    }

    @Override
    public Identifier getTextureLocation(NamekWarriorEntity animatable) {
        return animatable.getCurrentTexture();
    }

    @Override
    public RenderType getRenderType(NamekWarriorEntity animatable, Identifier texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutout(texture);
    }
}
