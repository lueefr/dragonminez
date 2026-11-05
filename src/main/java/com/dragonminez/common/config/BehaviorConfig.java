package com.dragonminez.common.config;

import com.dragonminez.api.rules.Conditions;
import com.dragonminez.api.runtime.DynamicValue;
import com.dragonminez.common.stats.StatsData;
import com.google.gson.*;
import java.util.*;

/** Conditional queries return factors; maintenance is executed separately by the server tick. */
public final class BehaviorConfig {
    public record Attack(int active, int recovery) {}
    private final Map<String, Attack> attacks = new LinkedHashMap<>();
    public Attack attack(String id) { return attacks.getOrDefault(id, new Attack(1, 0)); }
    public record Lifetime(JsonObject when, int duration, int cooldown, String fallback) {}
    private record Modifier(JsonObject when, Map<String, DynamicValue> factors) {}
    private final Map<String, Lifetime> lifetimes = new LinkedHashMap<>();
    private final List<Modifier> modifiers = new ArrayList<>();
    private static final Set<String> TARGETS = Set.of("STR", "SKP", "RES", "DEF", "STM", "VIT", "PWR", "ENE", "SPEED", "energyDrain", "staminaDrain", "healthDrain");
    public static BehaviorConfig parse(JsonElement value, Set<String> meters) {
        var config = new BehaviorConfig();
        if (value == null) return config;
        var root = value.getAsJsonObject(); fields(root, Set.of("forms", "modifiers", "attacks"));
        if (root.has("attacks")) root.getAsJsonObject("attacks").entrySet().forEach(entry -> {
            if (entry.getKey().isBlank()) throw new IllegalArgumentException("Attack needs a technique ID");
            var rule = entry.getValue().getAsJsonObject(); fields(rule, Set.of("activeTicks", "recoveryTicks"));
            int active = integer(rule, "activeTicks", 1), recovery = integer(rule, "recoveryTicks", 0);
            if (active < 1 || active > 72000 || recovery > 72000) throw new IllegalArgumentException("Attack phases must be within 0..72000 ticks with nonzero active time");
            config.attacks.put(entry.getKey(), new Attack(active, recovery));
        });
        if (config.attacks.size() > 256) throw new IllegalArgumentException("Too many attack phase policies");
        if (root.has("forms")) root.getAsJsonObject("forms").entrySet().forEach(entry -> {
            Conditions.form(entry.getKey()); var rule = entry.getValue().getAsJsonObject();
            fields(rule, Set.of("when", "durationTicks", "cooldownTicks", "fallback"));
            var when = rule.has("when") ? rule.getAsJsonObject("when").deepCopy() : new JsonObject(); Conditions.validate(when);
            String fallback = rule.has("fallback") ? rule.get("fallback").getAsString() : "";
            if (!fallback.isEmpty()) Conditions.form(fallback);
            if (fallback.equals(entry.getKey())) throw new IllegalArgumentException("Form cannot fall back to itself");
            config.lifetimes.put(entry.getKey(), new Lifetime(when, integer(rule, "durationTicks", -1), integer(rule, "cooldownTicks", 0), fallback));
        });
        if (root.has("modifiers")) for (var element : root.getAsJsonArray("modifiers")) {
            var rule = element.getAsJsonObject(); fields(rule, Set.of("when", "factors"));
            var when = rule.has("when") ? rule.getAsJsonObject("when").deepCopy() : new JsonObject(); Conditions.validate(when);
            Map<String, DynamicValue> factors = new LinkedHashMap<>();
            for (var entry : rule.getAsJsonObject("factors").entrySet()) {
                if (!TARGETS.contains(entry.getKey())) throw new IllegalArgumentException("Unknown modifier target: " + entry.getKey());
                var expression = DynamicValue.compile(entry.getValue());
                for (String source : expression.dependencies()) {
                    if (source.startsWith("meter:") ? !meters.contains(source.substring(6)) : !Set.of("state.formTicks", "state.stackTicks", "self.healthPercent", "attack.charging", "attack.active", "attack.recovery", "attack.ticks", "attack.charge").contains(source)) throw new IllegalArgumentException("Unknown modifier source: " + source);
                }
                factors.put(entry.getKey(), expression);
            }
            config.modifiers.add(new Modifier(when, Map.copyOf(factors)));
        }
        if (config.modifiers.size() > 128 || config.lifetimes.size() > 256) throw new IllegalArgumentException("Too many form behaviors");
        return config;
    }
    public Lifetime lifetime(String reference) { return lifetimes.get(reference); }
    private boolean failed;
    private final ThreadLocal<Set<StatsData>> evaluating = ThreadLocal.withInitial(HashSet::new);
    public double factor(StatsData data, String target) {
        if (failed || modifiers.isEmpty() || !evaluating.get().add(data)) return 1;
        try {
            double result = 1;
            for (var modifier : modifiers) {
                var expression = modifier.factors().get(target);
                if (expression == null || !Conditions.test(new Conditions.Context(data, null), modifier.when())) continue;
                double value = expression.evaluate(data.getRuntime().numbers(data));
                if (value < 0) throw new IllegalArgumentException("Modifier factors must be nonnegative");
                result = DynamicValue.finite(result * value);
            }
            return result;
        } catch (RuntimeException ex) {
            failed = true;
            com.dragonminez.LogUtil.error(com.dragonminez.Env.COMMON, "Runtime modifier failed: {}. Modifiers disabled until reload.", ex.getMessage());
            return 1;
        } finally { evaluating.get().remove(data); if (evaluating.get().isEmpty()) evaluating.remove(); }
    }
    private static int integer(JsonObject root, String key, int fallback) {
        if (!root.has(key)) return fallback;
        double value = root.get(key).getAsDouble();
        if (!Double.isFinite(value) || value != Math.rint(value) || value < (key.equals("durationTicks") ? -1 : 0) || value > Integer.MAX_VALUE) throw new IllegalArgumentException("Invalid behavior duration/cooldown");
        return (int) value;
    }
    private static void fields(JsonObject value, Set<String> allowed) {
        if (!allowed.containsAll(value.keySet())) throw new IllegalArgumentException("Unknown behavior field");
    }
}
