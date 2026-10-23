package com.dragonminez.common.network.C2S;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import com.dragonminez.compat.network.NetworkEvent;

import java.util.function.Supplier;

public class ReserveFirstHotbarSlotC2S {
	public static final String PLAYER_DATA_KEY = "dmz_reserve_first_hotbar_slot";
	private final boolean enabled;

	public ReserveFirstHotbarSlotC2S(boolean enabled) {
		this.enabled = enabled;
	}

	public ReserveFirstHotbarSlotC2S(FriendlyByteBuf buffer) {
		this.enabled = buffer.readBoolean();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeBoolean(this.enabled);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;
			CompoundTag persisted = com.dragonminez.platform.entity.EntityData.get(player).getCompoundOrEmpty(Player.PERSISTED_NBT_TAG);
			persisted.putBoolean(PLAYER_DATA_KEY, this.enabled);
			com.dragonminez.platform.entity.EntityData.get(player).put(Player.PERSISTED_NBT_TAG, persisted);
		});
		ctx.get().setPacketHandled(true);
	}

	public static boolean isReserved(Player player) {
		return com.dragonminez.platform.entity.EntityData.get(player).getCompoundOrEmpty(Player.PERSISTED_NBT_TAG).getBoolean(PLAYER_DATA_KEY);
	}
}
