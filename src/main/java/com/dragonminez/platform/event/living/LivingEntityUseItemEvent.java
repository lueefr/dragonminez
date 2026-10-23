package com.dragonminez.platform.event.living;
import com.dragonminez.platform.event.ICancellableEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
public abstract class LivingEntityUseItemEvent extends LivingEvent {
    private final ItemStack item;
    private int duration;
    private LivingEntityUseItemEvent(LivingEntity entity, ItemStack item, int duration) {
        super(entity); this.item = item; this.duration = duration;
    }
    public ItemStack getItem() { return item; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = Math.max(0, duration); }
    public static final class Start extends LivingEntityUseItemEvent implements ICancellableEvent {
        public Start(LivingEntity entity, ItemStack item, int duration) { super(entity, item, duration); }
    }
    public static final class Finish extends LivingEntityUseItemEvent {
        private ItemStack result;
        public Finish(LivingEntity entity, ItemStack item, int duration, ItemStack result) {
            super(entity, item, duration); this.result = result;
        }
        public ItemStack getResultStack() { return result; }
        public void setResultStack(ItemStack result) { this.result = java.util.Objects.requireNonNull(result); }
    }
}
