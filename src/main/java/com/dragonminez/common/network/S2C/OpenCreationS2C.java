package com.dragonminez.common.network.S2C;

import com.dragonminez.compat.network.NetworkEvent;
import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public final class OpenCreationS2C {
    public OpenCreationS2C() {}
    public OpenCreationS2C(FriendlyByteBuf buffer) {}
    public void encode(FriendlyByteBuf buffer) {}
    public static void handle(OpenCreationS2C packet,Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> com.dragonminez.compat.DistExecutor.unsafeRunWhenOn(
                net.neoforged.api.distmarker.Dist.CLIENT, () -> () -> com.dragonminez.client.network.CreationScreen.open()));
        context.get().setPacketHandled(true);
    }
}
