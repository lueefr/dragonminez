package com.dragonminez.client.init.blocks.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.block.entity.PatternStationBlockEntity;
import net.minecraft.resources.Identifier;
import com.dragonminez.platform.client.model.ExtractedGeoModel;

public class PatternStationBlockModel extends ExtractedGeoModel<PatternStationBlockEntity> {
	@Override
	public Identifier getModelResource(PatternStationBlockEntity animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/block/pattern_station.geo.json");
	}

	@Override
	public Identifier getTextureResource(PatternStationBlockEntity animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/block/custom/pattern_station.png");
	}

	@Override
	public Identifier getAnimationResource(PatternStationBlockEntity animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/block/pattern_station.animation.json");
	}
}
