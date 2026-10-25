package com.dragonminez.common.init.entities;

import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;

public class PunchMachineEntity extends Mob implements GeoEntity {

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
	private long lastHitTime = 0;
	private double accumulatedDamage = 0;
	private long combatStartTime = 0;

    public PunchMachineEntity(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoAi(true);
        this.setPersistenceRequired();
        if (!pLevel.isClientSide) maintainProtection();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.MOVEMENT_SPEED, 2.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) return false;

		if (source.getEntity() instanceof Player player) {
			if (player.isCrouching()) {
				if (!this.isRemoved()) {
					this.spawnAtLocation(MainItems.PUNCH_MACHINE_ITEM.get());
					this.discard();
				}
				return false;
			}
		}

		if (MainDamageTypes.isStrikeAttackDamage(source) || MainDamageTypes.isKiblastDamage(source)) {
			if (source.getEntity() instanceof Player player) processHit(amount, player);
			return false;
		}

		return super.hurt(source, amount);
    }

    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        // Keep LivingIncomingDamageEvent (damage readings and training rewards),
        // but never commit damage, including sources that bypass resistance.
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide) maintainProtection();
        super.tick();
    }

    private void maintainProtection() {
        ensureProtection(MobEffects.DAMAGE_RESISTANCE);
        ensureProtection(MobEffects.REGENERATION);
        if (this.isAlive() && this.getHealth() < this.getMaxHealth()) this.setHealth(this.getMaxHealth());
    }

    private void ensureProtection(Holder<MobEffect> effect) {
        MobEffectInstance current = this.getEffect(effect);
        if (current == null || current.getAmplifier() != 254 || !current.isInfiniteDuration()
                || current.isVisible() || current.showIcon()) {
            this.removeEffect(effect);
            this.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, 254, false, false, false));
        }
    }

	public void processHit(float damage, Player attacker) {
		long currentTime = System.currentTimeMillis();

		if (currentTime - lastHitTime > 5000) {
			accumulatedDamage = 0;
			combatStartTime = currentTime;
		}

		if (accumulatedDamage == 0) {
			combatStartTime = currentTime;
		}

		lastHitTime = currentTime;
		accumulatedDamage += damage;

		double seconds = (currentTime - combatStartTime) / 1000.0;
		if (seconds < 1.0) seconds = 1.0;

		double dps = accumulatedDamage / seconds;

		spawnDamageIndicator(damage);

		attacker.displayClientMessage(Component.translatable("message.dragonminez.training.damage",
                String.format(java.util.Locale.ROOT,"%.1f",damage),String.format(java.util.Locale.ROOT,"%.1f",dps))
                .withStyle(ChatFormatting.GOLD,ChatFormatting.BOLD),true);
	}

	private void spawnDamageIndicator(float damage) {
		AreaEffectCloud indicator = new AreaEffectCloud(this.level(), this.getX(), this.getY() + 0.8, this.getZ());
		indicator.setRadius(0.0F);
		indicator.setDuration(20);
		indicator.setParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AIR.defaultBlockState()));
		indicator.setWaitTime(0);

		String dmgText = String.format("%.0f", damage);
		indicator.setCustomName(com.dragonminez.api.lang.ConfigTranslations.text(dmgText).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
		indicator.setCustomNameVisible(true);

		this.level().addFreshEntity(indicator);
	}

    @Override
    public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
        tAnimationState.getController().setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity pEntity) {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public void push(Entity pEntity) {}

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity p_20971_) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
