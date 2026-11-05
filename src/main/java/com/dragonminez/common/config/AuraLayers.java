package com.dragonminez.common.config;

import com.dragonminez.api.rules.Conditions;
import com.google.gson.JsonObject;
import java.util.*;
import lombok.Getter;

/** Optional layer lists replace legacy main/extra definitions without changing old files. */
public final class AuraLayers {
    private AuraLayers() {}
    @Getter
    public static final class Layer {
        private String id;
        private String type2d = "kakarot", type3d = "smooth", color = "#FFFFFF";
        private float scale = 1, opacity = 1;
        private int order;
        private JsonObject when = new JsonObject();
        private JsonObject sealedWhen;
        private double sealSeconds = 0.4;
        private FormConfig.Aura3DStyle style = new FormConfig.Aura3DStyle();
    }
    public static void validate(List<Layer> layers) {
        if (layers == null) return;
        if (layers.size() > 16) throw new IllegalArgumentException("At most 16 aura layers per definition");
        Set<String> ids = new HashSet<>();
        for (var layer : layers) {
            if (layer == null || layer.id == null || !layer.id.matches("[a-z0-9_.:/-]{1,128}") || !ids.add(layer.id)) throw new IllegalArgumentException("Aura layers need unique stable IDs");
            if (layer.color == null || !layer.color.matches("#[0-9a-fA-F]{6}") || !Float.isFinite(layer.scale) || layer.scale < 0.05 || layer.scale > 8 || !Float.isFinite(layer.opacity) || layer.opacity < 0 || layer.opacity > 1) throw new IllegalArgumentException("Invalid aura color, scale or opacity");
            if (layer.type2d == null || !layer.type2d.matches("[a-z0-9_/-]{1,64}") || !Set.of("smooth", "sparking", "god", "userPreference").contains(layer.type3d)) throw new IllegalArgumentException("Invalid aura renderer type");
            if (layer.when == null || layer.style == null) throw new IllegalArgumentException("Missing aura conditions/style");
            Conditions.validate(layer.when);
            if (layer.sealedWhen != null) Conditions.validate(layer.sealedWhen);
            if (!Double.isFinite(layer.sealSeconds) || layer.sealSeconds < 0.01 || layer.sealSeconds > 10) throw new IllegalArgumentException("Aura sealSeconds must be 0.01..10");
        }
    }
}
