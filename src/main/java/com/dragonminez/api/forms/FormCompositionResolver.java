package com.dragonminez.api.forms;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Resolves immutable form definitions against explicitly versioned player progression. */
public final class FormCompositionResolver {
    public record Multipliers(double str, double skp, double stm, double def,
                              double vit, double pwr, double ene, double speed) {
        public static final Multipliers IDENTITY = new Multipliers(1, 1, 1, 1, 1, 1, 1, 1);
        public Multipliers {
            for (double value : new double[]{str, skp, stm, def, vit, pwr, ene, speed}) {
                if (!Double.isFinite(value)) throw new IllegalArgumentException("Form values must be finite");
            }
        }
        public Multipliers multiply(Multipliers b) {
            return new Multipliers(str*b.str, skp*b.skp, stm*b.stm, def*b.def,
                    vit*b.vit, pwr*b.pwr, ene*b.ene, speed*b.speed);
        }
        public Multipliers add(Multipliers b) {
            return new Multipliers(str+b.str, skp+b.skp, stm+b.stm, def+b.def,
                    vit+b.vit, pwr+b.pwr, ene+b.ene, speed+b.speed);
        }
        public Multipliers requirePositive() { return requireMinimum(false); }
        public Multipliers requireNonnegative() { return requireMinimum(true); }
        private Multipliers requireMinimum(boolean zeroAllowed) {
            for (double value : new double[]{str, skp, stm, def, vit, pwr, ene, speed})
                if (zeroAllowed?value<0:value<=0)throw new IllegalArgumentException(zeroAllowed?
                        "Additive deltas must be nonnegative":"Form multipliers must be positive");
            return this;
        }
        public double score() { return (str + skp + def + vit + pwr + ene) / 6; }
    }

    public record Definition(String id, String baseForm, String inheritsMultipliersFrom, Multipliers multipliers, String mode) {
        public Definition(String id, String baseForm, String inheritsMultipliersFrom, Multipliers multipliers) {
            this(id, baseForm, inheritsMultipliersFrom, multipliers, "multiply");
        }
        public Definition {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(multipliers, "multipliers");
            mode = mode == null ? "multiply" : mode;
            if (!mode.equals("multiply") && !mode.equals("add")) throw new IllegalArgumentException(id + ": mode must be multiply or add");
            if (mode.equals("multiply")) multipliers.requirePositive();else multipliers.requireNonnegative();
            baseForm = blankToNull(baseForm);
            inheritsMultipliersFrom = blankToNull(inheritsMultipliersFrom);
            if (baseForm != null && inheritsMultipliersFrom != null) {
                throw new IllegalArgumentException(id + ": use baseForm at the root OR inheritsMultipliersFrom for a child, not both");
            }
        }
        private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
        String parent() { return inheritsMultipliersFrom != null ? inheritsMultipliersFrom : baseForm; }
    }

    /** Revision must change whenever unlocks, mastery, race or the relevant progression changes. */
    public record PlayerState(long revision, String current, String currentAnchor, Set<String> unlocked,
                              Map<String, Multipliers> masteryAdjustedLocalMultipliers) {
        public PlayerState {
            unlocked = Collections.unmodifiableSet(new TreeSet<>(unlocked));
            masteryAdjustedLocalMultipliers = Map.copyOf(masteryAdjustedLocalMultipliers);
        }
    }

    private record Snapshot(long generation, Map<String, Definition> definitions) {}
    private static final class PlayerCache {
        final PlayerState state;
        final long generation;
        final Map<String, Multipliers> resolved = new HashMap<>();
        PlayerCache(PlayerState state, long generation) { this.state = state; this.generation = generation; }
    }
    private volatile Snapshot snapshot = new Snapshot(0, Map.of());
    private final Map<UUID, PlayerCache> players = new ConcurrentHashMap<>();

    /** Validates the complete static graph before atomically installing a config snapshot. */
    public synchronized void reload(Collection<Definition> definitions) {
        Map<String, Definition> next = new TreeMap<>();
        for (Definition definition : definitions) {
            if (next.putIfAbsent(definition.id(), definition) != null) {
                throw new IllegalArgumentException("Duplicate form id: " + definition.id());
            }
        }
        Set<String> checked = new HashSet<>();
        for (String id : next.keySet()) validate(id, next, new LinkedHashSet<>(), checked);
        snapshot = new Snapshot(snapshot.generation + 1, Map.copyOf(next));
        players.clear();
    }

    private static void validate(String id, Map<String, Definition> definitions,
                                 LinkedHashSet<String> path, Set<String> checked) {
        if (checked.contains(id)) return;
        if (path.size() >= 256) throw new IllegalArgumentException("Form inheritance exceeds 256 references: " + id);
        if (!path.add(id)) throw new IllegalArgumentException("Form inheritance cycle: " + String.join(" -> ", path) + " -> " + id);
        Definition definition = definitions.get(id);
        if (definition == null) throw new IllegalArgumentException("Unknown form reference: " + String.join(" -> ", path));
        String parent = definition.parent();
        if (parent != null && !isDynamic(parent)) validate(parent, definitions, path, checked);
        path.remove(id);
        checked.add(id);
    }

    public void updatePlayer(UUID player, PlayerState state) {
        Snapshot config = snapshot;
        players.compute(player, (id, old) -> old != null && old.generation == config.generation
                && old.state.equals(state) ? old : new PlayerCache(state, config.generation));
    }

    public void removePlayer(UUID player) { players.remove(player); }

    public Multipliers resolve(UUID player, String formId) {
        Snapshot config = snapshot;
        PlayerCache cache = players.get(player);
        if (cache == null || cache.generation != config.generation) {
            throw new IllegalStateException("Player progression must be supplied after a config reload: " + player);
        }
        synchronized (cache) {
            return cache.resolved.computeIfAbsent(formId,
                    id -> resolve(id, config.definitions, cache.state, new LinkedHashSet<>()));
        }
    }

    private static Multipliers resolve(String id, Map<String, Definition> definitions,
                                       PlayerState state, LinkedHashSet<String> path) {
        Definition definition = definitions.get(id);
        if (definition == null) throw new IllegalArgumentException("Unknown form reference: " + id);
        if (path.size() >= 256) throw new IllegalArgumentException("Dynamic form chain exceeds 256 references: " + id);
        if (!path.add(id)) throw new IllegalArgumentException("Dynamic form cycle: " + String.join(" -> ", path) + " -> " + id);
        try {
            Multipliers parent = Multipliers.IDENTITY;
            String reference = definition.parent();
            if ("best".equals(reference)) {
                double bestScore = parent.score();
                for (String candidate : state.unlocked) {
                    if (touchesPath(candidate, definitions, path, new HashSet<>())) continue;
                    Multipliers resolved = resolve(candidate, definitions, state, new LinkedHashSet<>(path));
                    double score = resolved.score();
                    if (score > bestScore) { parent = resolved; bestScore = score; }
                }
            } else if ("current".equals(reference)) {
                String current = state.current;
                if (!definitions.containsKey(current == null ? "" : current)) current = null;
                if (current != null && touchesPath(current, definitions, path, new HashSet<>())) current = state.currentAnchor;
                if (current != null && definitions.containsKey(current) && !touchesPath(current, definitions, path, new HashSet<>())) {
                    parent = resolve(current, definitions, state, path);
                }
            } else if (reference != null) {
                parent = resolve(reference, definitions, state, path);
            }
            Multipliers local = state.masteryAdjustedLocalMultipliers.getOrDefault(id, definition.multipliers);
            return (definition.mode.equals("add") ? parent.add(local) : parent.multiply(local)).requirePositive();
        } finally { path.remove(id); }
    }

    private static boolean touchesPath(String id, Map<String, Definition> definitions,
                                       Set<String> path, Set<String> visited) {
        if (path.contains(id)) return true;
        if (!visited.add(id)) return false;
        Definition definition = definitions.get(id);
        if (definition == null) throw new IllegalArgumentException("Unknown unlocked form: " + id);
        String parent = definition.parent();
        return parent != null && !isDynamic(parent) && touchesPath(parent, definitions, path, visited);
    }

    private static boolean isDynamic(String reference) { return "best".equals(reference) || "current".equals(reference); }
}
