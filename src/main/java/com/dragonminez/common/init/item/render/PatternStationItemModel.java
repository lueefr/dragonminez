package com.dragonminez.common.init.item.render;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.PatternStationItem;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;

public class PatternStationItemModel extends GeoModel<PatternStationItem> {
	@Override
	public Identifier getModelResource(PatternStationItem animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/block/pattern_station.geo.json");
	}

	@Override
	public Identifier getTextureResource(PatternStationItem animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/block/custom/pattern_station.png");
	}

	@Override
	public Identifier getAnimationResource(PatternStationItem animatable) {
		return null;
	}
}
