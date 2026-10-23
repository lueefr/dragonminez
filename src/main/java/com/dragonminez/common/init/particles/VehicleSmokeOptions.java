package com.dragonminez.common.init.particles;

import com.dragonminez.common.init.MainParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;


public record VehicleSmokeOptions(float size, float shade, float alpha, int lifetime) implements ParticleOptions {

	public static final com.mojang.serialization.MapCodec<VehicleSmokeOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.FLOAT.fieldOf("size").forGetter(VehicleSmokeOptions::size),
			Codec.FLOAT.fieldOf("shade").forGetter(VehicleSmokeOptions::shade),
			Codec.FLOAT.fieldOf("alpha").forGetter(VehicleSmokeOptions::alpha),
			Codec.INT.fieldOf("lifetime").forGetter(VehicleSmokeOptions::lifetime)
	).apply(instance, VehicleSmokeOptions::new));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, VehicleSmokeOptions> STREAM_CODEC = net.minecraft.network.codec.ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

	@Override
	public ParticleType<?> getType() {
		return MainParticles.VEHICLE_SMOKE.get();
	}

}
