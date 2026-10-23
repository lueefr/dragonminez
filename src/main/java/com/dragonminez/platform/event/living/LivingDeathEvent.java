package com.dragonminez.platform.event.living;
import com.dragonminez.platform.event.ICancellableEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
public final class LivingDeathEvent extends LivingEvent implements ICancellableEvent {
    private final DamageSource source;
    public LivingDeathEvent(LivingEntity entity, DamageSource source) { super(entity); this.source = source; }
    public DamageSource getSource() { return source; }
}
