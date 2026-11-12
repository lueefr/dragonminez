package com.dragonminez.common.spacepod;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record SpacePodDestinationDefinition(
		String id,
        @Nullable Integer slot,
		String name,
		boolean translate,
		String dimension,
		@Nullable Integer iconIndex,
		@Nullable String iconTexture,
		@Nullable Double x,
		@Nullable Double y,
		@Nullable Double z,
		boolean showWhenLocked,
		SpacePodUnlockExpression unlockRules
) {
	public static SpacePodDestinationDefinition fromJson(JsonObject object) {
		String id = getRequiredString(object, "id");
        if (id.isBlank() || id.length() > 128) throw new IllegalArgumentException("Invalid destination id");
        Integer slot = object.has("slot") ? object.get("slot").getAsInt() : switch (id) {
            case "overworld" -> 0; case "namek" -> 1; case "otherworld" -> 2;
            case "sacredkaiplanet" -> 3; case "cereal" -> 4; case "beerus" -> 5;
            case "time_chamber" -> 6; case "demon_realm" -> 7; default -> null;
        };
        if (slot != null && (slot < 0 || slot > 255)) throw new IllegalArgumentException("Destination slot must be 0..255");
		String name = getRequiredString(object, "name");
		boolean translate = !object.has("translate") || object.get("translate").getAsBoolean();
		String dimension = getRequiredString(object, "dimension");
        net.minecraft.resources.ResourceLocation.parse(dimension);

		Integer iconIndex = object.has("icon_index") && !object.get("icon_index").isJsonNull()
				? object.get("icon_index").getAsInt()
				: null;
		String iconTexture = object.has("icon_texture") && !object.get("icon_texture").isJsonNull()
				? object.get("icon_texture").getAsString()
				: null;

		if (iconIndex == null && (iconTexture == null || iconTexture.isBlank())) {
			throw new IllegalArgumentException("Destination '" + id + "' must define icon_index or icon_texture");
		}

		if (iconTexture != null) net.minecraft.resources.ResourceLocation.parse(iconTexture);
        if (iconIndex != null && (iconIndex < 0 || iconIndex > 255)) throw new IllegalArgumentException("Invalid destination icon index");

		Double x = getOptionalDouble(object, "x");
		Double y = getOptionalDouble(object, "y");
		Double z = getOptionalDouble(object, "z");
		validateCoordinates(id, x, y, z);

		boolean showWhenLocked = !object.has("show_when_locked") || object.get("show_when_locked").getAsBoolean();

		JsonElement unlockElement = object.get("unlock_rules");
		SpacePodUnlockExpression unlockRules = SpacePodUnlockExpression.fromJson(unlockElement);

		return new SpacePodDestinationDefinition(id, slot, name, translate, dimension, iconIndex, iconTexture, x, y, z, showWhenLocked, unlockRules);
	}

	public Vec3 resolvePosition(Vec3 playerPosition) {
		if (y != null && x == null && z == null) {
			return new Vec3(playerPosition.x, y, playerPosition.z);
		}

		if (x != null && y != null && z != null) {
			return new Vec3(x, y, z);
		}

		return playerPosition;
	}

	private static void validateCoordinates(String id, @Nullable Double x, @Nullable Double y, @Nullable Double z) {
		boolean hasX = x != null;
		boolean hasY = y != null;
		boolean hasZ = z != null;

		if ((hasX || hasZ) && !(hasX && hasY && hasZ)) {
			throw new IllegalArgumentException("Destination '" + id + "' must define x, y and z together when x or z is provided");
		}
	}

	private static @Nullable Double getOptionalDouble(JsonObject object, String key) {
		if (!object.has(key) || object.get(key).isJsonNull()) {
			return null;
		}
		double value = object.get(key).getAsDouble();
        if (!Double.isFinite(value) || Math.abs(value) > 30000000) throw new IllegalArgumentException("Invalid destination coordinate: " + key);
        return value;
	}

	private static String getRequiredString(JsonObject object, String key) {
		if (!object.has(key) || object.get(key).isJsonNull()) {
			throw new IllegalArgumentException("Missing required field '" + key + "'");
		}
		return object.get(key).getAsString();
	}
}
