package com.dragonminez.platform.event.player;

import com.dragonminez.platform.event.GameEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.world.InteractionResult;

public final class InteractionBridge {
    private static boolean installed;
    private InteractionBridge() {}
    public static synchronized void register() {
        if (installed) return;
        UseItemCallback.EVENT.register((player, level, hand) -> GameEvents.BUS.post(new PlayerInteractEvent.RightClickItem(player, hand)).result());
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> GameEvents.BUS.post(new PlayerInteractEvent.RightClickBlock(player, hand, hit)).result());
        AttackBlockCallback.EVENT.register((player, level, hand, pos, face) -> GameEvents.BUS.post(new PlayerInteractEvent.LeftClickBlock(player, hand, pos, face)).result());
        UseEntityCallback.EVENT.register((player, level, hand, target, hit) -> {
            PlayerInteractEvent event = hit == null ? new PlayerInteractEvent.EntityInteract(player, hand, target)
                    : new PlayerInteractEvent.EntityInteractSpecific(player, hand, target, hit.getLocation());
            return GameEvents.BUS.post(event).result();
        });
        AttackEntityCallback.EVENT.register((player, level, hand, target, hit) ->
                GameEvents.BUS.post(new AttackEntityEvent(player, target)).isCanceled() ? InteractionResult.FAIL : InteractionResult.PASS);
        installed = true;
    }
}
