package com.dragonminez.platform.event.entity;
import com.dragonminez.platform.event.ICancellableEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
public final class EntityJoinLevelEvent extends EntityEvent implements ICancellableEvent {
    private final boolean loaded;
    public EntityJoinLevelEvent(Entity entity, boolean loaded) { super(entity); this.loaded = loaded; }
    public Level getLevel() { return getEntity().level(); }
    public boolean loadedFromDisk() { return loaded; }
}
