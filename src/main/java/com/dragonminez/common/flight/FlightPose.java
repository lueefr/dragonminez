package com.dragonminez.common.flight;

/** Presentation threshold measured along the player's forward axis, in blocks per tick. */
public final class FlightPose {
    private FlightPose() {}

    public static boolean fast(double forward, double cap) {
        return Double.isFinite(forward) && Double.isFinite(cap) && cap > 0.0 && forward >= cap * 0.5;
    }
}
