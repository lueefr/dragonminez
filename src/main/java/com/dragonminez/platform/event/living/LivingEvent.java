package com.dragonminez.platform.event.living;
import com.dragonminez.platform.event.Event;
import net.minecraft.world.entity.LivingEntity;
public abstract class LivingEvent extends Event {
    private final LivingEntity entity;
    protected LivingEvent(LivingEntity entity) { this.entity = java.util.Objects.requireNonNull(entity); }
    public LivingEntity getEntity() { return entity; }
    public static final class LivingJumpEvent extends LivingEvent {
        public LivingJumpEvent(LivingEntity entity) { super(entity); }
    }
}
