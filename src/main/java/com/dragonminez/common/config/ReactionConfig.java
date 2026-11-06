package com.dragonminez.common.config;

import com.dragonminez.api.rules.Conditions;
import com.dragonminez.api.runtime.DynamicValue;
import com.dragonminez.common.stats.StatsData;
import com.google.gson.*;
import java.util.*;

/** Hit probability is explicit; it is never interpreted as dodge probability. */
public final class ReactionConfig {
    public record Rule(String id, JsonObject when, Set<String> attacks, DynamicValue chance, String power, Map<String, Double> weights, int interval, double distance, double energy, double stamina, int cooldown, String response, double damage, double counter, double range) {}
    private final List<Rule> rules = new ArrayList<>();
    private final Map<String,Set<String>> projectiles=new HashMap<>();
    public static ReactionConfig parse(JsonElement json, Set<String> meters) {
        var result = new ReactionConfig();
        if (json == null) return result;
        Set<String> ids = new HashSet<>();
        for (var element : json.getAsJsonArray()) {
            var rule = element.getAsJsonObject();
            if (!Set.of("id", "when", "attacks", "hitChance", "powerSource", "distance", "energyCost", "staminaCost", "cooldownTicks", "powerWeights", "reactionTicks", "response", "damageFactor", "counterFactor", "counterRange", "projectiles").containsAll(rule.keySet())) throw new IllegalArgumentException("Unknown reaction field");
            String id = rule.get("id").getAsString(); com.dragonminez.api.content.ContentProgress.requireId(id);
            if (!ids.add(id)) throw new IllegalArgumentException("Duplicate reaction ID");
            if(rule.has("projectiles")){
                Set<String> types=new HashSet<>();for(var value:rule.getAsJsonArray("projectiles")){
                    String type=value.getAsString();com.dragonminez.api.content.ContentProgress.requireId(type);
                    if(Conditions.ready()&&!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(net.minecraft.resources.ResourceLocation.parse(type)))throw new IllegalArgumentException("Unknown projectile entity type: "+type);
                    types.add(type);
                }
                if(types.isEmpty()||types.size()>64)throw new IllegalArgumentException("Projectile type list needs 1..64 IDs");
                result.projectiles.put(id,Set.copyOf(types));
            }
            var when = rule.has("when") ? rule.getAsJsonObject("when").deepCopy() : new JsonObject(); Conditions.validate(when);
            Set<String> attacks = new LinkedHashSet<>();
            if (rule.has("attacks")) rule.getAsJsonArray("attacks").forEach(value -> attacks.add(value.getAsString())); else attacks.add("melee");
            if (attacks.isEmpty() || !Set.of("melee", "projectile").containsAll(attacks)) throw new IllegalArgumentException("Unsupported reaction attack type");
            var chance = DynamicValue.compile(rule.get("hitChance"));
            for (String source : chance.dependencies()) if (!source.equals("event.powerRatio") && !(source.startsWith("meter:") && meters.contains(source.substring(6)))) throw new IllegalArgumentException("Unknown hit chance source: " + source);
            String power = rule.has("powerSource") ? rule.get("powerSource").getAsString() : "battlePower";
            if (!Set.of("battlePower", "STR", "PWR", "SPEED").contains(power)) throw new IllegalArgumentException("Unknown reaction power source");
            Map<String, Double> weights = new LinkedHashMap<>();
            if (rule.has("powerWeights")) {
                if (rule.has("powerSource")) throw new IllegalArgumentException("Use powerSource or powerWeights, not both");
                var values = rule.getAsJsonObject("powerWeights");
                if (values.size() == 0 || !Set.of("battlePower", "STR", "PWR", "SPEED").containsAll(values.keySet())) throw new IllegalArgumentException("Invalid reaction power weights");
                for (String key : values.keySet()) weights.put(key, number(values, key, 0, 0, 1000000));
                if (weights.values().stream().mapToDouble(Double::doubleValue).sum() <= 0) throw new IllegalArgumentException("Power weights cannot all be zero");
            }
            double interval = number(rule, "reactionTicks", 0, 0, 72000);
            if (interval != Math.rint(interval)) throw new IllegalArgumentException("Reaction interval must be whole ticks");
            double cooldown = number(rule, "cooldownTicks", 10, 0, 72000);
            if (cooldown != Math.rint(cooldown)) throw new IllegalArgumentException("Reaction cooldown must be whole ticks");
            String response = rule.has("response") ? rule.get("response").getAsString() : "dodge";
            if (!Set.of("dodge", "block", "counter", "destroy", "deflect").contains(response)) throw new IllegalArgumentException("Unknown reaction response");
            if(Set.of("destroy","deflect").contains(response)){
                if(!attacks.equals(Set.of("projectile")))throw new IllegalArgumentException("Projectile responses require projectile-only attacks");
                if(rule.has("damageFactor")||rule.has("counterFactor")||rule.has("counterRange"))throw new IllegalArgumentException("Projectile responses do not use block/counter factors");
            }
            if (response.equals("dodge") && (rule.has("damageFactor") || rule.has("counterFactor") || rule.has("counterRange"))) throw new IllegalArgumentException("Dodge does not use defense factors");
            if (response.equals("block") && (rule.has("counterFactor") || rule.has("counterRange"))) throw new IllegalArgumentException("Block does not use counter settings");
            double damage = number(rule, "damageFactor", 0, 0, 1), counter = number(rule, "counterFactor", 0.5, 0, 10);
            if (response.equals("block") && damage == 1) throw new IllegalArgumentException("Block must reduce damage");
            if (response.equals("counter") && counter == 0) throw new IllegalArgumentException("Counter factor must be positive");
            result.rules.add(new Rule(id, when, Set.copyOf(attacks), chance, power, Map.copyOf(weights), (int) interval, number(rule, "distance", 1, 0.1, 4), number(rule, "energyCost", 0, 0, 1), number(rule, "staminaCost", 0.02, 0, 1), (int) cooldown, response, damage, counter, number(rule, "counterRange", 3, 0.1, 32)));
        }
        if (result.rules.size() > 64) throw new IllegalArgumentException("At most 64 reaction rules");
        return result;
    }
    public Rule select(StatsData data,String attack){return select(data,attack,null);}
    public Rule select(StatsData data, String attack, net.minecraft.world.entity.projectile.Projectile projectile) {
        for (var rule : rules) if (rule.attacks().contains(attack) && (!projectiles.containsKey(rule.id())||projectile!=null&&projectiles.get(rule.id()).contains(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(projectile.getType()).toString())) && Conditions.test(new Conditions.Context(data, null), rule.when())) return rule;
        return null;
    }
    public static double weighted(Map<String, Double> weights, java.util.function.ToDoubleFunction<String> source) {
        if (weights.isEmpty()) throw new IllegalArgumentException("Power weights required");
        Map<String, Double> values = new LinkedHashMap<>(); double maximum = 0, total = 0;
        for (var entry : weights.entrySet()) {
            double weight = DynamicValue.finite(entry.getValue());
            if (weight < 0) throw new IllegalArgumentException("Negative power weight");
            if (weight == 0) continue;
            double value = DynamicValue.finite(source.applyAsDouble(entry.getKey()));
            if (value < 0) throw new IllegalArgumentException("Negative power score");
            values.put(entry.getKey(), value); maximum = Math.max(maximum, value); total = DynamicValue.finite(total + weight);
        }
        if (total <= 0) throw new IllegalArgumentException("Power weights cannot all be zero");
        if (maximum == 0) return 0;
        double result = 0;
        for (var entry : values.entrySet()) result += (entry.getValue() / maximum) * (weights.get(entry.getKey()) / total);
        return DynamicValue.finite(maximum * Math.min(1, result));
    }
    public static double ratio(double attacker, double defender) {
        DynamicValue.finite(attacker); DynamicValue.finite(defender);
        if (attacker < 0 || defender < 0) throw new IllegalArgumentException("Power cannot be negative");
        if (attacker == defender) return 1;
        return defender == 0 ? Double.MAX_VALUE : Math.min(Double.MAX_VALUE, attacker / defender);
    }
    private final Set<String> failed = new HashSet<>();
    public void disable(Rule rule, String reason) {
        if (failed.add(rule.id())) com.dragonminez.LogUtil.error(com.dragonminez.Env.COMMON, "Reaction {} failed: {}. Disabled until reload.", rule.id(), reason);
    }
    public boolean enabled(Rule rule) { return !failed.contains(rule.id()); }
    public double chance(Rule rule, StatsData data, double attacker, double defender) {
        if (failed.contains(rule.id())) return 1;
        try {
        Map<String, Double> values = new HashMap<>(data.getRuntime().numbers(data));
        values.put("event.powerRatio", ratio(attacker, defender));
        return Math.clamp(rule.chance().evaluate(values), 0, 1);
        } catch (RuntimeException ex) { disable(rule, ex.getMessage()); return 1; }
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
