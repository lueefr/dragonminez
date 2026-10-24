package com.dragonminez.mixin.common;

import com.dragonminez.server.world.gen.OtherworldGeneration;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** SeedGen's single-surface approximation cannot represent Otherworld's separated cloud decks. */
@Pseudo
@Mixin(targets = "mobius.seedrenderer.compat.WorldgenCompat", remap = false)
public abstract class DhSeedGenOtherworldMixin {
    @Inject(method = "foreignTerrain", at = @At("HEAD"), cancellable = true)
    private static void dragonminez$useCompleteGeneration(ChunkGenerator generator,
                                                        CallbackInfoReturnable<String> cir) {
        if (generator instanceof NoiseBasedChunkGenerator noise
                && noise.generatorSettings().is(OtherworldGeneration.OTHERWORLD_NOISE_SETTINGS)) {
            cir.setReturnValue("DragonMineZ Otherworld layered terrain and fixed structures");
        }
    }
}
