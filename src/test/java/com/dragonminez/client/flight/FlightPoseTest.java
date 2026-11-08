package com.dragonminez.client.flight;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightPoseTest {
	@Test void previews() {
		assertEquals(FlightPose.Mode.NONE, FlightPose.mode(true, true, false, false, false, true));
		assertEquals(FlightPose.Mode.NONE, FlightPose.mode(true, false, false, false, false, true));
		assertEquals(FlightPose.Mode.NONE, FlightPose.mode(false, true, false, true, false, true));
		assertEquals(FlightPose.Mode.NONE, FlightPose.mode(false, true, true, false, false, true));
		assertEquals(FlightPose.Mode.NONE, FlightPose.mode(false, true, false, false, true, true));
	}

	@Test void flightModes() {
		assertEquals(FlightPose.Mode.SLOW, FlightPose.mode(false, false, false, false, false, true));
		assertEquals(FlightPose.Mode.FAST, FlightPose.mode(false, true, false, false, false, true));
		assertEquals(FlightPose.Mode.NONE, FlightPose.mode(false, false, false, false, false, false));
	}

	@Test void cameraYaw() {
		assertEquals(-45.0F, FlightPose.delta(20.0F, 65.0F), 0.001F);
		assertEquals(25.0F, FlightPose.delta(10.0F, -15.0F), 0.001F);
		assertEquals(20.0F, FlightPose.delta(-170.0F, 170.0F), 0.001F);
		assertEquals(0.0F, FlightPose.delta(180.0F, -180.0F), 0.001F);
		float bodyYaw = 20.0F;
		float viewYaw = 65.0F;
		assertEquals(180.0F - viewYaw, 180.0F - bodyYaw + FlightPose.delta(bodyYaw, viewYaw), 0.001F);
	}
}
