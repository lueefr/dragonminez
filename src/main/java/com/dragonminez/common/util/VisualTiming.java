package com.dragonminez.common.util;

import com.dragonminez.api.forms.FormVisuals;
import com.dragonminez.common.stats.StatsData;

/** Presentation inputs measured in game ticks, independent of render frequency. */
public final class VisualTiming {
    private VisualTiming() {}

    public static double speed(double distance, int ticks) {
        return Double.isFinite(distance) && distance >= 0 ? distance / Math.max(1, ticks) : 0;
    }

    public static boolean moving(double speed, boolean previous) {
        return Double.isFinite(speed) && speed > (previous ? 0.0075 : 0.01);
    }

    public static boolean aura(StatsData data) {
        return data != null && !FormVisuals.sealed(data) && FormVisuals.flag(data, "aura",
                data.getStatus().isAuraActive() || data.getStatus().isPermanentAura() || data.getStatus().isForcedAura());
    }
}
