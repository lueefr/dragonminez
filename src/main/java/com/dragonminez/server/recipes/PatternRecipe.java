package com.dragonminez.server.recipes;

import com.dragonminez.common.init.MainRecipes;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public class PatternRecipe implements Recipe<net.minecraft.world.item.crafting.CraftingInput> {
	public static final int MAX_SIZE = 6;

	private final ResourceLocation id;
	private final String group;
	private final int width;
	private final int height;
	private final NonNullList<Ingredient> ingredients;
	private final ItemStack result;
	private final boolean showNotification;

	public PatternRecipe(ResourceLocation id, String group, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, boolean showNotification) {
		this.id = id;
		this.group = group;
		this.width = width;
		this.height = height;
		this.ingredients = ingredients;
		this.result = result;
		this.showNotification = showNotification;
	}

	@Override
	public boolean matches(net.minecraft.world.item.crafting.CraftingInput container, Level level) {
		for (int x = 0; x <= container.width() - this.width; ++x) {
			for (int y = 0; y <= container.height() - this.height; ++y) {
				if (this.matches(container, x, y, true)) return true;
				if (this.matches(container, x, y, false)) return true;
			}
		}
		return false;
	}

	private boolean matches(net.minecraft.world.item.crafting.CraftingInput container, int offsetX, int offsetY, boolean mirrored) {
		for (int i = 0; i < container.width(); ++i) {
			for (int j = 0; j < container.height(); ++j) {
				int k = i - offsetX;
				int l = j - offsetY;
				Ingredient ingredient = Ingredient.EMPTY;
				if (k >= 0 && l >= 0 && k < this.width && l < this.height) {
					ingredient = mirrored
							? this.ingredients.get(this.width - k - 1 + l * this.width)
							: this.ingredients.get(k + l * this.width);
				}
				if (!ingredient.test(container.getItem(i + j * container.width()))) return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack assemble(net.minecraft.world.item.crafting.CraftingInput container, net.minecraft.core.HolderLookup.Provider registryAccess) {
		return this.result.copy();
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width >= this.width && height >= this.height;
	}

	@Override
	public ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider registryAccess) {
		return this.result;
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return this.ingredients;
	}

	@Override
	public String getGroup() {
		return this.group;
	}

	@Override
	public boolean showNotification() {
		return this.showNotification;
	}

	@Override
	public boolean isIncomplete() {
		NonNullList<Ingredient> list = this.getIngredients();
		return list.isEmpty() || list.stream().filter(ing -> !ing.isEmpty()).anyMatch(ingredient -> ingredient.getItems().length == 0);
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	public ResourceLocation getId() {
		return this.id;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return MainRecipes.PATTERN_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return MainRecipes.PATTERN_TYPE.get();
	}

    public static PatternRecipe create(String group, java.util.List<String> rows, Map<String, Ingredient> key, ItemStack result, boolean notification) {
        String[] pattern = Serializer.shrink(rows.toArray(String[]::new));
        if (pattern.length == 0 || pattern.length > MAX_SIZE || pattern[0].length() > MAX_SIZE) throw new IllegalArgumentException("Invalid pattern dimensions");
        Map<String, Ingredient> ingredients = new java.util.HashMap<>(key);
        ingredients.put(" ", Ingredient.EMPTY);
        return new PatternRecipe(ResourceLocation.fromNamespaceAndPath("dragonminez", "pattern"), group, pattern[0].length(), pattern.length,
                Serializer.dissolvePattern(pattern, ingredients, pattern[0].length(), pattern.length), result, notification);
    }

    public static class Serializer implements RecipeSerializer<PatternRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        private record Data(String group, Map<String, Ingredient> key, java.util.List<String> pattern, ItemStack result, boolean notification) {}
        private static final com.mojang.serialization.MapCodec<Data> DATA = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance -> instance.group(
                com.mojang.serialization.Codec.STRING.optionalFieldOf("group", "").forGetter(Data::group),
                com.mojang.serialization.Codec.unboundedMap(com.mojang.serialization.Codec.STRING, Ingredient.CODEC_NONEMPTY).fieldOf("key").forGetter(Data::key),
                com.mojang.serialization.Codec.STRING.listOf().fieldOf("pattern").forGetter(Data::pattern),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(Data::result),
                com.mojang.serialization.Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(Data::notification)
        ).apply(instance, Data::new));
        public static final com.mojang.serialization.MapCodec<PatternRecipe> CODEC = DATA.xmap(
                data -> create(data.group(), data.pattern(), data.key(), data.result(), data.notification()),
                recipe -> {
                    Map<String, Ingredient> key = new java.util.LinkedHashMap<>();
                    java.util.List<String> pattern = new java.util.ArrayList<>();
                    String symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
                    for (int y = 0; y < recipe.height; y++) {
                        StringBuilder row = new StringBuilder();
                        for (int x = 0; x < recipe.width; x++) {
                            int index = x + y * recipe.width;
                            Ingredient ingredient = recipe.ingredients.get(index);
                            if (ingredient.isEmpty()) row.append(' ');
                            else {
                                String symbol = String.valueOf(symbols.charAt(index));
                                key.put(symbol, ingredient);
                                row.append(symbol);
                            }
                        }
                        pattern.add(row.toString());
                    }
                    return new Data(recipe.group, key, pattern, recipe.result, recipe.showNotification);
                });
        private static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, PatternRecipe> STREAM_CODEC = net.minecraft.network.codec.ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
        @Override public com.mojang.serialization.MapCodec<PatternRecipe> codec() { return CODEC; }
        @Override public net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, PatternRecipe> streamCodec() { return STREAM_CODEC; }

		private static NonNullList<Ingredient> dissolvePattern(String[] pattern, Map<String, Ingredient> key, int width, int height) {
			NonNullList<Ingredient> list = NonNullList.withSize(width * height, Ingredient.EMPTY);
			Set<String> unused = Sets.newHashSet(key.keySet());
			unused.remove(" ");
			for (int row = 0; row < pattern.length; ++row) {
				for (int col = 0; col < pattern[row].length(); ++col) {
					String s = pattern[row].substring(col, col + 1);
					Ingredient ingredient = key.get(s);
					if (ingredient == null) {
						throw new JsonSyntaxException("Pattern references symbol '" + s + "' but it's not defined in the key");
					}
					unused.remove(s);
					list.set(col + width * row, ingredient);
				}
			}
			if (!unused.isEmpty()) {
				throw new JsonSyntaxException("Key defines symbols that aren't used in pattern: " + unused);
			}
			return list;
		}

		private static String[] shrink(String... rows) {
			int first = Integer.MAX_VALUE;
			int last = 0;
			int emptyTop = 0;
			int emptyBottom = 0;
			for (int i = 0; i < rows.length; ++i) {
				String row = rows[i];
				first = Math.min(first, firstNonSpace(row));
				int lastIdx = lastNonSpace(row);
				last = Math.max(last, lastIdx);
				if (lastIdx < 0) {
					if (emptyTop == i) ++emptyTop;
					++emptyBottom;
				} else {
					emptyBottom = 0;
				}
			}
			if (rows.length == emptyBottom) return new String[0];
			String[] result = new String[rows.length - emptyBottom - emptyTop];
			for (int i = 0; i < result.length; ++i) {
				result[i] = rows[i + emptyTop].substring(first, last + 1);
			}
			return result;
		}

		private static int firstNonSpace(String s) {
			int i = 0;
			while (i < s.length() && s.charAt(i) == ' ') ++i;
			return i;
		}

		private static int lastNonSpace(String s) {
			int i = s.length() - 1;
			while (i >= 0 && s.charAt(i) == ' ') --i;
			return i;
		}

		private static String[] patternFromJson(JsonArray array) {
			String[] rows = new String[array.size()];
			if (rows.length > MAX_SIZE) {
				throw new JsonSyntaxException("Invalid pattern: too many rows, " + MAX_SIZE + " is maximum");
			} else if (rows.length == 0) {
				throw new JsonSyntaxException("Invalid pattern: empty pattern not allowed");
			}
			for (int i = 0; i < rows.length; ++i) {
				String row = GsonHelper.convertToString(array.get(i), "pattern[" + i + "]");
				if (row.length() > MAX_SIZE) {
					throw new JsonSyntaxException("Invalid pattern: too many columns, " + MAX_SIZE + " is maximum");
				}
				if (i > 0 && rows[0].length() != row.length()) {
					throw new JsonSyntaxException("Invalid pattern: each row must be the same width");
				}
				rows[i] = row;
			}
			return rows;
		}

		private static Map<String, Ingredient> keyFromJson(JsonObject json) {
			Map<String, Ingredient> map = Maps.newHashMap();
			for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
				if (entry.getKey().length() != 1) {
					throw new JsonSyntaxException("Invalid key entry: '" + entry.getKey() + "' is an invalid symbol (must be 1 character only).");
				}
				if (" ".equals(entry.getKey())) {
					throw new JsonSyntaxException("Invalid key entry: ' ' is a reserved symbol.");
				}
				map.put(entry.getKey(), Ingredient.CODEC_NONEMPTY.parse(com.mojang.serialization.JsonOps.INSTANCE, entry.getValue()).getOrThrow());
			}
			map.put(" ", Ingredient.EMPTY);
			return map;
		}
	}
}
