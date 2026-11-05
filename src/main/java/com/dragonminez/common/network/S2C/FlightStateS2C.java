package com.dragonminez.common.network.S2C;

import com.dragonminez.api.flight.FlightMotion.*;
import com.dragonminez.compat.DistExecutor;
import com.dragonminez.compat.network.NetworkEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import java.util.function.Supplier;

public record FlightStateS2C(int entity, long session, long sequence, ResourceLocation dimension, boolean active, State state) {
    public FlightStateS2C(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readLong(), buffer.readLong(), buffer.readResourceLocation(), buffer.readBoolean(),
                new State(read(buffer), read(buffer), new Rotation(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), read(buffer)));
    }
    private static Vector read(FriendlyByteBuf buffer) { return new Vector(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()); }
    private static void write(FriendlyByteBuf buffer, Vector value) { buffer.writeDouble(value.x()); buffer.writeDouble(value.y()); buffer.writeDouble(value.z()); }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entity); buffer.writeLong(session); buffer.writeLong(sequence); buffer.writeResourceLocation(dimension); buffer.writeBoolean(active);
        write(buffer, state.position()); write(buffer, state.velocity());
        var rotation = state.rotation();
        buffer.writeDouble(rotation.x()); buffer.writeDouble(rotation.y()); buffer.writeDouble(rotation.z()); buffer.writeDouble(rotation.w());
        write(buffer, state.angular());
    }
    public static void handle(FlightStateS2C message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.dragonminez.client.flight.FlightClient.receive(message)));
        context.get().setPacketHandled(true);
    }
}
