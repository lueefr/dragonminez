package com.dragonminez.client.animation;

/** Keeps short network gaps from restarting the ki charge animation. */
public final class ChargeState {
	private static final int GRACE_TICKS = 6;
	private int lastTick = Integer.MIN_VALUE;
	private int grace;

	public boolean update(int tick, boolean synced, boolean local, boolean held) {
		if (synced) grace = GRACE_TICKS;
		else if (tick != lastTick && grace > 0) grace--;
		lastTick = tick;
		return synced || grace > 0 && (!local || held);
	}
}
