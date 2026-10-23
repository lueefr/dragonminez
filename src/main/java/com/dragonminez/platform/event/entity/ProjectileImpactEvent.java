package com.dragonminez.platform.event.entity;
import com.dragonminez.platform.event.ICancellableEvent;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
public final class ProjectileImpactEvent extends EntityEvent implements ICancellableEvent {
    private final HitResult hit;
    public ProjectileImpactEvent(Projectile projectile, HitResult hit) { super(projectile); this.hit = hit; }
    public Projectile getProjectile() { return (Projectile) getEntity(); }
    public HitResult getRayTraceResult() { return hit; }
}
