package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.init.entities.model.QuestNPCModel;
import com.dragonminez.common.init.entities.questnpc.QuestNPCEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.renderer.GeoEntityRenderer;

public class QuestNPCRenderer extends com.dragonminez.platform.client.render.ExtractedGeoEntityRenderer<QuestNPCEntity> {

	public QuestNPCRenderer(EntityRendererProvider.Context renderManager) {
		super(renderManager, new QuestNPCModel());
		this.shadowRadius = 0.4f;
		this.addRenderLayer(new HalloweenPumpkinLayer<>(this));
	}

	@Override
	public RenderType getRenderType(QuestNPCEntity animatable, Identifier texture,
									@Nullable MultiBufferSource bufferSource, float partialTick) {
		return net.minecraft.client.renderer.rendertype.RenderTypes.entityCutoutCull(texture);
	}
}

