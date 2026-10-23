package com.dragonminez.mixin.common;

import com.dragonminez.platform.event.GameEvents;
import com.dragonminez.platform.event.tick.PlayerTickEvent;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerTickBridgeMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void dmz$afterPlayerTick(CallbackInfo callback) {
        GameEvents.BUS.post(new PlayerTickEvent.Post((Player) (Object) this));
    }
}
