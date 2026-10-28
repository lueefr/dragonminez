package com.dragonminez.common.combat.aim;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import java.util.Map;
import java.util.WeakHashMap;

/** Camera input is a ray, never an accepted hit or a damage/permission decision. */
public final class KiAim {
    public static final double RANGE=256,MAX_CAMERA_OFFSET=64;
    private record Sample(Vec3 origin,Vec3 direction,int lock,long tick,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {}
    private static final Map<ServerPlayer,Sample> SAMPLES=new WeakHashMap<>();
    private KiAim() {}
    public static boolean validRay(Vec3 origin,Vec3 direction,Vec3 eye) {
        return finite(origin)&&finite(direction)&&origin.distanceToSqr(eye)<=MAX_CAMERA_OFFSET*MAX_CAMERA_OFFSET
            &&direction.lengthSqr()>=0.99&&direction.lengthSqr()<=1.01;
    }
    private static boolean finite(Vec3 p){return Double.isFinite(p.x)&&Double.isFinite(p.y)&&Double.isFinite(p.z);}
    public static void accept(ServerPlayer player,Vec3 origin,Vec3 direction,int lock) {
        if(!player.isAlive()||player.isSpectator()||!validRay(origin,direction,player.getEyePosition()))return;
        // A camera behind a wall is clipped back to the visible side before tracing its aim.
        var clipped=player.level().clip(new ClipContext(player.getEyePosition(),origin,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        if(clipped.getType()!=net.minecraft.world.phys.HitResult.Type.MISS)origin=clipped.getLocation();
        int validatedLock=validLock(player,lock)==null?-1:lock;
        StatsProvider.get(StatsCapability.INSTANCE,player).ifPresent(data->data.getTechniques().setHomingTargetId(validatedLock));
        SAMPLES.put(player,new Sample(origin,direction.normalize(),validatedLock,player.level().getGameTime(),player.level().dimension()));
    }
    public static LivingEntity validLock(ServerPlayer player,int id) {
        if(id<0||!(player.serverLevel().getEntity(id) instanceof LivingEntity target)||target==player||!target.isAlive()||target.isSpectator())return null;
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElse(null);if(data==null)return null;
        int sense=data.getSkills().getSkillLevel("kisense");if(sense<=0)return null;
        double range=15+5*sense+(data.getStatus().isAndroidUpgraded()?25:0);
        return player.distanceToSqr(target)<=range*range&&(player.hasLineOfSight(target)||data.getStatus().isAndroidUpgraded())?target:null;
    }
    public static Vec3 trace(LivingEntity owner,Vec3 origin,Vec3 direction) {
        Vec3 end=origin.add(direction.scale(RANGE));
        var block=owner.level().clip(new ClipContext(origin,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,owner));
        end=block.getLocation();
        var bounds=new net.minecraft.world.phys.AABB(origin,end).inflate(1);
        var entity=ProjectileUtil.getEntityHitResult(owner.level(),owner,origin,end,bounds,e->e!=owner&&!e.isSpectator()&&e.isPickable()&&!(e instanceof com.dragonminez.common.init.entities.ki.AbstractKiProjectile),0.0F);
        return entity==null?end:entity.getLocation();
    }
    public static Vec3 target(LivingEntity owner) {
        if(owner instanceof ServerPlayer player) {
            var sample=SAMPLES.get(player);
            if(sample!=null&&sample.dimension().equals(player.level().dimension())&&player.level().getGameTime()-sample.tick()<=10) {
                var locked=validLock(player,sample.lock());
                if(locked!=null)return locked.getBoundingBox().getCenter();
                return trace(owner,sample.origin(),sample.direction());
            }
            var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElse(null);
            var locked=data==null?null:validLock(player,data.getTechniques().getHomingTargetId());
            if(locked!=null)return locked.getBoundingBox().getCenter();
        }
        return trace(owner,owner.getEyePosition(),owner.getLookAngle());
    }
    public static Vec3 direction(LivingEntity owner,Vec3 origin) {
        if(!(owner instanceof ServerPlayer player))return owner.getLookAngle();
        var sample=SAMPLES.get(player);
        boolean fresh=sample!=null&&sample.dimension().equals(player.level().dimension())&&player.level().getGameTime()-sample.tick()<=10;
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElse(null);
        if(!fresh&&(data==null||validLock(player,data.getTechniques().getHomingTargetId())==null))return owner.getLookAngle();
        Vec3 difference=target(owner).subtract(origin);
        return difference.lengthSqr()<1e-10?owner.getLookAngle():difference.normalize();
    }
}
