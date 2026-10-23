package com.dragonminez.platform.network;

import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.ConfigurationTask;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Both peers must confirm the packet fingerprint before a player can enter the world. */
public final class ProtocolHandshake {
    private ProtocolHandshake() {}

    public record Version(CustomPacketPayload.Type<Version> payloadType, String value) implements CustomPacketPayload {
        @Override public CustomPacketPayload.Type<Version> type() { return payloadType; }
    }
    public static CustomPacketPayload.Type<Version> type(Identifier channel) {
        return new CustomPacketPayload.Type<>(channel.withSuffix("/protocol"));
    }
    public static StreamCodec<FriendlyByteBuf, Version> codec(CustomPacketPayload.Type<Version> type) {
        return StreamCodec.of((buffer, packet) -> buffer.writeUtf(packet.value(), 512),
                buffer -> new Version(type, buffer.readUtf(512)));
    }
    public static void register(Identifier channel, Supplier<String> version, java.util.function.Predicate<String> accepted) {
        var type = type(channel);
        var codec = codec(type);
        var taskType = new ConfigurationTask.Type(channel + "/protocol");
        PayloadTypeRegistry.clientboundConfiguration().register(type, codec);
        PayloadTypeRegistry.serverboundConfiguration().register(type, codec);
        ServerConfigurationNetworking.registerGlobalReceiver(type, (packet, context) -> {
            if (!accepted.test(packet.value())) {
                context.packetListener().disconnect(mismatch(version.get(), packet.value()));
                return;
            }
            ((FabricServerConfigurationPacketListenerImpl) context.packetListener()).completeTask(taskType);
        });
        ServerConfigurationConnectionEvents.CONFIGURE.register((listener, server) -> {
            if (!ServerConfigurationNetworking.canSend(listener, type)) {
                listener.disconnect(Component.literal("DragonMineZ is required on the client (Minecraft 26.1.2 Fabric)."));
                return;
            }
            ((FabricServerConfigurationPacketListenerImpl) listener).addTask(new ConfigurationTask() {
                @Override public void start(Consumer<Packet<?>> sender) {
                    sender.accept(ServerConfigurationNetworking.createClientboundPacket(new Version(type, version.get())));
                }
                @Override public ConfigurationTask.Type type() { return taskType; }
            });
        });
    }
    public static void registerClient(Identifier channel, Supplier<String> version, java.util.function.Predicate<String> accepted) {
        ClientProtocolHandshake.register(type(channel), version, accepted);
    }
    public static Component mismatch(String expected, String received) {
        return Component.literal("DragonMineZ network mismatch. Expected " + expected + ", received " + received
                + ". Install the same Fabric build on the client and server.");
    }
}
