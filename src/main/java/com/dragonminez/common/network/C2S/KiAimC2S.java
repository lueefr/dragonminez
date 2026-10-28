package com.dragonminez.common.network.C2S;

import com.dragonminez.common.combat.aim.KiAim;
import com.dragonminez.common.network.PacketRateLimiter;
import com.dragonminez.compat.network.NetworkEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import java.util.function.Supplier;

public record KiAimC2S(Vec3 origin,Vec3 direction,int lockedTarget) {
    public KiAimC2S(FriendlyByteBuf b){this(new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),new Vec3(b.readFloat(),b.readFloat(),b.readFloat()),b.readVarInt());}
    public void encode(FriendlyByteBuf b){b.writeDouble(origin.x);b.writeDouble(origin.y);b.writeDouble(origin.z);b.writeFloat((float)direction.x);b.writeFloat((float)direction.y);b.writeFloat((float)direction.z);b.writeVarInt(lockedTarget);}
    public static void handle(KiAimC2S message,Supplier<NetworkEvent.Context> context){
        context.get().enqueueWork(()->{var player=context.get().getSender();
            if(player!=null&&PacketRateLimiter.allow(player.getUUID(),"ki_aim",player.level().getGameTime(),1))KiAim.accept(player,message.origin(),message.direction(),message.lockedTarget());
        });context.get().setPacketHandled(true);
    }
}
