package com.dragonminez.common.spacepod;

public final class SpacePodArrival {
    private static final int MIN_Y = 201;
    private static final int MAX_Y = 299;
    private static final int TARGET_Y = 250;

    private SpacePodArrival() {
    }

    public static int[] heights(int minBuild, int maxBuild, double height) {
        int min = Math.max(MIN_Y, minBuild + 1);
        int max = Math.min(MAX_Y, maxBuild - (int) Math.ceil(height) - 1);
        if (min > max) return new int[0];

        int target = Math.max(min, Math.min(TARGET_Y, max));
        int[] heights = new int[max - min + 1];
        int index = 0;
        for (int offset = 0; index < heights.length; offset++) {
            if (target + offset <= max) heights[index++] = target + offset;
            if (offset > 0 && target - offset >= min) heights[index++] = target - offset;
        }
        return heights;
    }
}
