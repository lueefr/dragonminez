package com.dragonminez.common.network.S2C;

import com.dragonminez.compat.DistExecutor;
import com.dragonminez.compat.network.NetworkEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import java.util.UUID;
import java.util.function.Supplier;

public record ProjectileMotionS2C(int entity, UUID uuid, int owner, UUID ownerUuid, ResourceLocation dimension, Vec3 position, Vec3 motion) {
    public ProjectileMotionS2C(FriendlyByteBuf buffer){this(buffer.readVarInt(),buffer.readUUID(),buffer.readVarInt(),buffer.readUUID(),buffer.readResourceLocation(),read(buffer),read(buffer));}
    private static Vec3 read(FriendlyByteBuf buffer){return new Vec3(buffer.readDouble(),buffer.readDouble(),buffer.readDouble());}
    private static void write(FriendlyByteBuf buffer,Vec3 vector){buffer.writeDouble(vector.x);buffer.writeDouble(vector.y);buffer.writeDouble(vector.z);}
    public void encode(FriendlyByteBuf buffer){buffer.writeVarInt(entity);buffer.writeUUID(uuid);buffer.writeVarInt(owner);buffer.writeUUID(ownerUuid);buffer.writeResourceLocation(dimension);write(buffer,position);write(buffer,motion);}
    public static void handle(ProjectileMotionS2C message,Supplier<NetworkEvent.Context> context){
        context.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.dragonminez.client.combat.ProjectileMotion.receive(message)));
        context.get().setPacketHandled(true);
    }
}
