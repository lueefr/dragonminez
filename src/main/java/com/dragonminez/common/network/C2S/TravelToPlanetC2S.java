package com.dragonminez.common.network.C2S;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import net.minecraft.core.BlockPos;
import com.dragonminez.common.init.entities.SpacePodEntity;
import com.dragonminez.common.network.PacketRateLimiter;
import com.dragonminez.common.spacepod.SpacePodDestinationDefinition;
import com.dragonminez.common.spacepod.SpacePodDestinationRegistry;
import com.dragonminez.common.spacepod.SpacePodArrival;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.dragonminez.compat.network.NetworkEvent;

import java.util.function.Supplier;

public class TravelToPlanetC2S {
	private final String destinationId;

	public TravelToPlanetC2S(String destinationId) {
		this.destinationId = destinationId;
	}

	public static void encode(TravelToPlanetC2S msg, FriendlyByteBuf buf) {
		buf.writeUtf(msg.destinationId, 256);
	}

	public static TravelToPlanetC2S decode(FriendlyByteBuf buf) {
		return new TravelToPlanetC2S(buf.readUtf(256));
	}

	public void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			ServerPlayer player = context.get().getSender();
			if (player == null || !player.isAlive()) return;
			if (!(player.getVehicle() instanceof SpacePodEntity pod)) return;
			if (pod.getControllingPassenger() != player) return;
			if (!PacketRateLimiter.allow(player.getUUID(), "space_pod_travel", player.level().getGameTime(), 20L)) return;

			ServerLevel currentLevel = player.serverLevel();
			SpacePodDestinationDefinition destination = SpacePodDestinationRegistry.getServerDestination(destinationId);
			if (destination == null || !destination.unlockRules().test(player)) {
				return;
			}

			ResourceLocation targetId = ResourceLocation.tryParse(destination.dimension());
			if (targetId == null) {
				return;
			}

			ResourceKey<Level> targetKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, targetId);
			ServerLevel targetLevel = player.server.getLevel(targetKey);
			if (targetLevel == null || targetLevel == currentLevel) {
				return;
			}

			Vec3 sourcePos = player.position();
			SpacePodEntity newPod = new SpacePodEntity(MainEntities.SPACE_POD.get(), targetLevel);
			Vec3 targetPos = landing(targetLevel, destination, sourcePos, newPod);
			if (targetPos == null) return;

			newPod.moveTo(targetPos.x, targetPos.y, targetPos.z, pod.getYRot(), pod.getXRot());
			newPod.setOpenNave(pod.isOpen());
			newPod.startDescent();
			if (!targetLevel.addFreshEntity(newPod)) return;

			float yaw = player.getYRot();
			float pitch = player.getXRot();
			player.stopRiding();
			try {
				player.teleportTo(targetLevel, targetPos.x, targetPos.y, targetPos.z, yaw, pitch);
				if (player.serverLevel() != targetLevel || !player.startRiding(newPod)) {
					throw new IllegalStateException("Could not mount the destination Space Pod");
				}
				pod.discard();
			} catch (Exception e) {
				newPod.discard();
				try {
					if (player.serverLevel() != currentLevel) {
						player.teleportTo(currentLevel, sourcePos.x, sourcePos.y, sourcePos.z, yaw, pitch);
					}
					if (player.serverLevel() == currentLevel) {
						player.startRiding(pod);
					}
				} catch (Exception restore) {
					LogUtil.error(Env.COMMON, "Failed to restore player after Space Pod travel", restore);
				}
				LogUtil.error(Env.COMMON, "Failed to travel with Space Pod to " + destinationId, e);
			}
		});
		context.get().setPacketHandled(true);
	}

	private static Vec3 landing(ServerLevel level, SpacePodDestinationDefinition destination, Vec3 source, SpacePodEntity pod) {
		Vec3 position = destination.resolvePosition(source);
		if (!Double.isFinite(position.x) || !Double.isFinite(position.z)) return null;

		int[] heights = SpacePodArrival.heights(level.getMinBuildHeight(), level.getMaxBuildHeight(), pod.getBbHeight());
		if (heights.length == 0) return null;

		int x = net.minecraft.util.Mth.floor(position.x);
		int z = net.minecraft.util.Mth.floor(position.z);
		BlockPos start = new BlockPos(x, heights[0], z);
		if (!level.getWorldBorder().isWithinBounds(start)) return null;
		level.getChunkAt(start);

		for (int y : heights) {
			Vec3 target = new Vec3(position.x, y + 0.25D, position.z);
			pod.moveTo(target.x, target.y, target.z, 0.0F, 0.0F);
			if (level.noCollision(pod) && level.getFluidState(BlockPos.containing(target)).isEmpty()) {
				return target;
			}
		}
		return null;
	}
}
