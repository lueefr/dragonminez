package com.dragonminez.common.init.item.weapons.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.weapons.TamagamiTridentItem;
import net.minecraft.resources.Identifier;
import com.dragonminez.platform.client.model.ExtractedGeoModel;

public class TamagamiTridentModel extends ExtractedGeoModel<TamagamiTridentItem> {
	@Override
	public Identifier getModelResource(TamagamiTridentItem animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/tamagami_trident.geo.json");
	}

	@Override
	public Identifier getTextureResource(TamagamiTridentItem animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/weapons/tamagami_trident.png");
	}

	@Override
	public Identifier getAnimationResource(TamagamiTridentItem animatable) {
		return null;
	}
}
