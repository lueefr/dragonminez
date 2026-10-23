package com.dragonminez.server.events;

import net.minecraft.world.entity.LivingEntity;
import com.dragonminez.platform.event.SubscribeEvent;
import com.dragonminez.platform.event.living.LivingDamageEvent;
import com.dragonminez.platform.event.living.LivingDeathEvent;
import com.dragonminez.platform.event.living.LivingHealEvent;
import com.dragonminez.platform.event.living.LivingIncomingDamageEvent;

/** Guards against NaN health/damage edge cases that can brick entities. */
public class FixVanillaEvents {

	@SubscribeEvent
	public void onLivingHurt(LivingIncomingDamageEvent e) {
		float dmg = e.getAmount();
		LivingEntity le = e.getEntity();
		if (Float.isNaN(dmg)) {
			e.setAmount(0f);
			rectify(le);
		}
	}

	@SubscribeEvent
	public void onLivingDamage(LivingDamageEvent.Post e) {
		LivingEntity le = e.getEntity();
		float dmg = e.getNewDamage();
		if (Float.isNaN(dmg)) {
			rectify(le);
		}
	}

	@SubscribeEvent
	public void onAttackEntity(LivingIncomingDamageEvent e) {
		LivingEntity le = e.getEntity();
		float dmg = e.getAmount();
		if (Float.isNaN(dmg)) {
			e.setCanceled(true);
			rectify(le);
		}
	}

	@SubscribeEvent
	public void onLivingHeal(LivingHealEvent e) {
		float amount = e.getAmount();
		LivingEntity le = e.getEntity();
		if (Float.isNaN(amount)) {
			e.setCanceled(true);
			return;
		}

		if (Float.isNaN(le.getHealth())) {
			e.setCanceled(true);
			rectify(le);
		}
	}

	@SubscribeEvent
	public void onLivingDeath(LivingDeathEvent e) {
		LivingEntity le = e.getEntity();
		float hp = le.getHealth();
		if (Float.isNaN(hp)) {
			e.setCanceled(true);
			rectify(le);
		}
	}

	private void rectify(LivingEntity le) {
		le.setHealth(le.getMaxHealth());
		le.setAbsorptionAmount(0);
	}
}
