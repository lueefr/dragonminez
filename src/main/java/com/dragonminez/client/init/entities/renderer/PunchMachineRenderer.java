package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.init.entities.model.PunchMachineModel;
import com.dragonminez.common.init.entities.PunchMachineEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.geckolib.renderer.GeoEntityRenderer;

public class PunchMachineRenderer extends com.dragonminez.platform.client.render.ExtractedGeoEntityRenderer<PunchMachineEntity> {

    public PunchMachineRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new PunchMachineModel<>());
    }
}
