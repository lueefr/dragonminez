package com.dragonminez.common.init.item.render;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.GravityDeviceItem;
import net.minecraft.resources.Identifier;
import com.dragonminez.platform.client.model.ExtractedGeoModel;

public class GravityDeviceItemModel extends ExtractedGeoModel<GravityDeviceItem> {
	@Override
	public Identifier getModelResource(GravityDeviceItem animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/block/gravitydevice.geo.json");
	}

	@Override
	public Identifier getTextureResource(GravityDeviceItem animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/block/custom/gravitydevice.png");
	}

	@Override
	public Identifier getAnimationResource(GravityDeviceItem animatable) {
		return null;
	}
}
