package com.dragonminez.platform.event.living;
import com.dragonminez.platform.event.ICancellableEvent;
import net.minecraft.world.entity.LivingEntity;
public final class LivingFallEvent extends LivingEvent implements ICancellableEvent {
    private float distance, damageMultiplier;
    public LivingFallEvent(LivingEntity entity, double distance, float damageMultiplier) {
        super(entity); this.distance = (float) distance; this.damageMultiplier = damageMultiplier;
    }
    public float getDistance() { return distance; }
    public void setDistance(float distance) { this.distance = distance; }
    public float getDamageMultiplier() { return damageMultiplier; }
    public void setDamageMultiplier(float multiplier) { damageMultiplier = multiplier; }
}
