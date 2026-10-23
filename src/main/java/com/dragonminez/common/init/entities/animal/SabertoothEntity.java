package com.dragonminez.common.init.entities.animal;

import com.dragonminez.Reference;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

public class SabertoothEntity extends DinoGlobalEntity{

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(SabertoothEntity.class, EntityDataSerializers.INT);
    private boolean isAttacking = false;

    public static final int VARIANT_COUNT = 3;

    public static final Identifier[] TEXTURES = new Identifier[VARIANT_COUNT];

    static {
        for (int i = 0; i < VARIANT_COUNT; i++) {
            TEXTURES[i] = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/animal/sabertooth_" + i + ".png");
        }
    }

    public SabertoothEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput dmz$output) {
        CompoundTag pCompound = new CompoundTag();
        try {
        super.addAdditionalSaveData(dmz$output);
        pCompound.putInt("Variant", getVariantTiger());

        } finally {
            com.dragonminez.platform.nbt.NbtValues.write(dmz$output, pCompound);
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput dmz$input) {
        CompoundTag pCompound = com.dragonminez.platform.nbt.NbtValues.read(dmz$input);
        super.readAdditionalSaveData(dmz$input);
        setVariantTiger(pCompound.getIntOr("Variant", 0));
    }

    public int getVariantTiger() {return this.entityData.get(VARIANT);}
    public void setVariantTiger(int variant) {this.entityData.set(VARIANT, variant);}

    public Identifier getCurrentTexture() {
        int variant = getVariantTiger();
        if (variant < 0 || variant >= TEXTURES.length) {
            variant = 0;
        }
        return TEXTURES[variant];
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, EntitySpawnReason pReason, @Nullable SpawnGroupData pSpawnData) {
        this.setVariantTiger(this.random.nextInt(VARIANT_COUNT));
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

        controllers.add(new AnimationController<>("base_controller", 5, this::walkPredicate));

        controllers.add(new AnimationController<>("attack_controller", 0, this::attackPredicate));

        controllers.add(new AnimationController<>("tail_controller", 0, this::tailPredicate));
    }

    private <T extends GeoAnimatable> PlayState walkPredicate(AnimationTest<T> event) {
        SabertoothEntity entity = (SabertoothEntity) event.animatable();

        if (event.isMoving()) {
            if (entity.isAggressive() || entity.getTarget() != null) {
                event.controller().setAnimation(RawAnimation.begin().thenLoop("run"));
            } else {
                event.controller().setAnimation(RawAnimation.begin().thenLoop("walk"));
            }
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
}
