package com.dragonminez.mixin.common;

import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Noise sources apply before native wiring so generation and biome sampling use the same channels. */
@Mixin(RandomState.class)
public abstract class WorldgenStateMixin {
    @ModifyVariable(method="<init>",at=@At("HEAD"),argsOnly=true)
    private static NoiseGeneratorSettings source(NoiseGeneratorSettings settings){return com.dragonminez.server.world.gen.WorldgenBridge.adapt(settings);}
}
