package com.dragonminez.mixin.client;
import com.dragonminez.platform.event.GameEvents;
import com.dragonminez.platform.event.client.ClientPlayerNetworkEvent;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPacketListener.class)
public abstract class ClientRespawnBridgeMixin {
    @Inject(method = "handleRespawn", at = @At("HEAD"))
    private void dmz$before(ClientboundRespawnPacket packet, CallbackInfo callback, @Share("dmz$oldPlayer") LocalRef<LocalPlayer> old) {
        old.set(Minecraft.getInstance().player);
    }
    @Inject(method = "handleRespawn", at = @At("RETURN"))
    private void dmz$after(ClientboundRespawnPacket packet, CallbackInfo callback, @Share("dmz$oldPlayer") LocalRef<LocalPlayer> old) {
        var player = Minecraft.getInstance().player;
        if (player != null && player != old.get()) GameEvents.BUS.post(new ClientPlayerNetworkEvent.Clone(old.get(), player));
    }
}
