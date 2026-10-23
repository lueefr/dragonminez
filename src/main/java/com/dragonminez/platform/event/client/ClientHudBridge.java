package com.dragonminez.platform.event.client;

import com.dragonminez.platform.event.GameEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import java.util.List;

public final class ClientHudBridge {
    private static boolean installed;
    private ClientHudBridge() {}
    public static synchronized void register() {
        if (installed) return;
        for (var id : List.of(VanillaHudElements.MISC_OVERLAYS, VanillaHudElements.CROSSHAIR,
                VanillaHudElements.SPECTATOR_MENU, VanillaHudElements.HOTBAR, VanillaHudElements.ARMOR_BAR,
                VanillaHudElements.HEALTH_BAR, VanillaHudElements.FOOD_BAR, VanillaHudElements.AIR_BAR,
                VanillaHudElements.MOUNT_HEALTH, VanillaHudElements.INFO_BAR, VanillaHudElements.EXPERIENCE_LEVEL,
                VanillaHudElements.HELD_ITEM_TOOLTIP, VanillaHudElements.SPECTATOR_TOOLTIP, VanillaHudElements.MOB_EFFECTS,
                VanillaHudElements.BOSS_BAR, VanillaHudElements.SLEEP, VanillaHudElements.DEMO_TIMER,
                VanillaHudElements.SCOREBOARD, VanillaHudElements.OVERLAY_MESSAGE, VanillaHudElements.TITLE_AND_SUBTITLE,
                VanillaHudElements.CHAT, VanillaHudElements.PLAYER_LIST, VanillaHudElements.SUBTITLES)) {
            HudElementRegistry.replaceElement(id, original -> (graphics, delta) -> {
                if (GameEvents.BUS.post(new RenderGuiLayerEvent.Pre(id, graphics, delta)).isCanceled()) return;
                original.extractRenderState(graphics, delta);
                GameEvents.BUS.post(new RenderGuiLayerEvent.Post(id, graphics, delta));
            });
        }
        installed = true;
    }
}
