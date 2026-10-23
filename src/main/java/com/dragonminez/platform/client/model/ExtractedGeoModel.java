package com.dragonminez.platform.client.model;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

/** Resolves configurable resources during extraction, keeping entity access off the render stage. */
public abstract class ExtractedGeoModel<T extends GeoAnimatable> extends GeoModel<T> {
    private record Resources(Identifier geometry, Identifier texture) {}
    private static final DataTicket<Resources> RESOURCES = DataTickets.create("dragonminez:model_resources", Resources.class);

    public abstract Identifier getModelResource(T animatable);
    public abstract Identifier getTextureResource(T animatable);

    @Override public final Identifier getModelResource(GeoRenderState state) {
        return resources(state).geometry();
    }
    @Override public final Identifier getTextureResource(GeoRenderState state) {
        return resources(state).texture();
    }
    @Override public void addAdditionalStateData(T animatable, Object relatedObject, GeoRenderState state) {
        super.addAdditionalStateData(animatable, relatedObject, state);
        state.addGeckolibData(RESOURCES, new Resources(getModelResource(animatable), getTextureResource(animatable)));
    }
    private Resources resources(GeoRenderState state) {
        Resources resources = state.getGeckolibData(RESOURCES);
        if (resources == null) throw new IllegalStateException("Model resources were requested before extraction: " + getClass().getName());
        return resources;
    }
}
