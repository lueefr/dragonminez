package com.dragonminez.client.init.blocks.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.block.entity.KikonoStationBlockEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;

public class KikonoStationBlockModel extends GeoModel<KikonoStationBlockEntity> {
	@Override
	public Identifier getModelResource(KikonoStationBlockEntity animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/block/kikono_station.geo.json");
	}

	@Override
	public Identifier getTextureResource(KikonoStationBlockEntity animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/block/custom/kikono_station.png");
	}

	@Override
	public Identifier getAnimationResource(KikonoStationBlockEntity animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/block/kikono_station.animation.json");
	}
}