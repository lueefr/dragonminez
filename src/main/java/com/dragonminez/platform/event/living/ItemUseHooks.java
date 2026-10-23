package com.dragonminez.platform.event.living;
import com.dragonminez.platform.event.GameEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
/** Event entrypoints for containers that consume a stored item without starting the held-item animation. */
public final class ItemUseHooks {
    private ItemUseHooks() {}
    public static int start(LivingEntity entity, ItemStack stack, int duration) {
        var event = GameEvents.BUS.post(new LivingEntityUseItemEvent.Start(entity, stack, duration));
        return event.isCanceled() ? -1 : event.getDuration();
    }
    public static ItemStack finish(LivingEntity entity, ItemStack stack, int duration, ItemStack result) {
        return GameEvents.BUS.post(new LivingEntityUseItemEvent.Finish(entity, stack, duration, result)).getResultStack();
    }
}
