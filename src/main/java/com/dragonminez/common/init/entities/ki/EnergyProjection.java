package com.dragonminez.common.init.entities.ki;

import com.dragonminez.api.rules.Conditions;
import com.dragonminez.common.config.*;
import com.dragonminez.common.init.*;
import com.dragonminez.common.stats.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.lang.ref.WeakReference;
import java.util.*;

/** Transient server-owned avatars follow their caster; visual size never creates an invisible obstacle. */
public final class EnergyProjection extends Entity implements GeoEntity {
    private static final EntityDataAccessor<String> DEFINITION=SynchedEntityData.defineId(EnergyProjection.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Optional<UUID>> OWNER=SynchedEntityData.defineId(EnergyProjection.class,EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Long> EXPIRY=SynchedEntityData.defineId(EnergyProjection.class,EntityDataSerializers.LONG);
    private static final Map<UUID,List<WeakReference<EnergyProjection>>> ACTIVE=new WeakHashMap<>();
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private WeakReference<Player> caster=new WeakReference<>(null);
    public EnergyProjection(EntityType<?> type,Level level){super(type,level);noPhysics=true;setNoGravity(true);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){builder.define(DEFINITION,"");builder.define(OWNER,Optional.empty());builder.define(EXPIRY,0L);}
    public ProjectionConfig.Definition definition(){return ConfigManager.getRuntime().projections().get(entityData.get(DEFINITION));}
    public Player owner(){
        var player=caster.get();if(player!=null)return player;
        return entityData.get(OWNER).map(id->level().getPlayerByUUID(id)).orElse(null);
    }
    public float fade(){return (float)Math.clamp((entityData.get(EXPIRY)-level().getGameTime())/10d,0,1);}
    public static boolean spawn(ServerPlayer player,String id){
        var definition=ConfigManager.getRuntime().projections().get(id);
        if(definition==null)throw new IllegalArgumentException("Unknown energy projection: "+id);
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElse(null);
        if(data==null||!data.getStatus().isHasCreatedCharacter()||!player.isAlive()||player.isSpectator()||data.getCooldowns().hasCooldown("RuntimeProjection:"+id)||!Conditions.test(new Conditions.Context(data,null),definition.when()))return false;
        var active=ACTIVE.computeIfAbsent(player.getUUID(),key->new ArrayList<>());
        active.removeIf(reference->{var entity=reference.get();return entity==null||entity.isRemoved();});
        if(active.size()>=4||active.stream().map(WeakReference::get).filter(Objects::nonNull).anyMatch(entity->entity.entityData.get(DEFINITION).equals(id)))return false;
        double energy=data.getMaxEnergy()*definition.energy(),stamina=data.getMaxStamina()*definition.stamina();
        if(data.getResources().getCurrentEnergy()<energy||data.getResources().getCurrentStamina()<stamina)return false;
        var projection=MainEntities.ENERGY_PROJECTION.get().create(player.level());
        projection.entityData.set(DEFINITION,id);projection.entityData.set(OWNER,Optional.of(player.getUUID()));projection.entityData.set(EXPIRY,player.level().getGameTime()+definition.duration());projection.caster=new WeakReference<>(player);
        if(!projection.follow(player,definition)||!player.serverLevel().addFreshEntity(projection))return false;
        active.add(new WeakReference<>(projection));data.getResources().removeEnergy((float)energy,false);data.getResources().removeStamina((float)stamina);
        data.getCooldowns().setCooldown("RuntimeProjection:"+id,definition.cooldown());
        return true;
    }
    private boolean follow(Player player,ProjectionConfig.Definition definition){
        var visual=definition.appearance();Vec3 forward=Vec3.directionFromRotation(0,player.getYRot());
        Vec3 point=player.position().add(forward.z*visual.x()+forward.x*visual.z(),visual.y(),-forward.x*visual.x()+forward.z*visual.z());
        var block=net.minecraft.core.BlockPos.containing(point);
        if(!level().hasChunkAt(block)||!level().getWorldBorder().isWithinBounds(block))return false;
        setPos(point);setYRot(player.yBodyRot);return true;
    }
    @Override public void tick(){
        super.tick();if(level().isClientSide)return;
        var definition=definition();var owner=owner();
        if(definition==null||!(owner instanceof ServerPlayer player)||player.isRemoved()||!player.isAlive()||player.isSpectator()||player.level()!=level()||level().getGameTime()>=entityData.get(EXPIRY)){discard();return;}
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElse(null);
        if(data==null||!data.getStatus().isHasCreatedCharacter()||!Conditions.test(new Conditions.Context(data,null),definition.when())||!follow(player,definition)){discard();return;}
        double upkeep=data.getMaxEnergy()*definition.upkeep();
        if(data.getResources().getCurrentEnergy()<upkeep){discard();return;}
        data.getResources().removeEnergy((float)upkeep,false);
        if(definition.attack()!=null&&tickCount%definition.attack().interval()==0)strike(player,data,definition.attack());
    }
    private void strike(ServerPlayer player,StatsData data,ProjectionConfig.Attack attack){
        double base=attack.stat().equals("STR")?data.getStats().getStrength():data.getStats().getKiPower();
        double damage=base*data.getTotalMultiplier(attack.stat())*attack.factor();
        if(!Double.isFinite(damage)||damage<=0)return;
        Vec3 origin=getBoundingBoxForCulling().getCenter();
        var targets=level().getEntitiesOfClass(LivingEntity.class,new AABB(origin,origin).inflate(attack.radius()),target->target!=player&&target.isAlive()&&!target.isSpectator()&&player.distanceToSqr(target)<=attack.range()*attack.range()&&target.getBoundingBox().getCenter().distanceToSqr(origin)<=attack.radius()*attack.radius());
        targets.sort(Comparator.comparingDouble((LivingEntity target)->target.distanceToSqr(this)).thenComparing(Entity::getUUID));
        int count=0;
        for(var target:targets){
            var relation=com.dragonminez.common.combat.logic.player.TargetHelper.getRelation(player,target);
            if(relation==com.dragonminez.common.combat.logic.player.TargetHelper.Relation.FRIENDLY||attack.targeting().equals("hostile")&&relation!=com.dragonminez.common.combat.logic.player.TargetHelper.Relation.HOSTILE)continue;
            if(attack.targeting().equals("aim")&&player.getLookAngle().dot(target.getBoundingBox().getCenter().subtract(player.getEyePosition()).normalize())<Math.cos(Math.toRadians(attack.angle())))continue;
            if(target instanceof Player victim&&(!player.getServer().isPvpAllowed()||!player.canHarmPlayer(victim)))continue;
            var start=net.minecraft.core.BlockPos.containing(origin);var end=net.minecraft.core.BlockPos.containing(target.getBoundingBox().getCenter());
            if(!level().hasChunksAt(Math.min(start.getX(),end.getX()),Math.min(start.getZ(),end.getZ()),Math.max(start.getX(),end.getX()),Math.max(start.getZ(),end.getZ())))continue;
            var hit=level().clip(new net.minecraft.world.level.ClipContext(origin,target.getBoundingBox().getCenter(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,this));
            if(hit.getType()!=HitResult.Type.MISS)continue;
            if(target.hurt(MainDamageTypes.strikeAttack(level(),player,null),(float)Math.min(Float.MAX_VALUE,damage))){
                for(var effect:attack.effects()){
                    var type=net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(net.minecraft.resources.ResourceLocation.parse(effect.id())).orElseThrow();
                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(type,effect.ticks(),effect.amplifier()),player);
                }
            }
            if(++count>=attack.targets())break;
        }
    }
    @Override public AABB getBoundingBoxForCulling(){var definition=definition();double scale=definition==null?1:definition.appearance().scale();return new AABB(getX()-.3*scale,getY(),getZ()-.3*scale,getX()+.3*scale,getY()+1.8*scale,getZ()+.3*scale);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){discard();}
    @Override protected void addAdditionalSaveData(CompoundTag tag){}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){controllers.add(new AnimationController<>(this,"projection",0,state->{var definition=definition();return definition==null?PlayState.STOP:state.setAndContinue(RawAnimation.begin().thenLoop(definition.appearance().clip()));}));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
