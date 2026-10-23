package com.dragonminez.platform.event.living;
import com.dragonminez.platform.event.ICancellableEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
public final class LivingIncomingDamageEvent extends LivingEvent implements ICancellableEvent {
    private final DamageSource source;
    private final float originalAmount;
    private float amount;
    public LivingIncomingDamageEvent(LivingEntity entity, DamageSource source, float amount) {
        super(entity); this.source = source; this.originalAmount = this.amount = amount;
    }
    public DamageSource getSource() { return source; }
    public float getOriginalAmount() { return originalAmount; }
    public float getAmount() { return amount; }
    public void setAmount(float amount) { this.amount = amount; }
}
