package com.dragonminez.platform.event.entity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
public final class EntityLeaveLevelEvent extends EntityEvent {
    public EntityLeaveLevelEvent(Entity entity) { super(entity); }
    public Level getLevel() { return getEntity().level(); }
}
