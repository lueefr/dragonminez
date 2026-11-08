package com.dragonminez.client.animation;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class TransformationTest {
	@Test void loopingClips() throws Exception {
		// Source contract avoids loading GeckoLib outside the Minecraft runtime.
		Path file = Path.of("src/main/java/com/dragonminez/client/animation/BaseAnimations.java");
		String source = Files.readString(file);
		int start = source.indexOf("public static RawAnimation transformationFor(");
		int end = source.indexOf("public static RawAnimation kiChargeFor(", start);
		assertTrue(start >= 0 && end > start);
		String method = source.substring(start, end);
		assertTrue(method.contains("return AnimationCache.getLoop(name);"));
		assertFalse(method.contains("getPlayAndHold"));
	}
}
