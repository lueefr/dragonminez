package com.dragonminez.common.network.C2S;

import com.dragonminez.common.wish.wishes.ReCustomizeWish;
import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairSanitizer;
import com.dragonminez.common.hair.HairStyleSlot;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.PacketRateLimiter;
import com.dragonminez.common.network.S2C.AppearanceSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.dragonminez.compat.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateCustomHairC2S {
	private static final long MIN_INTERVAL_TICKS = 10L;

	private final int hairIndex;
	private final CustomHair customHair;
    private String form;
    public UpdateCustomHairC2S(String form,CustomHair hair) {this(-1,hair);this.form=form;}

	public UpdateCustomHairC2S(HairStyleSlot slot, CustomHair customHair) {
		this(slot.index(), customHair);
	}

	private UpdateCustomHairC2S(int hairIndex, CustomHair customHair) {
		this.hairIndex = hairIndex;
		this.customHair = customHair;
	}

	public static void encode(UpdateCustomHairC2S msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.hairIndex);
		boolean hasHair = msg.customHair != null;
		buf.writeBoolean(hasHair);
		if (hasHair) msg.customHair.writeToBuffer(buf);
        if(msg.hairIndex==-1)buf.writeUtf(msg.form,256);
	}

	public static UpdateCustomHairC2S decode(FriendlyByteBuf buf) {
		int hairIndex = buf.readInt();
		boolean hasHair = buf.readBoolean();
		CustomHair hair = hasHair ? CustomHair.readFromBuffer(buf) : null;
		var result=new UpdateCustomHairC2S(hairIndex,hair);if(hairIndex==-1)result.form=buf.readUtf(256);return result;
	}

	public static void handle(UpdateCustomHairC2S msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null || msg.customHair == null) return;

			String playerName = player.getGameProfile().getName();
			HairStyleSlot slot = HairStyleSlot.byIndex(msg.hairIndex);
			if (slot == null && msg.form==null) {
				LogUtil.warn(Env.SERVER, "Rejected hair update from {}: invalid style index {}", playerName, msg.hairIndex);
				return;
			}
			if (!PacketRateLimiter.allow(player.getUUID(), msg.form==null?"hair_style_" + slot.name():"form_hair", player.level().getGameTime(), MIN_INTERVAL_TICKS)) {
				LogUtil.debug(Env.SERVER, "Rate limited hair update from {} for style {}", playerName, slot);
				return;
			}

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				boolean allowed = !data.getStatus().isHasCreatedCharacter() || ReCustomizeWish.isPending(player)
						|| NPCActionC2S.isNpcInRange(player, "popo");
				if (!allowed) {
					LogUtil.warn(Env.SERVER, "Rejected hair update from {}: no character creation, recustomize wish or Popo nearby", playerName);
					return;
				}
				if(msg.form!=null) {
                    int colon=msg.form.indexOf(':');if(colon<1)return;
                    String race=msg.form.substring(0,colon),id=msg.form.substring(colon+1);
                    if(data.getStatus().isHasCreatedCharacter()&&!race.equals(data.getCharacter().getRaceName()))return;
                    com.dragonminez.common.config.FormConfig.FormData definition;
                    try{definition=com.dragonminez.api.forms.FormHair.get(race,id);}catch(IllegalArgumentException ex){return;}
                    if(definition==null||!Boolean.TRUE.equals(definition.getEditHair()))return;
                    HairSanitizer.sanitizeAndLog(msg.customHair,HairStyleSlot.BASE,playerName);
                    data.getCharacter().setFormHairKey(msg.form,msg.customHair);
                    NetworkHandler.sendToTrackingEntityAndSelf(new AppearanceSyncS2C(player),player);return;
                }
                HairSanitizer.sanitizeAndLog(msg.customHair, slot, playerName);
				data.getCharacter().setHairStyle(slot, msg.customHair);
				data.getCharacter().setHairId(0);
				NetworkHandler.sendToTrackingEntityAndSelf(new AppearanceSyncS2C(player), player);
			});
		});
		ctx.get().setPacketHandled(true);
	}
}
