package com.dragonminez.platform.event.living;
import com.dragonminez.platform.event.ICancellableEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.Holder;
public abstract class MobEffectEvent extends LivingEvent {
    private final MobEffectInstance instance;
    private final Holder<MobEffect> effect;
    private MobEffectEvent(LivingEntity entity, Holder<MobEffect> effect, MobEffectInstance instance) {
        super(entity); this.effect = effect; this.instance = instance;
    }
    public Holder<MobEffect> getEffect() { return effect; }
    public MobEffectInstance getEffectInstance() { return instance; }
    public static final class Added extends MobEffectEvent {
        public Added(LivingEntity entity, MobEffectInstance instance) { super(entity, instance.getEffect(), instance); }
    }
    public static final class Expired extends MobEffectEvent {
        public Expired(LivingEntity entity, MobEffectInstance instance) { super(entity, instance.getEffect(), instance); }
    }
    public static final class Remove extends MobEffectEvent implements ICancellableEvent {
        public Remove(LivingEntity entity, Holder<MobEffect> effect) { super(entity, effect, entity.getEffect(effect)); }
    }
}
