package com.dragonminez.common.init;

import com.dragonminez.Reference;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.core.component.DataComponents;
import com.dragonminez.platform.registry.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import com.dragonminez.platform.registry.DeferredHolder;

public final class MainPotions {

	public static final DeferredRegister<Potion> POTION_REGISTER =
			DeferredRegister.create(BuiltInRegistries.POTION, Reference.MOD_ID);

	public static final int CURATIVE_COLOR = 0x9DFF00;

	public static final DeferredHolder<Potion, Potion> CURATIVE = POTION_REGISTER.register("curative",
			() -> new Potion("curative", new MobEffectInstance(MobEffects.REGENERATION, 100, 4)));

	public static ItemStack createCurativeFlask() {
		ItemStack stack = PotionContents.createItemStack(Items.POTION, CURATIVE);
		stack.update(DataComponents.POTION_CONTENTS, PotionContents.EMPTY, contents -> new PotionContents(contents.potion(), java.util.Optional.of(CURATIVE_COLOR), contents.customEffects()));
		return stack;
	}

	public static void register() {
		POTION_REGISTER.register();
	}
}
