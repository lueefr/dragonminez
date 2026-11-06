package com.dragonminez.server.world.gen;

import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.server.world.biome.OverworldBiomes;
import net.minecraft.world.level.levelgen.SurfaceRules;

public class OverworldSurfaceRules {
	private static final SurfaceRules.RuleSource ROCKY_DIRT = SurfaceRules.state(MainBlocks.ROCKY_DIRT.get().defaultBlockState());
	private static final SurfaceRules.RuleSource ROCKY_STONE = SurfaceRules.state(MainBlocks.ROCKY_STONE.get().defaultBlockState());

	public static SurfaceRules.RuleSource makeRules() {
		SurfaceRules.ConditionSource isRockyWasteland = SurfaceRules.isBiome(OverworldBiomes.ROCKY);

		return SurfaceRules.sequence(
				SurfaceRules.ifTrue(isRockyWasteland, SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(),
						SurfaceRules.sequence(
								SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, ROCKY_DIRT),
								SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, ROCKY_STONE),
								SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR, ROCKY_STONE)
						))
				)
		);
	}
}
