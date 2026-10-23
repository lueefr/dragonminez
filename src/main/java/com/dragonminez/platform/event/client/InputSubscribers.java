package com.dragonminez.platform.event.client;
import com.dragonminez.platform.event.*;
public final class InputSubscribers {
    private static boolean installed;
    private InputSubscribers() {}
    public static synchronized void register() {
        if (installed) return;
        GameEvents.BUS.addListener(ViewportEvent.ComputeFov.class, EventPriority.NORMAL, false, com.dragonminez.client.events.BeamClashClientEvents::onComputeFov);
        GameEvents.BUS.addListener(InputEvent.MouseScrollingEvent.class, EventPriority.NORMAL, false, com.dragonminez.client.events.ClientStatsEvents::onMouseScroll);
        GameEvents.BUS.addListener(InputEvent.Key.class, EventPriority.NORMAL, false, com.dragonminez.client.events.ClientStatsEvents::onKeyPressed);
        GameEvents.BUS.addListener(InputEvent.InteractionKeyMappingTriggered.class, EventPriority.NORMAL, false, com.dragonminez.client.events.ClientStatsEvents::onInteractionKey);
        GameEvents.BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, EventPriority.NORMAL, false, com.dragonminez.client.events.ClientStatsEvents::onClientLogout);
        GameEvents.BUS.addListener(ViewportEvent.ComputeCameraAngles.class, EventPriority.NORMAL, false, com.dragonminez.client.events.EffectsEvents::onCameraSetup);
        GameEvents.BUS.addListener(InputEvent.Key.class, EventPriority.NORMAL, false, com.dragonminez.client.events.FlySkillEvent::onKeyPress);
        GameEvents.BUS.addListener(ViewportEvent.RenderFog.class, EventPriority.NORMAL, false, com.dragonminez.client.events.ForgeClientEvents::onRenderFog);
        GameEvents.BUS.addListener(ViewportEvent.ComputeFogColor.class, EventPriority.NORMAL, false, com.dragonminez.client.events.ForgeClientEvents::onComputeFogColor);
        GameEvents.BUS.addListener(ClientPlayerNetworkEvent.Clone.class, EventPriority.NORMAL, false, com.dragonminez.client.events.ForgeClientEvents::onPlayerClone);
        GameEvents.BUS.addListener(ClientPlayerNetworkEvent.LoggingIn.class, EventPriority.NORMAL, false, com.dragonminez.client.events.ForgeClientEvents::onPlayerLogin);
        GameEvents.BUS.addListener(InputEvent.Key.class, EventPriority.NORMAL, false, com.dragonminez.client.events.ForgeClientEvents::onKeyInput);
        GameEvents.BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, EventPriority.NORMAL, false, com.dragonminez.client.events.ForgeClientEvents::onClientDisconnect);
        GameEvents.BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, EventPriority.NORMAL, false, com.dragonminez.client.events.RadarRenderEvent::onClientDisconnect);
        GameEvents.BUS.addListener(ViewportEvent.ComputeFov.class, EventPriority.NORMAL, false, com.dragonminez.client.render.camera.StrikeCinematicCamera::onComputeFov);
        GameEvents.BUS.addListener(ViewportEvent.ComputeFov.class, EventPriority.NORMAL, false, com.dragonminez.client.render.effects.TechniquePreview::onComputeFov);
        GameEvents.BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, EventPriority.NORMAL, false, com.dragonminez.client.render.effects.TechniquePreview::onLoggingOut);
        GameEvents.BUS.addListener(InputEvent.InteractionKeyMappingTriggered.class, EventPriority.NORMAL, false, com.dragonminez.client.systems.kishare.KiShareInput::onInteraction);
        GameEvents.BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, EventPriority.NORMAL, false, com.dragonminez.client.systems.kishare.KiShareInput::onLogout);
        installed = true;
    }
}
