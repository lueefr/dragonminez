package com.dragonminez.server.world.feature;

import com.dragonminez.Reference;
import com.dragonminez.server.world.structure.BossStructures.TreeOfMightCanopyFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import com.dragonminez.platform.registry.DeferredRegister;
import com.dragonminez.platform.registry.DeferredHolder;

public class OverworldFeatures {
	public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Reference.MOD_ID);

	public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> STONE_SPIKE = FEATURES.register("stone_spike",
			() -> new StoneSpikeFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> ROCKY_PEAK = FEATURES.register("rocky_peak",
            () -> new RockyPeakFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> KARST_PILLAR = FEATURES.register("karst_pillar",
            () -> new KarstPillarFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> ROCKY_CLIFF = FEATURES.register("rocky_cliff",
            () -> new RockyCliffFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> TREE_OF_MIGHT_CANOPY = FEATURES.register("tree_of_might_canopy",
            () -> new TreeOfMightCanopyFeature(NoneFeatureConfiguration.CODEC));

    public static void register() {
		FEATURES.register();
	}
}
