package com.dragonminez.client.init.entities.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.vehicle.VehicleEntity;
import net.minecraft.resources.Identifier;
import com.dragonminez.platform.client.model.ExtractedGeoModel;

import java.util.function.Function;

public class VehicleModel<T extends VehicleEntity> extends ExtractedGeoModel<T> {

	private final Identifier model;
	private final Function<T, Identifier> texture;

	public VehicleModel(String path) {
		this(path, texture(path));
	}

	public VehicleModel(String path, Identifier texture) {
		this(path, entity -> texture);
	}

	public VehicleModel(String path, Function<T, Identifier> texture) {
		this.model = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/" + path + ".geo.json");
		this.texture = texture;
	}

	public static Identifier texture(String path) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/" + path + ".png");
	}

	@Override
	public Identifier getModelResource(T animatable) {
		return model;
	}

	@Override
	public Identifier getTextureResource(T animatable) {
		return texture.apply(animatable);
	}

	@Override
	public Identifier getAnimationResource(T animatable) {
		return null;
	}
}
