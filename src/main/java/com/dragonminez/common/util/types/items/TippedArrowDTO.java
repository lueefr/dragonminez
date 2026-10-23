package com.dragonminez.common.util.types.items;

import lombok.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.resources.Identifier;

import java.util.List;

/** A tipped arrow. Carries the same potion tags as a potion; see {@link PotionDTO}. */
@Getter
@Setter
@NoArgsConstructor
public class TippedArrowDTO extends PotionDTO {

	public static final String ITEM_TYPE = "tipped_arrow";
	private static final Identifier TIPPED_ARROW_ID = Identifier.fromNamespaceAndPath("minecraft", "tipped_arrow");

	public TippedArrowDTO(Identifier potion, int count, List<PotionEffectDTO> mobEffects) {
		super(ITEM_TYPE, TIPPED_ARROW_ID, count, potion, mobEffects);
	}
}
