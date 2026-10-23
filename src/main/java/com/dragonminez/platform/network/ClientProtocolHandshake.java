package com.dragonminez.platform.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
final class ClientProtocolHandshake {
    private ClientProtocolHandshake() {}
    static void register(CustomPacketPayload.Type<ProtocolHandshake.Version> type, Supplier<String> version, java.util.function.Predicate<String> accepted) {
        ClientConfigurationNetworking.registerGlobalReceiver(type, (packet, context) -> {
            if (!accepted.test(packet.value())) {
                context.packetContext().orElseThrow(PacketContext.CONNECTION)
                        .disconnect(ProtocolHandshake.mismatch(version.get(), packet.value()));
                return;
            }
            context.responseSender().sendPacket(new ProtocolHandshake.Version(type, version.get()));
        });
    }
}
