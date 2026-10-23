package com.dragonminez.common.util.types.items;

import lombok.*;
import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import com.google.gson.GsonBuilder;
import net.minecraft.world.item.ItemStack;
import java.util.Map;

/**
 * An armour piece with a smithing trim, and optionally enchantments.
 */
@Getter
@Setter
@NoArgsConstructor
public class TrimmedArmorDTO extends EnchantedItemDTO {

	public static final String ITEM_TYPE = "trimmed_armor";

	protected ResourceLocation material;
	protected ResourceLocation pattern;

	public TrimmedArmorDTO(ResourceLocation itemId, ResourceLocation material, ResourceLocation pattern) {
		super(ITEM_TYPE, itemId, 1);
		this.material = material;
		this.pattern = pattern;
	}

	public TrimmedArmorDTO(ResourceLocation itemId, Map<ResourceLocation, Integer> enchantments,
			ResourceLocation material, ResourceLocation pattern) {
		super(ITEM_TYPE, itemId, 1, enchantments);
		this.material = material;
		this.pattern = pattern;
	}

	@Override
	public void validate() {
		super.validate();
		if (this.material == null) {
			throw new JsonSyntaxException(errorPrefix() + " 'material' must not be null.");
		}
		if (this.pattern == null) {
			throw new JsonSyntaxException(errorPrefix() + " 'pattern' must not be null.");
		}
	}

	@Override
	public ItemStack getItemStack() {
		ItemStack itemStack = super.getItemStack();
		if (itemStack.isEmpty()) {
			return itemStack;
		}
		applyTrim(itemStack);
		return itemStack;
	}

	protected void applyTrim(ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof ArmorItem)) {
			LogUtil.warn(Env.COMMON, "Item '{}' is not armour, skipping its trim.", this.itemId);
			return;
		}
		if (this.material == null || this.pattern == null) {
			return;
		}

        var registries = com.dragonminez.common.util.RuntimeRegistries.access();
        var material = registries.registry(net.minecraft.core.registries.Registries.TRIM_MATERIAL)
                .flatMap(registry -> registry.getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.TRIM_MATERIAL, this.material)));
        var pattern = registries.registry(net.minecraft.core.registries.Registries.TRIM_PATTERN)
                .flatMap(registry -> registry.getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.TRIM_PATTERN, this.pattern)));
        if (material.isPresent() && pattern.isPresent()) itemStack.set(net.minecraft.core.component.DataComponents.TRIM,
                new net.minecraft.world.item.armortrim.ArmorTrim(material.get(), pattern.get()));
        else LogUtil.warn(Env.COMMON, "Unknown trim material '{}' or pattern '{}' on item '{}'", this.material, this.pattern, this.itemId);
    }
}
