package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.init.entities.model.CustomNpcModel;
import com.dragonminez.common.init.entities.questnpc.CustomNpcEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class CustomNpcRenderer extends GeoEntityRenderer<CustomNpcEntity> {
    public CustomNpcRenderer(EntityRendererProvider.Context context) {
        super(context,new CustomNpcModel());
        shadowRadius=0.4f;
        addRenderLayer(new CustomNpcArmorLayer<>(this));
        addRenderLayer(new CustomNpcHairLayer(this));
    }
}
