package com.dragonminez.common.init.entities.animal;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

public class NamekFrogEntity extends Animal implements GeoEntity {

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(NamekFrogEntity.class, EntityDataSerializers.INT);

    public static final int VARIANT_COUNT = 3;

    public static final Identifier[] TEXTURES = new Identifier[VARIANT_COUNT];

    static {
        for (int i = 0; i < VARIANT_COUNT; i++) {
            TEXTURES[i] = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/animal/namekfrog_" + i + ".png");
        }
    }

    private AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);


    public NamekFrogEntity(EntityType<? extends Animal> pEntityType, Level pLevel) {
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
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22F)
                .build();
    }

    public int getVariant() {
        return this.entityData.get(VARIANT);
    }
    public void setVariant(int variant) {
        this.entityData.set(VARIANT, variant);
    }

    public Identifier getCurrentTexture() {
        int variant = getVariant();
        if (variant < 0 || variant >= TEXTURES.length) {
            variant = 0;
        }
        return TEXTURES[variant];
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
        return null;
    }
    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput dmz$output) {
        CompoundTag pCompound = new CompoundTag();
        try {
        super.addAdditionalSaveData(dmz$output);
        pCompound.putInt("Variant", getVariant());

        } finally {
            com.dragonminez.platform.nbt.NbtValues.write(dmz$output, pCompound);
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput dmz$input) {
        CompoundTag pCompound = com.dragonminez.platform.nbt.NbtValues.read(dmz$input);
        super.readAdditionalSaveData(dmz$input);
        setVariant(pCompound.getIntOr("Variant", 0));
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("base_controller", 5, this::walkPredicate));
    }

    private <T extends GeoAnimatable> PlayState walkPredicate(AnimationTest<T> event) {
        NamekFrogEntity entity = (NamekFrogEntity) event.animatable();

        if (event.isMoving()) {
            event.controller().setAnimation(RawAnimation.begin().thenLoop("walk"));
            return PlayState.CONTINUE;
        }

        event.controller().setAnimation(RawAnimation.begin().thenLoop("idle"));
        return PlayState.CONTINUE;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, EntitySpawnReason pReason, @org.jetbrains.annotations.Nullable SpawnGroupData pSpawnData) {
        this.setVariant(this.random.nextInt(VARIANT_COUNT));
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
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
