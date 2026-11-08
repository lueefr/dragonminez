package com.dragonminez.client.animation;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertSame;

class TransformationTest {
	@Test void animatedClips() {
		for (String clip : List.of("transf.generic", "transf.daima", "transf.ssj3",
				"transf.freezer", "transf.ozaru", "transf.absorb", "transf.berserker")) {
			assertSame(AnimationCache.getLoop(clip), BaseAnimations.transformationFor(clip));
		}
	}
}
