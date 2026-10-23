package com.dragonminez.platform.event.living;
import com.dragonminez.platform.event.ICancellableEvent;
import net.minecraft.world.entity.LivingEntity;
public final class LivingHealEvent extends LivingEvent implements ICancellableEvent {
    private float amount;
    public LivingHealEvent(LivingEntity entity, float amount) { super(entity); this.amount = amount; }
    public float getAmount() { return amount; }
    public void setAmount(float amount) { this.amount = amount; }
}
