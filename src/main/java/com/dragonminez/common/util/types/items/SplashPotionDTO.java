package com.dragonminez.common.util.types.items;

import lombok.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.resources.Identifier;

import java.util.List;
import com.google.gson.GsonBuilder;
import java.util.Map;
@Getter
@Setter
@NoArgsConstructor

public class SplashPotionDTO extends PotionDTO {

	public static final String ITEM_TYPE = "splash_potion";
	private static final Identifier SPLASH_POTION_ID = Identifier.fromNamespaceAndPath("minecraft", "splash_potion");

	public SplashPotionDTO(Identifier potion, int count, List<PotionEffectDTO> mobEffects) {
		super(ITEM_TYPE, SPLASH_POTION_ID, count, potion, mobEffects);
	}
}
