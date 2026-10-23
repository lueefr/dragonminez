package com.dragonminez.platform.event.living;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
public abstract class LivingDamageEvent extends LivingEvent {
    private final DamageSource source;
    protected LivingDamageEvent(LivingEntity entity, DamageSource source) { super(entity); this.source = source; }
    public DamageSource getSource() { return source; }
    public static final class Pre extends LivingDamageEvent {
        private final float originalDamage;
        private float damage;
        public Pre(LivingEntity entity, DamageSource source, float damage) { super(entity, source); originalDamage = this.damage = damage; }
        public float getOriginalDamage() { return originalDamage; }
        public float getNewDamage() { return damage; }
        public void setNewDamage(float damage) { this.damage = damage; }
    }
    public static final class Post extends LivingDamageEvent {
        private final float damage;
        public Post(LivingEntity entity, DamageSource source, float damage) { super(entity, source); this.damage = damage; }
        public float getNewDamage() { return damage; }
    }
}
