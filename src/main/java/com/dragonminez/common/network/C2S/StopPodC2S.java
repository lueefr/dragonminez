package com.dragonminez.common.network.C2S;

import com.dragonminez.common.init.entities.SpacePodEntity;
import com.dragonminez.compat.network.NetworkEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class StopPodC2S {

    public StopPodC2S() {
    }

    public StopPodC2S(FriendlyByteBuf buffer) {
    }

    public void encode(FriendlyByteBuf buffer) {
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null || !player.isAlive()) return;
            if (!(player.getVehicle() instanceof SpacePodEntity pod)) return;
            if (pod.getControllingPassenger() != player) return;
            pod.stopDescent();
        });
        context.get().setPacketHandled(true);
    }
}
