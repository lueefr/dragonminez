package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.server.recipes.KikonoRecipe;
import com.dragonminez.server.recipes.PatternRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import com.dragonminez.platform.registry.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import com.dragonminez.platform.registry.DeferredHolder;

public class MainRecipes {
	public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
			DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Reference.MOD_ID);
	public static final DeferredRegister<RecipeType<?>> TYPES =
			DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Reference.MOD_ID);

	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<KikonoRecipe>> KIKONO_SERIALIZER =
			SERIALIZERS.register("kikono_crafting", () -> KikonoRecipe.Serializer.INSTANCE);

	public static final DeferredHolder<RecipeType<?>, RecipeType<KikonoRecipe>> KIKONO_TYPE =
			TYPES.register("kikono_crafting", () -> new RecipeType<KikonoRecipe>() {
				@Override
				public String toString() {
					return "kikono_crafting";
				}
			});

	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PatternRecipe>> PATTERN_SERIALIZER =
			SERIALIZERS.register("pattern_crafting", () -> PatternRecipe.Serializer.INSTANCE);

	public static final DeferredHolder<RecipeType<?>, RecipeType<PatternRecipe>> PATTERN_TYPE =
			TYPES.register("pattern_crafting", () -> new RecipeType<PatternRecipe>() {
				@Override
				public String toString() {
					return "pattern_crafting";
				}
			});

	public static void register() {
		SERIALIZERS.register();
		TYPES.register();
	}
}
