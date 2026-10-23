package com.dragonminez.common.init.item.weapons;

import com.dragonminez.common.init.item.weapons.render.TamagamiHammerRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class TamagamiHammerItem extends WeaponItem implements GeoItem {

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public TamagamiHammerItem() {
		super(300, -3.0f, 2800, 18, "tamagami_hammer");
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
	}

	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {

			private TamagamiHammerRenderer renderer;

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (this.renderer == null)
					this.renderer = new TamagamiHammerRenderer();

				return this.renderer;

			}
		});
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
