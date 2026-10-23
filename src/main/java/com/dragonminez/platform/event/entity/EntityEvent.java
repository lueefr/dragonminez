package com.dragonminez.platform.event.entity;
import com.dragonminez.platform.event.Event;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
public abstract class EntityEvent extends Event {
    private final Entity entity;
    protected EntityEvent(Entity entity) { this.entity = java.util.Objects.requireNonNull(entity); }
    public Entity getEntity() { return entity; }
    public static final class Size extends EntityEvent {
        private final Pose pose;
        private final EntityDimensions oldSize;
        private EntityDimensions newSize;
        public Size(Entity entity, Pose pose, EntityDimensions size) {
            super(entity); this.pose = pose; this.oldSize = size; this.newSize = size;
        }
        public Pose getPose() { return pose; }
        public EntityDimensions getOldSize() { return oldSize; }
        public EntityDimensions getNewSize() { return newSize; }
        public void setNewSize(EntityDimensions size) { newSize = java.util.Objects.requireNonNull(size); }
    }
}
