package com.dragonminez.platform.event.living;
import com.dragonminez.platform.event.ICancellableEvent;
import net.minecraft.world.entity.LivingEntity;
public final class LivingChangeTargetEvent extends LivingEvent implements ICancellableEvent {
    private LivingEntity target;
    public LivingChangeTargetEvent(LivingEntity entity, LivingEntity target) { super(entity); this.target = target; }
    public LivingEntity getNewAboutToBeSetTarget() { return target; }
    public void setNewAboutToBeSetTarget(LivingEntity target) { this.target = target; }
}
