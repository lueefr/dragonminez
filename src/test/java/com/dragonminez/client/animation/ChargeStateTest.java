package com.dragonminez.client.animation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChargeStateTest {
	@Test void syncGap() {
		ChargeState state = new ChargeState();
		assertTrue(state.update(100, true, true, true));
		for (int tick = 101; tick <= 105; tick++) {
			assertTrue(state.update(tick, false, true, true));
		}
		assertFalse(state.update(106, false, true, true));
	}

	@Test void frames() {
		ChargeState state = new ChargeState();
		assertTrue(state.update(100, true, true, true));
		for (int frame = 0; frame < 200; frame++) {
			assertTrue(state.update(101, false, true, true));
		}
		assertTrue(state.update(102, false, true, true));
	}

	@Test void release() {
		ChargeState state = new ChargeState();
		assertTrue(state.update(100, true, true, true));
		assertFalse(state.update(101, false, true, false));
		assertFalse(state.update(102, false, true, false));
	}

	@Test void remote() {
		ChargeState state = new ChargeState();
		assertFalse(state.update(100, false, false, false));
		assertTrue(state.update(101, true, false, false));
		assertTrue(state.update(102, false, false, false));
		assertFalse(state.update(107, false, false, false));
	}
}
