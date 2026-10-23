package com.dragonminez.common.init.entities.animal;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.ITextureVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
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

public class GiantTurtleEntity extends Animal implements GeoEntity, ITextureVariant {
    @Override public boolean isFood(net.minecraft.world.item.ItemStack stack) { return stack.is(net.minecraft.world.item.Items.SEAGRASS); }

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(GiantTurtleEntity.class, EntityDataSerializers.INT);

    public static final int VARIANT_COUNT = 3;

    public static final Identifier[] TEXTURES = new Identifier[VARIANT_COUNT];

    static {
        for (int i = 0; i < VARIANT_COUNT; i++) {
            TEXTURES[i] = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/animal/giantturtle_" + i + ".png");
        }
    }

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");

    private static final int MAX_SPAWN_HEIGHT_ABOVE_SEA = 4;

    private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);

    public GiantTurtleEntity(EntityType<? extends Animal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    public static AttributeSupplier createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
                .build();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.2D));
        this.goalSelector.addGoal(2, new RandomSwimmingGoal(this, 1.0D, 60));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.8D, 100));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level pLevel) {
        return new AmphibiousPathNavigation(this, pLevel);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void travel(Vec3 pTravelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(0.1F, pTravelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.005D, 0.0D));
        } else {
            super.travel(pTravelVector);
        }
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
        return null;
    }

    public int getVariant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.entityData.set(VARIANT, variant);
    }

    @Override
    public int getTextureVariant() {
        return getVariant();
    }

    @Override
    public void setTextureVariant(int variant) {
        setVariant(variant);
    }

    public Identifier getCurrentTexture() {
        int variant = getVariant();
        if (variant < 0 || variant >= TEXTURES.length) {
            variant = 0;
        }
        return TEXTURES[variant];
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
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, EntitySpawnReason pReason,
                                        @Nullable SpawnGroupData pSpawnData) {
        this.setVariant(this.random.nextInt(VARIANT_COUNT));
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
    }

    public static boolean canSpawnHere(EntityType<GiantTurtleEntity> pType, LevelAccessor pLevel, EntitySpawnReason pReason,
                                       BlockPos pPos, RandomSource pRandom) {
        if (pReason == EntitySpawnReason.SPAWNER) return true;

        return pPos.getY() < pLevel.getSeaLevel() + MAX_SPAWN_HEIGHT_ABOVE_SEA
                && pLevel.getBlockState(pPos.below()).is(BlockTags.SAND)
                && isBrightEnoughToSpawn(pLevel, pPos);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("base_controller", 5, this::movePredicate));
    }

    private <T extends GeoAnimatable> PlayState movePredicate(AnimationTest<T> event) {
        event.controller().setAnimation(event.isMoving() ? WALK : IDLE);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
