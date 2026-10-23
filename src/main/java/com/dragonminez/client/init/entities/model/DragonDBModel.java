
package com.dragonminez.client.init.entities.model;

import com.dragonminez.Reference;
import com.dragonminez.common.dragonball.DragonAssetDefinition;
import com.dragonminez.common.dragonball.DragonDefinition;
import com.dragonminez.common.init.entities.dragon.DragonWishEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.core.registries.BuiltInRegistries;
import com.geckolib.constant.DataTickets;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.animation.state.AnimationTest;
import com.dragonminez.platform.client.model.HeadTrackingGeoModel;

public class DragonDBModel<T extends DragonWishEntity> extends HeadTrackingGeoModel<T> {

	@Override
	public Identifier getModelResource(T animatable) {
		DragonAssetDefinition assets = resolveAssets(animatable);
		if (assets != null && assets.getModelPath().isPresent()) return Identifier.parse(assets.getModelPath().get());
		String name = BuiltInRegistries.ENTITY_TYPE.getKey(animatable.getType()).getPath();
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/dragon/" + name + ".geo.json");
	}

	@Override
	public Identifier getTextureResource(T animatable) {
		DragonAssetDefinition assets = resolveAssets(animatable);
		if (assets != null && assets.getTexturePath().isPresent()) return Identifier.parse(assets.getTexturePath().get());
		String name = BuiltInRegistries.ENTITY_TYPE.getKey(animatable.getType()).getPath();
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/dragon/" + name + ".png");
	}

	@Override
	public Identifier getAnimationResource(T animatable) {
		DragonAssetDefinition assets = resolveAssets(animatable);
		if (assets != null && assets.getAnimationPath().isPresent()) return Identifier.parse(assets.getAnimationPath().get());
		String name = BuiltInRegistries.ENTITY_TYPE.getKey(animatable.getType()).getPath();
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/dragon/" + name + ".animation.json");
	}

	private DragonAssetDefinition resolveAssets(T animatable) {
		DragonDefinition definition = animatable.getDragonDefinition();
		return definition == null ? null : definition.resolveAssetDefinition();
	}

    @Override protected float pitchLimit() { return 20; }
    @Override protected float yawLimit() { return 35; }
    @Override protected boolean additiveHeadRotation() { return true; }

}
