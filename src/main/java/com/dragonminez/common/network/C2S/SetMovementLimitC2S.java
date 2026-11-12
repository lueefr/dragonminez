package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.PacketRateLimiter;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.compat.network.NetworkEvent;
import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public record SetMovementLimitC2S(boolean jump, int value) {
    public SetMovementLimitC2S(FriendlyByteBuf buffer) { this(buffer.readBoolean(), buffer.readVarInt()); }
    public void encode(FriendlyByteBuf buffer) { buffer.writeBoolean(jump); buffer.writeVarInt(value); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null || !player.isAlive() || player.isSpectator()
                    || !PacketRateLimiter.allow(player.getUUID(), "movement_limit", player.level().getGameTime(), 1)) return;
            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                if (!data.getStatus().isHasCreatedCharacter() || !data.getSkills().hasSkill(jump ? "jump" : "sprint")) return;
                if (jump) data.getResources().setJumpLimit(value); else data.getResources().setSprintLimit(value);
                StatsSyncS2C.sendRequested(player);
            });
        });
        context.setPacketHandled(true);
    }
}
