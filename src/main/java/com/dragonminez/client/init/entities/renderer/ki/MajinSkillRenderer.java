package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.client.init.entities.model.MajinSkillModel;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.entities.MajinSkillEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.util.Color;
import com.geckolib.renderer.GeoEntityRenderer;

public class MajinSkillRenderer<T extends MajinSkillEntity> extends com.dragonminez.platform.client.render.ExtractedGeoEntityRenderer<T> {

    public MajinSkillRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MajinSkillModel<>());
        this.shadowRadius = 0.4f;
    }

    @Override
    public RenderType getRenderType(T animatable, Identifier texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return net.minecraft.client.renderer.rendertype.RenderTypes.entityCutoutCull(texture);
    }

    @Override
    public Color getRenderColor(T animatable, float partialTick, int packedLight) {
        int colorInt = animatable.getBodyColor();

        float[] rgb = ColorUtils.rgbIntToFloat(colorInt);

        return Color.ofRGBA(rgb[0], rgb[1], rgb[2], 1.0f);

    }
}
