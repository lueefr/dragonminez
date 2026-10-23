package com.dragonminez.platform.event.client;
import com.dragonminez.platform.event.*;
public final class HudSubscribers {
    private static boolean installed;
    private HudSubscribers() {}
    public static synchronized void register() {
        if (installed) return;
        GameEvents.BUS.addListener(RenderGuiLayerEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.client.events.EffectsEvents::renderStaggerOverlay);
        GameEvents.BUS.addListener(RenderGuiLayerEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.client.events.EffectsEvents::renderStunOverlay);
        GameEvents.BUS.addListener(RenderGuiLayerEvent.Pre.class, EventPriority.NORMAL, false, com.dragonminez.client.events.ForgeClientEvents::RenderHealthBar);
        GameEvents.BUS.addListener(RenderGuiLayerEvent.Pre.class, EventPriority.NORMAL, false, com.dragonminez.client.events.ForgeClientEvents::onPreRenderCrosshair);
        GameEvents.BUS.addListener(RenderGuiLayerEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.client.events.ForgeClientEvents::onPostRenderCrosshair);
        GameEvents.BUS.addListener(RenderGuiLayerEvent.Pre.class, EventPriority.NORMAL, false, com.dragonminez.client.events.RadarRenderEvent::onRenderGameOverlay);
        GameEvents.BUS.addListener(RenderGuiLayerEvent.Pre.class, EventPriority.NORMAL, false, com.dragonminez.client.render.effects.BeamClashScreenRenderer::onRenderOverlay);
        GameEvents.BUS.addListener(RenderGuiLayerEvent.Pre.class, EventPriority.NORMAL, false, com.dragonminez.client.render.effects.KnockoutScreenRenderer::onRenderOverlay);
        installed = true;
    }
}
