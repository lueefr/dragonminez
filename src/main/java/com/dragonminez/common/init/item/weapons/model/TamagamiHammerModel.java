package com.dragonminez.common.init.item.weapons.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.weapons.TamagamiHammerItem;
import net.minecraft.resources.Identifier;
import com.dragonminez.platform.client.model.ExtractedGeoModel;

public class TamagamiHammerModel extends ExtractedGeoModel<TamagamiHammerItem> {
	@Override
	public Identifier getModelResource(TamagamiHammerItem animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/tamagami_hammer.geo.json");
	}

	@Override
	public Identifier getTextureResource(TamagamiHammerItem animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/weapons/tamagami_hammer.png");
	}

	@Override
	public Identifier getAnimationResource(TamagamiHammerItem animatable) {
		return null;
	}
}
