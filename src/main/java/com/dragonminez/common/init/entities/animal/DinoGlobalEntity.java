package com.dragonminez.common.init.entities.animal;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import com.dragonminez.common.init.entities.ai.EnemyMeleeBrainGoal;
import com.dragonminez.common.init.entities.ai.PartyAwareTargetGoal;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.shapes.CollisionContext;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

public class DinoGlobalEntity extends Monster implements GeoEntity {

    private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);
    private boolean isAttacking = false;

    protected DinoGlobalEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));

        this.goalSelector.addGoal(2, new EnemyMeleeBrainGoal(this, 1.1D, false, true, false));

        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));

        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 15.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(DinoGlobalEntity.class));
        this.targetSelector.addGoal(2, new PartyAwareTargetGoal(this, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Animal.class, true));

    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

        controllers.add(new AnimationController<>("base_controller", 5, this::walkPredicate));

        controllers.add(new AnimationController<>("attack_controller", 0, this::attackPredicate));

        controllers.add(new AnimationController<>("tail_controller", 0, this::tailPredicate));
    }

    private <T extends GeoAnimatable> PlayState walkPredicate(AnimationTest<T> event) {
        if (event.isMoving()) {
            event.controller().setAnimation(RawAnimation.begin().thenLoop("walk"));
            return PlayState.CONTINUE;
        }
        event.controller().setAnimation(RawAnimation.begin().thenLoop("idle"));
        return PlayState.CONTINUE;
    }

    private <T extends GeoAnimatable> PlayState attackPredicate(AnimationTest<T> event) {
        DinoGlobalEntity entity = (DinoGlobalEntity) event.animatable();

        if (entity.swingTime > 0 && !isAttacking) {
            isAttacking = true;

            event.controller().forceAnimationReset();

            event.controller().setAnimation(RawAnimation.begin().thenPlay("attack"));

            return PlayState.CONTINUE;
        }

        if (isAttacking) {
            if (event.controller().getAnimationState() == AnimationController.State.STOPPED) {
                isAttacking = false;
                return PlayState.STOP;
            }
            return PlayState.CONTINUE;
        }

        return PlayState.STOP;
    }

    private <T extends GeoAnimatable> PlayState tailPredicate(AnimationTest<T> event) {

        event.controller().setAnimation(RawAnimation.begin().thenLoop("tail"));

        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor pLevel, EntitySpawnReason reason) {
        return pLevel.getDifficulty() != Difficulty.PEACEFUL && this.checkSpawnObstruction(pLevel);
    }

    public static boolean canSpawnHere(EntityType<? extends DinoGlobalEntity> entity, ServerLevelAccessor world, EntitySpawnReason spawn, BlockPos pos, RandomSource random) {
        if (world.getDifficulty() == Difficulty.PEACEFUL) return false;
        if (random.nextFloat() < 0.65f) return false;

        if (world.getBrightness(LightLayer.BLOCK, pos) > 7) return false;

        boolean solidGround = world.getBlockState(pos.below()).isSolidRender(world, pos.below());
        boolean noCollision = world.isUnobstructed(world.getBlockState(pos), pos, CollisionContext.empty());

        return solidGround && noCollision;
    }
}
