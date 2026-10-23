package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.clash.BeamClashCinematicCamera;
import com.dragonminez.client.clash.ClientBeamClashState;
import net.fabricmc.api.EnvType;
import com.dragonminez.platform.event.client.ViewportEvent;
import com.dragonminez.platform.event.tick.ClientTickEvent;
import com.dragonminez.platform.event.SubscribeEvent;
import com.dragonminez.platform.event.EventBusSubscriber;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = EnvType.CLIENT)
public class BeamClashClientEvents {

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {

		if (ClientBeamClashState.isActive() && !BeamClashCinematicCamera.isActive()) {
			BeamClashCinematicCamera.activate();
		} else if (!ClientBeamClashState.isActive() && BeamClashCinematicCamera.isActive()) {
			BeamClashCinematicCamera.deactivate();
		}

		BeamClashCinematicCamera.tickFov();
	}

	@SubscribeEvent
	public static void onComputeFov(ViewportEvent.ComputeFov event) {
		event.setFOV(BeamClashCinematicCamera.applyFov(event.getFOV()));
	}
}
