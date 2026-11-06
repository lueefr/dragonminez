package com.dragonminez.common.config;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Dimensions share the model pipeline, including transitions and flight. */
@Getter
@Setter
public final class FormSize {
    private float multiplier = 1;
    private Float height;
    private Float width;
    private Float eyeRatio;
    private String reference;

    public void validate() {
        if (!Float.isFinite(multiplier) || multiplier < 0.05f || multiplier > 16) throw new IllegalArgumentException("Form size multiplier must be 0.05..16");
        if (height != null && (!Float.isFinite(height) || height < 0.1f || height > 32)) throw new IllegalArgumentException("Form height must be 0.1..32 blocks");
        if (width != null && (!Float.isFinite(width) || width < 0.1f || width > 32)) throw new IllegalArgumentException("Form width must be 0.1..32 blocks");
        if (eyeRatio != null && (!Float.isFinite(eyeRatio) || eyeRatio < 0.05f || eyeRatio > 1)) throw new IllegalArgumentException("Form eyeRatio must be 0.05..1");
        if (reference != null && BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(reference)).isEmpty()) throw new IllegalArgumentException("Unknown form size entity: " + reference);
    }

    public Float[] resolve(Float[] base) {
        float factor = multiplier;
        if (height != null) factor *= height / (1.9f * base[1]);
        else if (reference != null) factor *= BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(reference)).orElseThrow().getHeight() / (1.9f * base[1]);
        Float[] result = {base[0]*factor, base[1]*factor, base[2]*factor};
        if (width != null) result[0] = result[2] = width * multiplier / 0.6f;
        for (float value : result) if (!Float.isFinite(value) || value < 0.01f || value > 64) throw new IllegalArgumentException("Resolved form scale outside 0.01..64");
        return result;
    }
}
