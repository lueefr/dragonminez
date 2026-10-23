package com.dragonminez.platform.event.tick;

import com.dragonminez.platform.event.*;

/** Explicit subscriptions generated from the original annotated gameplay handlers. */
public final class ClientTickSubscribers {
    private static boolean installed;
    private ClientTickSubscribers() {}
    public static synchronized void register() {
        if (installed) return;
        GameEvents.BUS.addListener(ClientTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.client.events.BeamClashClientEvents::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.client.events.ClientStatsEvents::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.client.events.CustomTooltipHandler::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.client.events.EffectsEvents::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.client.events.FlySkillEvent::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.client.events.ForgeClientEvents::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.client.events.KiSenseEvent::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.client.events.LockOnEvent::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.client.events.MovementSkillsHandler::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.client.events.SoundClientHandler::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.client.events.VehicleControlHandler::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.client.gui.hud.ScouterHUD::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.client.gui.tutorial.TutorialManager::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.client.init.entities.renderer.ki.GumBindRenderer::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.client.render.camera.OverShoulderCamera::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.client.render.camera.StrikeCinematicCamera::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Pre.class, EventPriority.NORMAL, false, com.dragonminez.client.render.effects.AfterimageEffect::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Pre.class, EventPriority.NORMAL, false, com.dragonminez.client.render.effects.CandyEffect::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Pre.class, EventPriority.NORMAL, false, com.dragonminez.client.render.effects.ClawSlashEffect::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Pre.class, EventPriority.NORMAL, false, com.dragonminez.client.render.effects.DimensionalFistEffect::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Pre.class, EventPriority.NORMAL, false, com.dragonminez.client.render.effects.DimensionalShatterEffect::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Pre.class, EventPriority.NORMAL, false, com.dragonminez.client.render.effects.GaleEffect::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Pre.class, EventPriority.NORMAL, false, com.dragonminez.client.render.effects.ImpactBurstEffect::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.client.render.effects.InstantTransmissionEffect::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.client.render.effects.MajinAbsorptionTracker::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.client.render.effects.NamekAssimilationEffect::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Pre.class, EventPriority.NORMAL, false, com.dragonminez.client.render.effects.RageScreamEffect::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Pre.class, EventPriority.NORMAL, false, com.dragonminez.client.render.effects.ShockwaveEffect::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Pre.class, EventPriority.NORMAL, false, com.dragonminez.client.render.effects.SwordSlashEffect::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Pre.class, EventPriority.NORMAL, false, com.dragonminez.client.render.effects.TelegraphEffect::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Pre.class, EventPriority.NORMAL, false, com.dragonminez.client.systems.KiBurstShakeState::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Pre.class, EventPriority.NORMAL, false, com.dragonminez.client.systems.impactframes.ImpactFramesHandler::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.client.systems.kishare.KiShareInput::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Post.class, EventPriority.NORMAL, false, com.dragonminez.client.systems.kishare.KiShareVisuals::onClientTick);
        GameEvents.BUS.addListener(ClientTickEvent.Pre.class, EventPriority.NORMAL, false, com.dragonminez.client.systems.taiyoken.TaiyokenFlashHandler::onClientTick);
        installed = true;
    }
}
