package com.dragonminez.mixin.common;
import com.dragonminez.platform.event.GameEvents;
import com.dragonminez.platform.event.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class EntitySizeBridgeMixin {
    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void dmz$dimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> callback) {
        callback.setReturnValue(GameEvents.BUS.post(new EntityEvent.Size((LivingEntity) (Object) this,
                pose, callback.getReturnValue())).getNewSize());
    }
}
