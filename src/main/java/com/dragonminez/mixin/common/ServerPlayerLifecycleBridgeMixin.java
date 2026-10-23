package com.dragonminez.mixin.common;

import com.dragonminez.platform.event.GameEvents;
import com.dragonminez.platform.event.player.PlayerEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerLifecycleBridgeMixin {
    @Inject(method = "getTabListDisplayName", at = @At("RETURN"), cancellable = true)
    private void dmz$formatTabName(CallbackInfoReturnable<Component> callback) {
        callback.setReturnValue(GameEvents.BUS.post(new PlayerEvent.TabListNameFormat(
                (ServerPlayer) (Object) this, callback.getReturnValue())).getDisplayName());
    }
    @Inject(method = "setGameMode", at = @At("HEAD"), cancellable = true)
    private void dmz$beforeGameModeChange(GameType target, CallbackInfoReturnable<Boolean> callback) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        GameType current = player.gameMode.getGameModeForPlayer();
        if (current != target && GameEvents.BUS.post(new PlayerEvent.PlayerChangeGameModeEvent(player, current, target)).isCanceled()) {
            callback.setReturnValue(false);
        }
    }
}
