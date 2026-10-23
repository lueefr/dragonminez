package com.dragonminez.common.util.types.items;

import lombok.*;
import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import com.dragonminez.common.util.RuntimeRegistries;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An item carrying enchantments, keyed by enchantment id.
 *
 * <p>Stored as {@code {"minecraft:sharpness": 3}}: {@code ResourceLocationTypeAdapter}
 * reads and writes map keys as strings.
 */
@Getter
@Setter
@NoArgsConstructor
public class EnchantedItemDTO extends GenericItemDTO {

	public static final String ITEM_TYPE = "enchanted_item";

	protected Map<ResourceLocation, Integer> enchantments = new HashMap<>();

	public EnchantedItemDTO(ResourceLocation itemId, int count) {
		super(ITEM_TYPE, itemId, count);
	}

	public EnchantedItemDTO(ResourceLocation itemId, int count, Map<ResourceLocation, Integer> enchantments) {
		super(ITEM_TYPE, itemId, count);
		this.enchantments = enchantments;
	}

	protected EnchantedItemDTO(String itemType, ResourceLocation itemId, int count) {
		super(itemType, itemId, count);
	}

	protected EnchantedItemDTO(String itemType, ResourceLocation itemId, int count,
			Map<ResourceLocation, Integer> enchantments) {
		super(itemType, itemId, count);
		this.enchantments = enchantments;
	}

	@Override
	public void validate() {
		super.validate();
		if (this.enchantments == null) {
			return;
		}
		this.enchantments.forEach((id, level) -> {
			if (id == null) {
				throw new JsonSyntaxException(errorPrefix() + " an enchantment id must not be null.");
			}
			if (level == null || level < 1) {
				throw new JsonSyntaxException(
						errorPrefix() + " enchantment '" + id + "' has an invalid level: " + level + ".");
			}
		});
	}

	@Override
	public ItemStack getItemStack() {
		ItemStack itemStack = super.getItemStack();
		if (itemStack.isEmpty()) {
			return itemStack;
		}
		applyEnchantments(itemStack);
		return itemStack;
	}

	protected void applyEnchantments(ItemStack itemStack) {
		Map<Holder<Enchantment>, Integer> resolved = resolveEnchantments();
		if (!resolved.isEmpty()) {
			EnchantmentHelper.updateEnchantments(itemStack, mutable -> resolved.forEach(mutable::set));
		}
	}

	/** Resolves what it can; unknown enchantments are logged and skipped, not fatal. */
	protected final Map<Holder<Enchantment>, Integer> resolveEnchantments() {
		Map<Holder<Enchantment>, Integer> resolved = new LinkedHashMap<>();
		if (this.enchantments == null) {
			return resolved;
		}
		this.enchantments.forEach((id, level) -> {
			Holder<Enchantment> enchantment = resolveEnchantment(id);
			if (enchantment != null) {
				resolved.put(enchantment, level);
			}
		});
		return resolved;
	}

	protected final Holder<Enchantment> resolveEnchantment(ResourceLocation id) {
		if (id == null) {
			return null;
		}
		Holder<Enchantment> enchantment = RuntimeRegistries.access().registry(Registries.ENCHANTMENT).flatMap(registry -> registry.getHolder(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT, id))).orElse(null);
		if (enchantment == null) {
			LogUtil.warn(Env.COMMON, "Unknown enchantment '{}' on item '{}', skipping.", id, this.itemId);
		}
		return enchantment;
	}
}
