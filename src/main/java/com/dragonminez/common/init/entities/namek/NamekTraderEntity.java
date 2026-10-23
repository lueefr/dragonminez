package com.dragonminez.common.init.entities.namek;

import com.dragonminez.Reference;
import com.dragonminez.common.init.CapsuleCorpMapTrade;
import com.dragonminez.common.init.entities.goals.VillageAlertSystem;
import com.dragonminez.server.world.structure.helper.DMZStructures;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

public class NamekTraderEntity extends NamekVillagerEntity{

    private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(NamekTraderEntity.class, EntityDataSerializers.INT);

    public static final int VARIANT_COUNT = 4;

    private static final int GETE_STAR_MAP_PRICE = 16;
    private static final int GETE_STAR_MAP_USES = 4;
    private static final int GETE_STAR_MAP_XP = 10;

    public static final Identifier[] TEXTURES = new Identifier[VARIANT_COUNT];

    static {
        for (int i = 0; i < VARIANT_COUNT; i++) {
            TEXTURES[i] = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/enemies/namek_trader_" + i + ".png");
        }
    }
    public NamekTraderEntity(EntityType<? extends Villager> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable("entity." + Reference.MOD_ID + ".namek_trader");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.20D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public void checkDespawn() {
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
        pCompound.putInt("Variant", getVariantNamek());

        } finally {
            com.dragonminez.platform.nbt.NbtValues.write(dmz$output, pCompound);
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput dmz$input) {
        CompoundTag pCompound = com.dragonminez.platform.nbt.NbtValues.read(dmz$input);
        super.readAdditionalSaveData(dmz$input);
        setVariantNamek(pCompound.getIntOr("Variant", 0));
    }

    public int getVariantNamek() {
        return this.entityData.get(VARIANT);
    }
    public void setVariantNamek(int variant) {
        this.entityData.set(VARIANT, variant);
    }

    public Identifier getCurrentTexture() {
        int variant = getVariantNamek();
        if (variant < 0 || variant >= TEXTURES.length) {
            variant = 0;
        }
        return TEXTURES[variant];
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, EntitySpawnReason pReason, @Nullable SpawnGroupData pSpawnData) {
        this.setVariantNamek(this.random.nextInt(VARIANT_COUNT));
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
    }

    @Override
    public MerchantOffers getOffers() {
        MerchantOffers offers = super.getOffers();
        if (this.level() instanceof ServerLevel && !(this instanceof CCNamekianEntity) && !hasGeteStarMap(offers)) {
            MerchantOffer map = new CapsuleCorpMapTrade(new ItemStack(Items.EMERALD, GETE_STAR_MAP_PRICE), new ItemStack(Items.MAP),
                    DMZStructures.GETE_STAR, Reference.MOD_ID + ".gete_star", net.minecraft.world.level.saveddata.maps.MapDecorationTypes.RED_X, GETE_STAR_MAP_USES, GETE_STAR_MAP_XP)
                    .getOffer(this, this.random);
            if (map != null) offers.add(map);
        }
        return offers;
    }

    private static boolean hasGeteStarMap(MerchantOffers offers) {
        String id = DMZStructures.GETE_STAR.identifier().toString();
        for (MerchantOffer offer : offers) {
            CompoundTag tag = offer.getResult().getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            if (tag != null && id.equals(tag.getStringOr(CapsuleCorpMapTrade.STRUCTURE_TAG, ""))) return true;
        }
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("base_controller", 5, this::walkPredicate));
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel dmz$level, DamageSource source, float amount) {
        boolean isHurt = super.hurtServer(dmz$level, source, amount);

        if (isHurt && source.getEntity() instanceof Player) {
            Player player = (Player) source.getEntity();
            VillageAlertSystem.alertAll(player);
        }

        return isHurt;
    }

    private <T extends GeoAnimatable> PlayState walkPredicate(com.geckolib.animation.AnimationTest<T> event) {
        NamekTraderEntity entity = (NamekTraderEntity) event.animatable();

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

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
