package com.dragonminez.mixin.common;
import com.dragonminez.platform.event.GameEvents;
import com.dragonminez.platform.event.living.LivingChangeTargetEvent;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(Mob.class)
public abstract class MobTargetBridgeMixin {
    @WrapMethod(method = "setTarget")
    private void dmz$target(LivingEntity target, Operation<Void> original) {
        var event = GameEvents.BUS.post(new LivingChangeTargetEvent((Mob) (Object) this, target));
        if (!event.isCanceled()) original.call(event.getNewAboutToBeSetTarget());
    }
}
