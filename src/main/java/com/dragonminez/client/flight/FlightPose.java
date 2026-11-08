package com.dragonminez.client.flight;

/** Selects the player body rotation without changing entity or camera rotation. */
public final class FlightPose {
	public enum Mode { NONE, SLOW, FAST }

	private FlightPose() {}

	public static Mode mode(boolean preview, boolean fast, boolean bound,
			boolean seated, boolean transforming, boolean flying) {
		if (preview || bound || seated || transforming) return Mode.NONE;
		if (fast) return Mode.FAST;
		return flying ? Mode.SLOW : Mode.NONE;
	}

	public static float delta(float bodyYaw, float viewYaw) {
		float diff = (bodyYaw - viewYaw) % 360.0F;
		if (diff >= 180.0F) diff -= 360.0F;
		if (diff < -180.0F) diff += 360.0F;
		return diff;
	}
}
