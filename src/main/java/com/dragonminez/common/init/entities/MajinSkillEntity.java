package com.dragonminez.common.init.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

public class MajinSkillEntity  extends PathfinderMob implements GeoEntity {

    private static final EntityDataAccessor<Integer> BODY_COLOR = SynchedEntityData.defineId(MajinSkillEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);


    public MajinSkillEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("base_controller", 5, this::walkPredicate));
    }

    private <T extends GeoAnimatable> PlayState walkPredicate(AnimationTest<T> event) {
        event.controller().setAnimation(RawAnimation.begin().thenLoop("idle"));
        return PlayState.CONTINUE;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BODY_COLOR, 0xFFFFFF);
    }

    public int getBodyColor() {
        return this.entityData.get(BODY_COLOR);
    }

    public void setBodyColor(int color) {
        this.entityData.set(BODY_COLOR, color);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput dmz$output) {
        CompoundTag pCompound = new CompoundTag();
        try {
        super.addAdditionalSaveData(dmz$output);
        pCompound.putInt("BodyColor", this.getBodyColor());

        } finally {
            com.dragonminez.platform.nbt.NbtValues.write(dmz$output, pCompound);
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput dmz$input) {
        CompoundTag pCompound = com.dragonminez.platform.nbt.NbtValues.read(dmz$input);
        super.readAdditionalSaveData(dmz$input);
        if (pCompound.contains("BodyColor")) {
            this.setBodyColor(pCompound.getIntOr("BodyColor", 0));
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
