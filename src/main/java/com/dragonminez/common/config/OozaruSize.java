package com.dragonminez.common.config;

/** Matches the classic saga great ape, whose 129-pixel model renders at 1.8 scale. */
public final class OozaruSize {
    public static final float MODEL_HEIGHT = 32f / 16f;
    public static final float TARGET_HEIGHT = 129f / 16f * 1.8f;

    private OozaruSize() {}

    public static void apply(FormConfig.FormData form) {
        FormSize size = new FormSize();
        size.setReference("dragonminez:saga_ozaru");
        // FormSize expresses a nominal 1.9-block body; this custom model is 2 blocks high.
        size.setHeight(TARGET_HEIGHT * 1.9f / MODEL_HEIGHT);
        form.setSize(size);
    }

    public static void upgrade(FormConfig group) {
        if (group == null || !"oozaru".equalsIgnoreCase(group.getGroupName())) return;
        for (var form : group.getForms().values()) {
            if (!"oozaru".equals(form.getCustomModel())) continue;
            Float[] scale = form.getModelScaling();
            if (scale == null || scale.length != 3) continue;
            if (!java.util.Arrays.equals(scale, new Float[]{3.8f, 3.8f, 3.8f})) continue;
            FormSize size = form.getSize();
            if (size != null && (size.getHeight() != null || size.getWidth() != null
                    || size.getEyeRatio() != null || size.getMultiplier() != 1f
                    || !"dragonminez:saga_saiyan_oozaru".equals(size.getReference()))) continue;
            apply(form);
        }
    }
}
