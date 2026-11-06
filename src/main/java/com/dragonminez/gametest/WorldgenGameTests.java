package com.dragonminez.gametest;

import com.dragonminez.common.config.*;
import com.dragonminez.server.world.gen.*;
import com.dragonminez.server.world.biome.OverworldBiomes;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.world.level.levelgen.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class WorldgenGameTests {
    @GameTest(template="port_empty")
    public static void nativeNoiseSources(GameTestHelper helper) {
        var registries=helper.getLevel().registryAccess();var settings=registries.registryOrThrow(Registries.NOISE_SETTINGS);var noises=registries.lookupOrThrow(Registries.NOISE);
        var target=settings.getOrThrow(NoiseGeneratorSettings.OVERWORLD);var large=settings.getOrThrow(NoiseGeneratorSettings.LARGE_BIOMES);
        var original=target.noiseRouter();long seed=54321;
        try {
            WorldgenBridge.clear();var baseline=RandomState.create(target,noises,seed);var expected=RandomState.create(large,noises,seed);
            WorldgenBridge.install(settings,Map.of("minecraft:overworld",new NoiseBinding("minecraft:large_biomes",false,true)));
            var mixed=WorldgenBridge.adapt(target);
            helper.assertTrue(mixed.noiseRouter().finalDensity()==original.finalDensity()&&mixed.defaultBlock()==target.defaultBlock()&&mixed.surfaceRule()==target.surfaceRule()&&mixed.noiseSettings()==target.noiseSettings(),"Climate binding changed terrain/material/height ownership");
            var adapted=RandomState.create(target,noises,seed);boolean changed=false;
            for(int x=-128;x<=128;x+=32){
                var old=baseline.sampler().sample(x,16,x/2);var actual=adapted.sampler().sample(x,16,x/2);var wanted=expected.sampler().sample(x,16,x/2);
                helper.assertTrue(actual.equals(wanted),"Native sampler did not receive the borrowed climate channels");changed|=!actual.equals(old);
            }
            helper.assertTrue(changed,"Biome-size source did not change climate sampling");
            helper.assertTrue(settings.getOrThrow(NoiseGeneratorSettings.OVERWORLD)==target&&target.noiseRouter()==original,"Binding rewrote registered/saved generator definitions");
            WorldgenBridge.install(settings,Map.of("minecraft:overworld",new NoiseBinding("minecraft:large_biomes",true,false)));
            var terrain=WorldgenBridge.adapt(target).noiseRouter();helper.assertTrue(terrain.finalDensity()==large.noiseRouter().finalDensity()&&terrain.temperature()==original.temperature(),"Terrain-only binding changed climate");
            var before=WorldgenBridge.adapt(target);boolean rejected=false;
            try{WorldgenBridge.install(settings,Map.of("minecraft:overworld",new NoiseBinding("missing:settings",true,true)));}catch(RuntimeException ex){rejected=true;}
            helper.assertTrue(rejected&&WorldgenBridge.adapt(target)==before,"Invalid bindings partially replaced the working snapshot");
            var points=new ArrayList<com.mojang.datafixers.util.Pair<net.minecraft.world.level.biome.Climate.ParameterPoint,ResourceKey<net.minecraft.world.level.biome.Biome>>>();
            new com.dragonminez.server.world.region.OverworldRegion(40).addBiomes(registries.registryOrThrow(Registries.BIOME),points::add);
            helper.assertTrue(points.stream().anyMatch(point->point.getSecond().equals(OverworldBiomes.ROCKY))&&points.stream().noneMatch(point->point.getSecond().equals(net.minecraft.world.level.biome.Biomes.BADLANDS)),"Rocky did not replace native slots inside its own region");
            var fallback=new ArrayList<com.mojang.datafixers.util.Pair<net.minecraft.world.level.biome.Climate.ParameterPoint,ResourceKey<net.minecraft.world.level.biome.Biome>>>();
            new com.dragonminez.server.world.region.OverworldRegion(40,List.of(ResourceKey.create(Registries.BIOME,ResourceLocation.parse("missing:biome")))).addBiomes(registries.registryOrThrow(Registries.BIOME),fallback::add);
            helper.assertTrue(fallback.equals(points),"Unknown anchor biome did not retain the native slot defaults");
            var rocky=registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(OverworldBiomes.ROCKY);
            helper.assertTrue(rocky.is(net.neoforged.neoforge.common.Tags.Biomes.IS_DRY_OVERWORLD)&&rocky.is(net.neoforged.neoforge.common.Tags.Biomes.IS_HOT_OVERWORLD)&&rocky.is(net.neoforged.neoforge.common.Tags.Biomes.IS_WASTELAND),"Rocky lacks common worldgen classification tags");
            var surface=SurfaceRules.RuleSource.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,OverworldSurfaceRules.makeRules()).getOrThrow().toString();
            helper.assertTrue(surface.contains("above_preliminary_surface")&&!surface.contains("bedrock")&&!surface.contains("y_above"),"Rocky surface retained a fixed height/global bedrock override");
            var defaults=new GeneralServerConfig().getWorldGen().sources();
            helper.assertTrue(defaults.get("dragonminez:namek").terrain()&&defaults.get("dragonminez:sacredkaiplanet").climate()&&!defaults.get("dragonminez:sacredkaiplanet").terrain(),"Default inheritance erased special dimension terrain policy");
            helper.succeed();
        } finally {WorldgenBridge.install(settings,ConfigManager.getServerConfig().getWorldGen().sources());}
    }
}
