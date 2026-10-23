package com.dragonminez.server.world.structure.processor;

import com.dragonminez.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import com.dragonminez.platform.registry.DeferredRegister;
import com.dragonminez.platform.registry.DeferredHolder;

public class MainStructureProcessors {
	public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS =
			DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, Reference.MOD_ID);

	public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<FoundationProcessor>> FOUNDATION =
			PROCESSORS.register("foundation", () -> () -> FoundationProcessor.CODEC);
	public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<ExteriorAirProcessor>> EXTERIOR_AIR =
			PROCESSORS.register("exterior_air", () -> () -> ExteriorAirProcessor.CODEC);

	public static void register() {
		PROCESSORS.register();
	}
}
