package com.dragonminez.client.init.blocks.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.block.entity.EnergyCableBlockEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;

public class EnergyCableBlockModel extends GeoModel<EnergyCableBlockEntity> {
	@Override
	public Identifier getModelResource(EnergyCableBlockEntity animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/block/energy_cable.geo.json");
	}

	@Override
	public Identifier getTextureResource(EnergyCableBlockEntity animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/block/custom/energy_cable.png");
	}

	@Override
	public Identifier getAnimationResource(EnergyCableBlockEntity animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/block/energy_cable.animation.json");
	}
}