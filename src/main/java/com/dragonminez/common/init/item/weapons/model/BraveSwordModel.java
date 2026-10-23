package com.dragonminez.common.init.item.weapons.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.weapons.BraveSwordItem;
import net.minecraft.resources.Identifier;
import com.dragonminez.platform.client.model.ExtractedGeoModel;

public class BraveSwordModel extends ExtractedGeoModel<BraveSwordItem> {
	@Override
	public Identifier getModelResource(BraveSwordItem animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/brave_sword.geo.json");
	}

	@Override
	public Identifier getTextureResource(BraveSwordItem animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/weapons/brave_sword.png");
	}

	@Override
	public Identifier getAnimationResource(BraveSwordItem animatable) {
		return null;
	}
}
