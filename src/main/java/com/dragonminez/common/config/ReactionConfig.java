package com.dragonminez.common.config;

import com.dragonminez.api.rules.Conditions;
import com.dragonminez.api.runtime.DynamicValue;
import com.dragonminez.common.stats.StatsData;
import com.google.gson.*;
import java.util.*;

/** Hit probability is explicit; it is never interpreted as dodge probability. */
public final class ReactionConfig {
    public record Rule(String id, JsonObject when, Set<String> attacks, DynamicValue chance, String power, double distance, double energy, double stamina, int cooldown) {}
    private final List<Rule> rules = new ArrayList<>();
    public static ReactionConfig parse(JsonElement json, Set<String> meters) {
        var result = new ReactionConfig();
        if (json == null) return result;
        Set<String> ids = new HashSet<>();
        for (var element : json.getAsJsonArray()) {
            var rule = element.getAsJsonObject();
            if (!Set.of("id", "when", "attacks", "hitChance", "powerSource", "distance", "energyCost", "staminaCost", "cooldownTicks").containsAll(rule.keySet())) throw new IllegalArgumentException("Unknown reaction field");
            String id = rule.get("id").getAsString(); com.dragonminez.api.content.ContentProgress.requireId(id);
            if (!ids.add(id)) throw new IllegalArgumentException("Duplicate reaction ID");
            var when = rule.has("when") ? rule.getAsJsonObject("when").deepCopy() : new JsonObject(); Conditions.validate(when);
            Set<String> attacks = new LinkedHashSet<>();
            if (rule.has("attacks")) rule.getAsJsonArray("attacks").forEach(value -> attacks.add(value.getAsString())); else attacks.add("melee");
            if (attacks.isEmpty() || !Set.of("melee", "projectile").containsAll(attacks)) throw new IllegalArgumentException("Unsupported reaction attack type");
            var chance = DynamicValue.compile(rule.get("hitChance"));
            for (String source : chance.dependencies()) if (!source.equals("event.powerRatio") && !(source.startsWith("meter:") && meters.contains(source.substring(6)))) throw new IllegalArgumentException("Unknown hit chance source: " + source);
            String power = rule.has("powerSource") ? rule.get("powerSource").getAsString() : "battlePower";
            if (!Set.of("battlePower", "STR", "PWR", "SPEED").contains(power)) throw new IllegalArgumentException("Unknown reaction power source");
            double cooldown = number(rule, "cooldownTicks", 10, 0, 72000);
            if (cooldown != Math.rint(cooldown)) throw new IllegalArgumentException("Reaction cooldown must be whole ticks");
            result.rules.add(new Rule(id, when, Set.copyOf(attacks), chance, power, number(rule, "distance", 1, 0.1, 4), number(rule, "energyCost", 0, 0, 1), number(rule, "staminaCost", 0.02, 0, 1), (int) cooldown));
        }
        if (result.rules.size() > 64) throw new IllegalArgumentException("At most 64 reaction rules");
        return result;
    }
    public Rule select(StatsData data, String attack) {
        for (var rule : rules) if (rule.attacks().contains(attack) && Conditions.test(new Conditions.Context(data, null), rule.when())) return rule;
        return null;
    }
    public static double ratio(double attacker, double defender) {
        DynamicValue.finite(attacker); DynamicValue.finite(defender);
        if (attacker < 0 || defender < 0) throw new IllegalArgumentException("Power cannot be negative");
        if (attacker == defender) return 1;
        return defender == 0 ? Double.MAX_VALUE : Math.min(Double.MAX_VALUE, attacker / defender);
    }
    private final Set<String> failed = new HashSet<>();
    public double chance(Rule rule, StatsData data, double attacker, double defender) {
        if (failed.contains(rule.id())) return 1;
        try {
        Map<String, Double> values = new HashMap<>(data.getRuntime().numbers(data));
        values.put("event.powerRatio", ratio(attacker, defender));
        return Math.clamp(rule.chance().evaluate(values), 0, 1);
        } catch (RuntimeException ex) { failed.add(rule.id()); com.dragonminez.LogUtil.error(com.dragonminez.Env.COMMON, "Reaction {} failed: {}. Disabled until reload.", rule.id(), ex.getMessage()); return 1; }
    }
    private static double number(JsonObject root, String key, double fallback, double minimum, double maximum) {
        if (!root.has(key)) return fallback;
        var entry = root.get(key);
        if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Reaction field must be numeric: " + key);
        double value = entry.getAsDouble();
        if (!Double.isFinite(value) || value < minimum || value > maximum) throw new IllegalArgumentException("Reaction field out of bounds: " + key);
        return value;
    }
}
