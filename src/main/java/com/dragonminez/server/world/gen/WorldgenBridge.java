package com.dragonminez.server.world.gen;

import com.dragonminez.common.config.NoiseBinding;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.*;
import java.util.*;

/** Adapt only newly constructed samplers; saved generator definitions and loaded chunks stay intact. */
public final class WorldgenBridge {
    private static volatile Map<NoiseGeneratorSettings,NoiseGeneratorSettings> sources=Map.of();
    private WorldgenBridge(){}
    public static void clear(){sources=Map.of();}
    public static NoiseGeneratorSettings adapt(NoiseGeneratorSettings target){return sources.getOrDefault(target,target);}
    public static void install(Registry<NoiseGeneratorSettings> registry,Map<String,NoiseBinding> bindings){
        if(bindings.size()>64)throw new IllegalArgumentException("At most 64 noise source bindings");
        Map<NoiseGeneratorSettings,NoiseGeneratorSettings> prepared=new IdentityHashMap<>();
        bindings.forEach((id,binding)->{
            com.dragonminez.api.content.ContentProgress.requireId(id);binding.validate();
            var target=registry.getOptional(ResourceLocation.parse(id)).orElseThrow(()->new IllegalArgumentException("Unknown target noise settings: "+id));
            var source=registry.getOptional(ResourceLocation.parse(binding.source())).orElseThrow(()->new IllegalArgumentException("Unknown source noise settings: "+binding.source()));
            prepared.put(target,mix(target,source,binding));
        });
        sources=Collections.unmodifiableMap(prepared);
    }
    public static NoiseGeneratorSettings mix(NoiseGeneratorSettings target,NoiseGeneratorSettings source,NoiseBinding binding){
        var a=target.noiseRouter();var b=source.noiseRouter();boolean terrain=binding.terrain(),climate=binding.climate();
        var router=new NoiseRouter(
                terrain?b.barrierNoise():a.barrierNoise(),terrain?b.fluidLevelFloodednessNoise():a.fluidLevelFloodednessNoise(),
                terrain?b.fluidLevelSpreadNoise():a.fluidLevelSpreadNoise(),terrain?b.lavaNoise():a.lavaNoise(),
                climate?b.temperature():a.temperature(),climate?b.vegetation():a.vegetation(),climate?b.continents():a.continents(),
                climate?b.erosion():a.erosion(),climate?b.depth():a.depth(),climate?b.ridges():a.ridges(),
                terrain?b.initialDensityWithoutJaggedness():a.initialDensityWithoutJaggedness(),terrain?b.finalDensity():a.finalDensity(),
                terrain?b.veinToggle():a.veinToggle(),terrain?b.veinRidged():a.veinRidged(),terrain?b.veinGap():a.veinGap());
        return new NoiseGeneratorSettings(target.noiseSettings(),target.defaultBlock(),target.defaultFluid(),router,target.surfaceRule(),target.spawnTarget(),target.seaLevel(),target.disableMobGeneration(),target.aquifersEnabled(),target.oreVeinsEnabled(),target.useLegacyRandomSource());
    }
}
