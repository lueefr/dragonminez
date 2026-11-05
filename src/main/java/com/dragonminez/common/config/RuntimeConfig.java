package com.dragonminez.common.config;

import com.dragonminez.api.rules.Conditions;
import com.dragonminez.api.runtime.EventRules;
import com.dragonminez.common.stats.StatsData;
import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.*;

/** Compiled server rules; content IDs and effect names remain entirely data driven. */
public final class RuntimeConfig {
    private final JsonObject source;
    private final EventRules rules;
    private final Map<String, JsonObject> conditions;
    private final int combatTicks;
    private ReactionConfig reactions;
    public ReactionConfig reactions() { return reactions; }
    private BehaviorConfig behaviors;
    public BehaviorConfig behaviors() { return behaviors; }
    private RuntimeConfig(JsonObject source, EventRules rules, Map<String, JsonObject> conditions, int combatTicks) {
        this.source = source.deepCopy(); this.rules = rules; this.conditions = Map.copyOf(conditions); this.combatTicks = combatTicks;
    }
    public EventRules rules() { return rules; }
    public int combatTicks() { return combatTicks; }
    public String json() { return new GsonBuilder().setPrettyPrinting().create().toJson(source); }
    public Map<String, Boolean> flags(StatsData data) {
        Map<String, Boolean> flags = new LinkedHashMap<>();
        conditions.forEach((key, condition) -> flags.put(key, Conditions.test(new Conditions.Context(data, null), condition)));
        return flags;
    }
    public static RuntimeConfig empty() { return parse("{}"); }
    public static RuntimeConfig parse(String json) {
        var source = JsonParser.parseString(json).getAsJsonObject();
        var engine = source.deepCopy();
        var behavior = engine.remove("behaviors");
        var reactions = engine.remove("reactions");
        int combatTicks = 200;
        if (engine.has("combatTicks")) {
            var value = engine.remove("combatTicks");
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Combat timeout must be numeric");
            double number = value.getAsDouble();
            if (!Double.isFinite(number) || number < 1 || number > 72000 || number != Math.rint(number)) throw new IllegalArgumentException("Combat timeout must be 1..72000 ticks");
            combatTicks = (int) number;
        }
        Map<String, JsonObject> conditions = new LinkedHashMap<>();
        var rules = EventRules.compile(engine, condition -> {
            Conditions.validate(condition);
            String key = Integer.toString(conditions.size()); conditions.put(key, condition.deepCopy());
            return context -> Boolean.TRUE.equals(context.flags().get(key));
        });
        rules.sources().forEach((event, sources) -> {
            if (event.startsWith("dmz:") && !Set.of("dmz:tick", "dmz:form_changed", "dmz:combat_enter", "dmz:combat_exit", "dmz:damage_received", "dmz:damage_dealt", "dmz:evaded").contains(event)) throw new IllegalArgumentException("Unknown native runtime event: " + event);
            for (String value : sources) if (!value.startsWith("meter:") && (!Set.of("event.damage", "event.originalDamage", "event.blockedDamage").contains(value) || !Set.of("dmz:damage_received", "dmz:damage_dealt").contains(event))) throw new IllegalArgumentException("Numeric source unavailable in event " + event + ": " + value);
        });
        conditions.values().forEach(condition -> checkMeters(condition, rules.meters()));
        var result = new RuntimeConfig(source, rules, conditions, combatTicks);
        result.behaviors = BehaviorConfig.parse(behavior, rules.meters());
        result.reactions = ReactionConfig.parse(reactions, rules.meters());
        return result;
    }
    private static void checkMeters(JsonObject condition, Set<String> meters) {
        for (var entry : condition.entrySet()) {
            if (entry.getKey().equals("meter") && !meters.contains(entry.getValue().getAsJsonObject().get("id").getAsString())) throw new IllegalArgumentException("Unknown meter in condition");
            if (entry.getKey().equals("not")) checkMeters(entry.getValue().getAsJsonObject(), meters);
            if (Set.of("all", "any").contains(entry.getKey())) for (var value : entry.getValue().getAsJsonArray()) checkMeters(value.getAsJsonObject(), meters);
        }
    }
    public static RuntimeConfig load(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.writeString(path, "{\n  \"combatTicks\": 200,\n  \"meters\": {},\n  \"rules\": []\n}\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        }
        if (Files.size(path) > 1048576) throw new IllegalArgumentException("Runtime configuration exceeds 1 MiB");
        return parse(Files.readString(path, StandardCharsets.UTF_8));
    }
}
