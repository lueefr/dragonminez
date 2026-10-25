package com.dragonminez.client.render.effects;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AuraTypeBlendTest {
    @Test void everyTypePairCrossfadesInBothDirectionsWithoutAnOpacitySpike() {
        var types = List.of("smooth", "sparking", "god", "kakarot", "default", "custom:texture");
        for (String from : types) for (String to : types) {
            if (from.equals(to)) continue;
            var blend = new AuraTypeBlend<String>();
            assertEquals(1, blend.update(List.of(from), 0).get(from));
            var start = blend.update(List.of(to), 0);
            assertEquals(1, start.get(from));
            assertEquals(0, start.get(to));
            double previous = 0;
            for (int frame = 0; frame < 24; frame++) {
                var weights = blend.update(List.of(to), 1d / 60);
                double next = weights.get(to);
                assertTrue(next >= previous);
                assertEquals(1, weights.values().stream().mapToDouble(Double::doubleValue).sum(), 1e-9);
                previous = next;
            }
            assertEquals(1, blend.update(List.of(to), .01).get(to));
            assertEquals(1, blend.update(List.of(to), .1).size());
        }
    }

    @Test void rapidReversalAndThirdTypeStartFromTheVisibleMixture() {
        var blend = new AuraTypeBlend<String>();
        blend.update(List.of("a"), 0);
        blend.update(List.of("b"), 0);
        var half = blend.update(List.of("b"), .2);
        assertEquals(.5, half.get("a"), 1e-9);
        assertEquals(half, blend.update(List.of("a"), 0));
        var reversed = blend.update(List.of("a"), .1);
        assertTrue(reversed.get("a") > .5);
        var third = blend.update(List.of("c"), 0);
        assertEquals(reversed.get("a"), third.get("a"));
        assertEquals(reversed.get("b"), third.get("b"));
        assertEquals(0, third.get("c"));
        assertEquals(java.util.Map.of("c", 1d), blend.update(List.of("c"), 1));
    }

    @Test void layersFadeIndependentlyAndPauseDoesNotAdvanceTime() {
        var blend = new AuraTypeBlend<String>();
        blend.update(List.of("base", "extra"), 0);
        blend.update(List.of("base", "stack"), 0);
        var half = blend.update(List.of("base", "stack"), .2);
        assertEquals(1, half.get("base"));
        assertEquals(.5, half.get("extra"), 1e-9);
        assertEquals(.5, half.get("stack"), 1e-9);
        assertEquals(half, blend.update(List.of("base", "stack"), 0));
        blend.clear();
        assertEquals(java.util.Map.of("replacement", 1d), blend.update(List.of("replacement"), 0));
    }
}
