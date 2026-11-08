package com.dragonminez.client.animation;

/** Keeps short network gaps from restarting the ki charge animation. */
public final class ChargeState {
	private static final int GRACE_TICKS = 6;
	private int expires = Integer.MIN_VALUE;

	public boolean update(int tick, boolean synced, boolean local, boolean held) {
		if (synced) expires = tick + GRACE_TICKS;
		return synced || tick < expires && (!local || held);
	}
}
