package com.dragonminez.client.render.effects;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Short, reversible crossfade. Keys identify geometry/texture, not changing style parameters. */
public final class AuraTypeBlend<K> {
    public static final double DURATION = 0.4;
    private final Map<K, Double> weights = new LinkedHashMap<>();
    private final Map<K, Double> starts = new LinkedHashMap<>();
    private Set<K> target;
    private double elapsed;

    public Map<K, Double> update(Collection<K> desired, double seconds) {
        return update(desired,seconds,DURATION);
    }
    public Map<K,Double> update(Collection<K> desired,double seconds,double duration){
        duration=Double.isFinite(duration)?Math.max(0.01,Math.min(10,duration)):DURATION;
        Set<K> next = new LinkedHashSet<>(desired);
        if (target == null) {
            next.forEach(key -> weights.put(key, 1.0));
            target = next;
        } else {
            elapsed = Math.min(duration, elapsed + Math.max(0, seconds));
            double t = elapsed / duration;
            double eased = t * t * (3 - 2 * t);
            weights.replaceAll((key, value) -> {
                double start = starts.getOrDefault(key, value);
                return start + ((target.contains(key) ? 1 : 0) - start) * eased;
            });
            if (elapsed >= duration) weights.keySet().retainAll(target);
            if (!target.equals(next)) {
                starts.clear();
                starts.putAll(weights);
                next.forEach(key -> { weights.putIfAbsent(key, 0.0); starts.putIfAbsent(key, 0.0); });
                target = next;
                elapsed = 0;
            }
        }
        return new LinkedHashMap<>(weights);
    }

    public void clear() {
        weights.clear();
        starts.clear();
        target = null;
        elapsed = 0;
    }
}
