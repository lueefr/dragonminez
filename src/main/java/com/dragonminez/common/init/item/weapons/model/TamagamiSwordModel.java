package com.dragonminez.common.init.item.weapons.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.weapons.TamagamiSwordItem;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;

public class TamagamiSwordModel extends GeoModel<TamagamiSwordItem> {
	@Override
	public Identifier getModelResource(TamagamiSwordItem animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/tamagami_sword.geo.json");
	}

	@Override
	public Identifier getTextureResource(TamagamiSwordItem animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/weapons/tamagami_sword.png");
	}

	@Override
	public Identifier getAnimationResource(TamagamiSwordItem animatable) {
		return null;
	}
}
