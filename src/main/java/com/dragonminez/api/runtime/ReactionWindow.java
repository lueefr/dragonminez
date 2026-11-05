package com.dragonminez.api.runtime;

import java.util.*;

/** Bounded per-defender projectile attempts; this does not grant damage immunity. */
public final class ReactionWindow {
    private record Key(UUID projectile, String rule) {}
    private record Attempt(long tick, int interval) {}
    private final LinkedHashMap<Key, Attempt> attempts = new LinkedHashMap<>();
    public void clear() { attempts.clear(); }
    public boolean attempt(UUID projectile, String rule, long tick, int interval) {
        if (interval < 0 || interval > 72000) throw new IllegalArgumentException("Invalid reaction interval");
        if (interval == 0) return true;
        var key = new Key(Objects.requireNonNull(projectile), Objects.requireNonNull(rule));
        var previous = attempts.get(key);
        if (previous != null && tick >= previous.tick() && tick - previous.tick() < previous.interval()) return false;
        if (attempts.size() >= 256 && !attempts.containsKey(key)) attempts.pollFirstEntry();
        attempts.put(key, new Attempt(tick, interval)); return true;
    }
}
