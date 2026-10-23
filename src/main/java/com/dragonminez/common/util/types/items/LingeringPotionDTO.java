package com.dragonminez.common.util.types.items;

import lombok.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.resources.Identifier;

import java.util.List;
@Getter
@Setter
@NoArgsConstructor

public class LingeringPotionDTO extends PotionDTO {

	public static final String ITEM_TYPE = "lingering_potion";
	private static final Identifier LINGERING_POTION_ID = Identifier.fromNamespaceAndPath("minecraft", "lingering_potion");

	public LingeringPotionDTO(Identifier potion, int count, List<PotionEffectDTO> mobEffects) {
		super(ITEM_TYPE, LINGERING_POTION_ID, count, potion, mobEffects);
	}
}
