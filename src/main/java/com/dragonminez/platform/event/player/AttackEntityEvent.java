package com.dragonminez.platform.event.player;
import com.dragonminez.platform.event.ICancellableEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
public final class AttackEntityEvent extends PlayerEvent implements ICancellableEvent {
    private final Entity target;
    public AttackEntityEvent(Player player, Entity target) { super(player); this.target = target; }
    public Entity getTarget() { return target; }
}
