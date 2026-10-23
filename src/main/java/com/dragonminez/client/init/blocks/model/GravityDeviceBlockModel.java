package com.dragonminez.client.init.blocks.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.block.entity.GravityDeviceBlockEntity;
import net.minecraft.resources.Identifier;
import com.dragonminez.platform.client.model.ExtractedGeoModel;

public class GravityDeviceBlockModel extends ExtractedGeoModel<GravityDeviceBlockEntity> {
	@Override
	public Identifier getModelResource(GravityDeviceBlockEntity blockEntity) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/block/gravitydevice.geo.json");
	}

	@Override
	public Identifier getTextureResource(GravityDeviceBlockEntity blockEntity) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/block/custom/gravitydevice.png");
	}

	@Override
	public Identifier getAnimationResource(GravityDeviceBlockEntity blockEntity) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/block/gravitydevice.animation.json");
	}
}
