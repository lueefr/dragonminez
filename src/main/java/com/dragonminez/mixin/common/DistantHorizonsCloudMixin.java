package com.dragonminez.mixin.common;

import com.dragonminez.common.init.block.custom.OtherworldCloudBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cloud terrain has a visible volume even though entities can pass through it. */
@Pseudo
@Mixin(targets = "com.seibel.distanthorizons.common.wrappers.block.BlockStateWrapper_neoforge", remap = false)
public abstract class DistantHorizonsCloudMixin {
    @Shadow @Final public BlockState blockState;

    @Inject(method = "isSolid", at = @At("HEAD"), cancellable = true)
    private void dragonminez$keepCloudTerrain(CallbackInfoReturnable<Boolean> cir) {
        if (blockState != null && blockState.getBlock() instanceof OtherworldCloudBlock) {
            cir.setReturnValue(true);
        }
    }
}
