package com.dragonminez.mixin.common;

import com.dragonminez.common.flight.*;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class FlightConnectionMixin implements FlightConnection {
    @Shadow public ServerPlayer player;
    @Shadow private Vec3 awaitingPositionFromClient;
    @Shadow private double firstGoodX;
    @Shadow private double firstGoodY;
    @Shadow private double firstGoodZ;
    @Shadow private double lastGoodX;
    @Shadow private double lastGoodY;
    @Shadow private double lastGoodZ;
    @Shadow private boolean clientIsFloating;

    @Override public boolean awaiting() { return awaitingPositionFromClient != null; }
    @Override public void anchor() {
        firstGoodX = lastGoodX = player.getX();
        firstGoodY = lastGoodY = player.getY();
        firstGoodZ = lastGoodZ = player.getZ();
        clientIsFloating = false;
    }

    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.AFTER), cancellable = true)
    private void flightMove(ServerboundMovePlayerPacket packet, CallbackInfo callback) {
        if (FlightFrames.active(player) && !awaiting()) {
            anchor();
            callback.cancel();
        }
    }

    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"))
    private void flightTeleport(double x, double y, double z, float yaw, float pitch,
                                java.util.Set<net.minecraft.world.entity.RelativeMovement> relative, CallbackInfo callback) {
        if (FlightFrames.active(player)) FlightServer.stop(player);
    }
}
