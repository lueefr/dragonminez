package com.dragonminez.common.init.item.weapons.render;

import com.dragonminez.common.init.item.weapons.YajirobeKatanaItem;
import com.dragonminez.common.init.item.weapons.model.YajirobeKatanaModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.renderer.GeoItemRenderer;

public class YajirobeKatanaRenderer extends GeoItemRenderer<YajirobeKatanaItem> {

    public YajirobeKatanaRenderer() {
        super(new YajirobeKatanaModel());
    }

    @Override
    public RenderType getRenderType(YajirobeKatanaItem animatable, Identifier texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
