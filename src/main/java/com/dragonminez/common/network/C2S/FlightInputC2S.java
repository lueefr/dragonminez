package com.dragonminez.common.network.C2S;

import com.dragonminez.api.flight.FlightMotion.Controls;
import com.dragonminez.common.flight.FlightServer;
import com.dragonminez.compat.network.NetworkEvent;
import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public record FlightInputC2S(long session, long sequence, Controls controls) {
    public FlightInputC2S(FriendlyByteBuf buffer) {
        this(buffer.readLong(), buffer.readLong(), new Controls(buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readBoolean(), buffer.readBoolean()));
    }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeLong(session); buffer.writeLong(sequence);
        buffer.writeFloat((float) controls.pitch()); buffer.writeFloat((float) controls.yaw());
        buffer.writeFloat((float) controls.roll()); buffer.writeFloat((float) controls.throttle());
        buffer.writeFloat((float) controls.lift()); buffer.writeBoolean(controls.boost()); buffer.writeBoolean(controls.brake());
    }
    public static void handle(FlightInputC2S message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            var player = context.get().getSender();
            if (player != null) FlightServer.accept(player, message.session(), message.sequence(), message.controls());
        });
        context.get().setPacketHandled(true);
    }
}
