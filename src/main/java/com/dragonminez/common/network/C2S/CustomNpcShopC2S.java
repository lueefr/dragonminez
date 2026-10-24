package com.dragonminez.common.network.C2S;

import com.dragonminez.api.npc.CustomNpcServices;
import com.dragonminez.common.init.entities.questnpc.CustomNpcEntity;
import com.dragonminez.common.network.PacketRateLimiter;
import com.dragonminez.compat.network.NetworkEvent;
import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public record CustomNpcShopC2S(int entityId, int offerIndex) {
    public CustomNpcShopC2S(FriendlyByteBuf buffer) { this(buffer.readVarInt(),buffer.readVarInt()); }
    public void encode(FriendlyByteBuf buffer) { buffer.writeVarInt(entityId);buffer.writeVarInt(offerIndex); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context=supplier.get();
        context.enqueueWork(() -> {
            var player=context.getSender();
            if (player==null || !PacketRateLimiter.allow(player.getUUID(),"custom_npc_shop",player.level().getGameTime(),4)) return;
            if (player.level().getEntity(entityId) instanceof CustomNpcEntity npc) {
                boolean success=CustomNpcServices.purchase(player,npc,offerIndex);
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(success
                        ? "message.dragonminez.custom_npc.purchase_success" : "message.dragonminez.custom_npc.purchase_failed"),true);
            }
        });
        context.setPacketHandled(true);
    }
}
