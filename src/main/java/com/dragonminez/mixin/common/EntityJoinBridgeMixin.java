package com.dragonminez.mixin.common;
import com.dragonminez.platform.event.GameEvents;
import com.dragonminez.platform.event.entity.EntityJoinLevelEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(PersistentEntitySectionManager.class)
public abstract class EntityJoinBridgeMixin {
    @Inject(method = "addEntity(Lnet/minecraft/world/level/entity/EntityAccess;Z)Z", at = @At("HEAD"), cancellable = true)
    private void dmz$join(EntityAccess access, boolean loaded, CallbackInfoReturnable<Boolean> callback) {
        if (access instanceof Entity entity && GameEvents.BUS.post(new EntityJoinLevelEvent(entity, loaded)).isCanceled())
            callback.setReturnValue(false);
    }
}
