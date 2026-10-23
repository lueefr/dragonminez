package com.dragonminez.common.init.entities.animal;

import com.dragonminez.common.init.MainSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

public class NamekFrogGinyuEntity extends NamekFrogEntity{

    private AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public NamekFrogGinyuEntity(EntityType<? extends Animal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new BreathAirGoal(this));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5D));
    }

    public static AttributeSupplier createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22F)
                .build();
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("base_controller", 5, this::walkPredicate));
    }

    private <T extends GeoAnimatable> PlayState walkPredicate(AnimationTest<T> event) {
        NamekFrogGinyuEntity entity = (NamekFrogGinyuEntity) event.animatable();

        if (event.isMoving()) {
            event.controller().setAnimation(RawAnimation.begin().thenLoop("walkginyu"));
            return PlayState.CONTINUE;
        }

        event.controller().setAnimation(RawAnimation.begin().thenLoop("idleginyu"));
        return PlayState.CONTINUE;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        RandomSource random = this.level().random;

        if (random.nextInt(5) == 0) {
            this.playSound(MainSounds.FROG_LAUGH.get(), 1.0F, 1.0F);
        }
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        RandomSource random = this.level().random;
        int choice = random.nextInt(3);

        return switch (choice) {
            case 0 -> MainSounds.FROG1.get();
            case 1 -> MainSounds.FROG2.get();
            case 2 -> MainSounds.FROG3.get();
            default -> null;
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
