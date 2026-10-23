package com.dragonminez.server.world.feature;

import com.dragonminez.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import com.dragonminez.platform.registry.DeferredRegister;
import com.dragonminez.platform.registry.DeferredHolder;

public class OtherworldFeatures {
	public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Reference.MOD_ID);

	public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> CANDY_ORB = FEATURES.register("candy_orb",
			() -> new CandyOrbFeature(NoneFeatureConfiguration.CODEC));

	public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> CRYSTAL_SPIKE = FEATURES.register("crystal_spike",
			() -> new CrystalSpikeFeature(NoneFeatureConfiguration.CODEC));

	public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> BONE_SPINE = FEATURES.register("bone_spine",
			() -> new BoneSpineFeature(NoneFeatureConfiguration.CODEC));

	public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> THORN_BALL = FEATURES.register("thorn_ball",
			() -> new ThornBallFeature(NoneFeatureConfiguration.CODEC));

	public static void register() {
		FEATURES.register();
	}
}
