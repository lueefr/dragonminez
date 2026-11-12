package com.dragonminez.mixin.common;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only seed approximations are blocked: Distant Horizons keeps its native chunk generator. */
@Pseudo
@Mixin(targets = "mobius.seedrenderer.dh.SeedWorldGenerator", remap = false)
public abstract class SeedTerrainMixin {
    @Inject(method = "createOrNull(Lcom/seibel/distanthorizons/api/interfaces/world/IDhApiLevelWrapper;Lmobius/seedrenderer/dh/WorldBasis;)Lmobius/seedrenderer/dh/SeedWorldGenerator;",
            at = @At("HEAD"), cancellable = true, require = 0)
    private static void guard(@Coerce Object wrapper, @Coerce Object basis, CallbackInfoReturnable<Object> result) {
        try {
            var method = basis.getClass().getMethod("dimension");
            if (!method.canAccess(basis)) method.trySetAccessible();
            Object dimension = method.invoke(basis);
            if (dimension instanceof ResourceKey<?> key && key.location().toString().equals("dragonminez:otherworld")) {
                result.setReturnValue(null);
            }
        } catch (ReflectiveOperationException ignored) {
            // Unknown SeedGen versions retain their own behavior rather than disabling native terrain.
        }
    }
}
