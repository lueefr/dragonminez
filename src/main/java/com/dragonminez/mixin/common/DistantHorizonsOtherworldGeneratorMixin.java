package com.dragonminez.mixin.common;

import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Otherworld requires full columns: rough surface generation fills its vertical air gaps. */
@Pseudo
@Mixin(targets = "com.seibel.distanthorizons.core.generation.DhWorldGenerator", remap = false)
public abstract class DistantHorizonsOtherworldGeneratorMixin {
    @Inject(method = "getLargestDataDetailLevel", at = @At("HEAD"), cancellable = true)
    private void dragonminez$useFullColumns(CallbackInfoReturnable<Byte> cir) {
        // Keep DH optional. Its wrapper interface is not a Minecraft or mod runtime dependency.
        try {
            Object wrapper = getClass().getField("serverLevelWrapper").get(this);
            Object level = wrapper.getClass().getMethod("getWrappedMcObject").invoke(wrapper);
            if (level instanceof ServerLevel serverLevel
                    && serverLevel.dimension().location().toString().equals("dragonminez:otherworld")) {
                cir.setReturnValue((byte) 0);
            }
        } catch (ReflectiveOperationException ignored) {
            // A different DH wrapper layout retains that version's normal generator behavior.
        }
    }
}
