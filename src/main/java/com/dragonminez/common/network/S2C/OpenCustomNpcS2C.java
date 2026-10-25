package com.dragonminez.common.network.S2C;

import com.dragonminez.compat.DistExecutor;
import com.dragonminez.compat.network.NetworkEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import java.util.function.Supplier;

public record OpenCustomNpcS2C(int entityId) {
    public OpenCustomNpcS2C(FriendlyByteBuf buffer) { this(buffer.readVarInt()); }
    public void encode(FriendlyByteBuf buffer) { buffer.writeVarInt(entityId); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context=supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.dragonminez.client.gui.dialogue.CustomNpcScreen.open(entityId)));
        context.setPacketHandled(true);
    }
}
