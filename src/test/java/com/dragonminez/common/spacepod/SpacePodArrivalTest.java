package com.dragonminez.common.spacepod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpacePodArrivalTest {
    @Test
    void prioritizesY250() {
        int[] heights = SpacePodArrival.heights(-64, 320, 2.0D);
        assertEquals(250, heights[0]);
        assertEquals(251, heights[1]);
        assertEquals(249, heights[2]);
        assertEquals(99, heights.length);
        for (int y : heights) assertTrue(y > 200 && y < 300);
    }

    @Test
    void respectsDimensionHeight() {
        int[] heights = SpacePodArrival.heights(0, 256, 2.0D);
        assertEquals(250, heights[0]);
        for (int y : heights) assertTrue(y >= 201 && y <= 253);
    }

    @Test
    void rejectsTooShortDimension() {
        assertEquals(0, SpacePodArrival.heights(0, 128, 2.0D).length);
    }
}
