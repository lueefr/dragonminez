package com.dragonminez.common.util.types.items;

import lombok.*;
import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.util.gson.PotionEffectListTypeAdapter;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.JsonAdapter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.ArrayList;
import java.util.List;

/**
 * A potion: a base potion id, custom effects, or both.
 *
 * <p>Subclassed by the splash, lingering and tipped-arrow variants. The potion NBT
 * ({@code Potion} and {@code custom_potion_effects}) is written for any item, not only a
 * {@code PotionItem}: {@code Items.TIPPED_ARROW} is a {@code TippedArrowItem extends
 * ArrowItem} and reads the same tags.
 */
@Getter
@Setter
@NoArgsConstructor
public class PotionDTO extends GenericItemDTO {

	public static final String ITEM_TYPE = "potion";
	private static final Identifier POTION_ITEM_ID = Identifier.fromNamespaceAndPath("minecraft", "potion");

	protected Identifier potion;

	@JsonAdapter(PotionEffectListTypeAdapter.class)
	protected List<PotionEffectDTO> mobEffects = new ArrayList<>();

	public PotionDTO(Identifier potion, List<PotionEffectDTO> mobEffects) {
		this(ITEM_TYPE, POTION_ITEM_ID, 1, potion, mobEffects);
	}

	protected PotionDTO(String itemType, Identifier itemId, int count, Identifier potion,
			List<PotionEffectDTO> mobEffects) {
		super(itemType, itemId, count);
		this.potion = potion;
		this.mobEffects = mobEffects;
	}

	@Override
	public void validate() {
		super.validate();
		if (this.mobEffects == null) {
			return;
		}
		for (PotionEffectDTO effect : this.mobEffects) {
			if (effect == null) {
				throw new JsonSyntaxException(errorPrefix() + " 'mobEffects' contains a null entry.");
			}
			effect.validate(errorPrefix());
		}
	}

	@Override
	public ItemStack getItemStack() {
		ItemStack itemStack = super.getItemStack();
		if (itemStack.isEmpty()) {
			return itemStack;
		}
		applyPotionData(itemStack);
		return itemStack;
	}

	protected void applyPotionData(ItemStack itemStack) {
		if (this.potion != null) {
			Potion resolved = BuiltInRegistries.POTION.getValue(this.potion);
			if (resolved == null) {
				LogUtil.warn(Env.COMMON, "Unknown potion '{}' on item '{}', skipping.", this.potion, this.itemId);
			} else {
				itemStack.set(DataComponents.POTION_CONTENTS, new PotionContents(BuiltInRegistries.POTION.wrapAsHolder(resolved)));
			}
		}

		List<MobEffectInstance> custom = resolveMobEffects();
		if (!custom.isEmpty()) {
			itemStack.update(DataComponents.POTION_CONTENTS, PotionContents.EMPTY, contents -> new PotionContents(contents.potion(), contents.customColor(), custom));
		}
	}

	/** Resolves what it can; unknown effects are logged and skipped, not fatal. */
	protected final List<MobEffectInstance> resolveMobEffects() {
		List<MobEffectInstance> resolved = new ArrayList<>();
		if (this.mobEffects == null) {
			return resolved;
		}
		for (PotionEffectDTO entry : this.mobEffects) {
			if (entry == null || entry.getEffect() == null) {
				continue;
			}
			MobEffect effect = BuiltInRegistries.MOB_EFFECT.getValue(entry.getEffect());
			if (effect == null) {
				LogUtil.warn(Env.COMMON, "Unknown mob effect '{}' on item '{}', skipping.",
						entry.getEffect(), this.itemId);
				continue;
			}
			resolved.add(new MobEffectInstance(
					BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
					entry.getDuration() == null ? 1 : entry.getDuration(),
					entry.getAmplifier() == null ? 0 : entry.getAmplifier(),
					Boolean.TRUE.equals(entry.getAmbient()),
					!Boolean.FALSE.equals(entry.getVisible()),
					!Boolean.FALSE.equals(entry.getShowIcon())
			));
		}
		return resolved;
	}
}
