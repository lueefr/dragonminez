package com.dragonminez.server.events.players.combat;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainEnchants;
import com.dragonminez.server.events.players.TickHandler;
import com.dragonminez.platform.event.living.LivingKnockBackEvent;
import com.dragonminez.platform.event.SubscribeEvent;
import com.dragonminez.platform.event.EventBusSubscriber;

/**
 * Gameplay effects for Bulma's Gete-tech armor enchantments (Pillar IV).
 * <ul>
 *   <li><b>Gravity Forged</b> — reduces incoming knockback while worn.</li>
 * </ul>
 * (Ki Conductivity is handled in {@link TickHandler}'s ki/energy regen.) Levels are summed across all
 * four armor slots, matching the recovery enchantments.
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public class GeteEnchantmentEvents {

	@SubscribeEvent
	public static void onKnockback(LivingKnockBackEvent event) {
		int level = TickHandler.getTotalArmorEnchantmentLevel(MainEnchants.GRAVITY_FORGED, event.getEntity());
		if (level <= 0) return;
		double factor = Math.max(0.4, 1.0 - level * 0.15);
		event.setStrength((float) (event.getStrength() * factor));
	}
}
