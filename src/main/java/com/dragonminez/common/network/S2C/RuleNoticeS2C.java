package com.dragonminez.common.network.S2C;

import com.dragonminez.compat.DistExecutor;
import com.dragonminez.compat.network.NetworkEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.neoforged.api.distmarker.Dist;
import java.util.function.Supplier;

/** Server-authoritative denial, retaining translatable components and per-form text colors. */
public record RuleNoticeS2C(Component message, Integer color) {
    public RuleNoticeS2C(FriendlyByteBuf buf) {
        this(buf.readJsonWithCodec(ComponentSerialization.CODEC),buf.readBoolean()?buf.readInt():null);
    }
    public void encode(FriendlyByteBuf buf) {
        buf.writeJsonWithCodec(ComponentSerialization.CODEC,message);
        buf.writeBoolean(color!=null);if(color!=null)buf.writeInt(color);
    }
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->
                com.dragonminez.client.gui.RuleNoticeScreen.open(message,color)));
        context.get().setPacketHandled(true);
    }
}
