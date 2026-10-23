package com.dragonminez.client.render.util;

public final class RenderColors {
    private RenderColors() {}
    public static int pack(float red, float green, float blue, float alpha) {
        return net.minecraft.util.FastColor.ARGB32.color(channel(alpha), channel(red), channel(green), channel(blue));
    }
    private static int channel(float value) { return Math.clamp(Math.round(value * 255.0F), 0, 255); }
}
