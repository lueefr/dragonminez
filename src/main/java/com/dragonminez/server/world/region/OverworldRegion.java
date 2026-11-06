package com.dragonminez.server.world.region;

import com.dragonminez.Reference;
import com.dragonminez.server.world.biome.OverworldBiomes;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;
import java.util.List;
import java.util.function.Consumer;

/**
 * TerraBlender region that maps native arid terrain slots to rocky inside the DMZ region.
 *
 * Reusing native parameter points avoids equal-distance overlaps with the complete vanilla
 * layout and follows the active climate sampler. Other regions keep their own biome choices.
 */
public class OverworldRegion extends Region {
    private final List<ResourceKey<Biome>> anchors;
    private static final java.util.Set<ResourceKey<Biome>> missing=java.util.concurrent.ConcurrentHashMap.newKeySet();
    public static List<ResourceKey<Biome>> defaults(){return List.of(Biomes.BADLANDS,Biomes.ERODED_BADLANDS,Biomes.WOODED_BADLANDS);}
    public OverworldRegion(int weight){this(weight,defaults());}
    public OverworldRegion(int weight,List<ResourceKey<Biome>> anchors){
        super(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,"overworld_region"),RegionType.OVERWORLD,weight);
        this.anchors=List.copyOf(anchors);
    }
    @Override public void addBiomes(Registry<Biome> registry,Consumer<Pair<Climate.ParameterPoint,ResourceKey<Biome>>> mapper){
        var selected=anchors;
        for(var anchor:anchors)if(!registry.containsKey(anchor.location())){
            if(missing.add(anchor))com.dragonminez.LogUtil.error(com.dragonminez.Env.COMMON,"Unknown rocky anchor biome {}. Native defaults retained; configuration left untouched.",anchor.location());
            selected=defaults();break;
        }
        var resolved=selected;
        addModifiedVanillaOverworldBiomes(mapper,builder->resolved.forEach(anchor->builder.replaceBiome(anchor,OverworldBiomes.ROCKY)));
    }
}
