package com.dragonminez.api.content;

public final class ContentProgressTest {
    public static void main(String[] args) {
        ContentProgress progress = new ContentProgress();
        if (progress.hasCompleted("custom:event")) throw new AssertionError();
        progress.complete("custom:event");
        progress.complete("custom:event");
        ContentProgress restored = new ContentProgress();
        restored.load(progress.save());
        if (restored.count("custom:event")!=2) throw new AssertionError("NBT round trip lost completions");
        ContentProgress cloned = new ContentProgress();
        cloned.copyFrom(restored);
        if (!cloned.hasCompleted("custom:event")) throw new AssertionError("Respawn copy lost flags");
        try { cloned.hasCompleted("invalid"); throw new AssertionError("Invalid id accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Content progress: lifetime flags, completion counts, NBT round trip, player copy and identifier validation passed.");
    }
}
