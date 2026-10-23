package com.dragonminez.mixin.common;

import com.dragonminez.platform.event.GameEvents;
import com.dragonminez.platform.event.living.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class LivingItemUseBridgeMixin {
    @Shadow @Final private Map<EquipmentSlot, ItemStack> lastEquipmentItems;
    @Unique private Integer dmz$startingUseDuration;
    @WrapMethod(method = "startUsingItem")
    private void dmz$start(InteractionHand hand, Operation<Void> original) {
        var entity = (LivingEntity) (Object) this;
        var stack = entity.getItemInHand(hand);
        if (stack.isEmpty() || entity.isUsingItem()) { original.call(hand); return; }
        var event = GameEvents.BUS.post(new LivingEntityUseItemEvent.Start(entity, stack, stack.getUseDuration(entity)));
        if (event.isCanceled()) return;
        Integer previous = dmz$startingUseDuration;
        dmz$startingUseDuration = event.getDuration();
        try { original.call(hand); } finally { dmz$startingUseDuration = previous; }
    }
    @WrapOperation(method = "startUsingItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getUseDuration(Lnet/minecraft/world/entity/LivingEntity;)I"))
    private int dmz$duration(ItemStack stack, LivingEntity entity, Operation<Integer> original) {
        return dmz$startingUseDuration != null ? dmz$startingUseDuration : original.call(stack, entity);
    }
    @WrapOperation(method = "completeUsingItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack dmz$finish(ItemStack stack, Level level, LivingEntity entity, Operation<ItemStack> original) {
        ItemStack before = stack.copy();
        int duration = entity.getUseItemRemainingTicks();
        ItemStack result = original.call(stack, level, entity);
        return GameEvents.BUS.post(new LivingEntityUseItemEvent.Finish(entity, before, duration, result)).getResultStack();
    }
    @Inject(method = "collectEquipmentChanges", at = @At("RETURN"))
    private void dmz$equipment(CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> callback) {
        var entity = (LivingEntity) (Object) this;
        Map<EquipmentSlot, ItemStack> changed = callback.getReturnValue();
        if (changed != null && !entity.level().isClientSide()) changed.forEach((slot, current) ->
            GameEvents.BUS.post(new LivingEquipmentChangeEvent(entity, slot, lastEquipmentItems.get(slot).copy(), current.copy())));
    }
}
