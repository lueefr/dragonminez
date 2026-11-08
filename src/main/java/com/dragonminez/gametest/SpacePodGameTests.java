package com.dragonminez.gametest;

import com.dragonminez.common.spacepod.SpacePodDestinationRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class SpacePodGameTests {

	@GameTest(template = "port_empty")
	public static void namekLookup(GameTestHelper helper) {
		var destination = SpacePodDestinationRegistry.getServerDestination("namek");
		helper.assertTrue(destination != null && "dragonminez:namek".equals(destination.dimension()),
				"Space Pod destination lookup did not resolve Namek");
		helper.succeed();
	}
}
