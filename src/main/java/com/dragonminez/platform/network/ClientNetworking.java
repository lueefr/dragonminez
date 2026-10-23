package com.dragonminez.platform.network;

import com.dragonminez.compat.network.NetworkEvent;
import com.dragonminez.compat.network.simple.SimpleChannel.WrappedPayload;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/** Client-only Fabric linkage, never initialized by a dedicated server. */
@Environment(EnvType.CLIENT)
public final class ClientNetworking {
    private ClientNetworking() {}
    public static void send(CustomPacketPayload payload) { ClientPlayNetworking.send(payload); }
    public static <MSG> void register(CustomPacketPayload.Type<WrappedPayload<MSG>> type,
            BiConsumer<MSG, Supplier<NetworkEvent.Context>> consumer) {
        ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) ->
                consumer.accept(payload.message(), NetworkEvent.wrap(null, context.client(), payload.receivedNanos())));
    }
}
