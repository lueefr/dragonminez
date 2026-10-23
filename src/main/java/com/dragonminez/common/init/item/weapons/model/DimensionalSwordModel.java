package com.dragonminez.common.init.item.weapons.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.weapons.BraveSwordItem;
import com.dragonminez.common.init.item.weapons.DimensionalSwordItem;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;

public class DimensionalSwordModel extends GeoModel<DimensionalSwordItem> {
	@Override
	public Identifier getModelResource(DimensionalSwordItem animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/dimensional_sword.geo.json");
	}

	@Override
	public Identifier getTextureResource(DimensionalSwordItem animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/weapons/dimensional_sword.png");
	}

	@Override
	public Identifier getAnimationResource(DimensionalSwordItem animatable) {
		return null;
	}
}
