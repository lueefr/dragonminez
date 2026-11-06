package com.dragonminez.api.runtime;

import com.google.gson.*;
import java.util.*;
import java.util.function.Predicate;

/** Event predicates and numeric expressions read one immutable pre-event snapshot. */
public final class EventRules {
    public record Context(String event, Map<String, Double> values, Map<String, Boolean> flags) {
        public Context(String event, Map<String, Double> values) { this(event, values, Map.of()); }
        public Context { Objects.requireNonNull(event); values = Map.copyOf(values); flags = Map.copyOf(flags); }
    }
    public interface Compiler { Predicate<Context> compile(JsonObject condition); }
    private record Action(String meter, String operation, DynamicValue value) {}
    private record Rule(String event, Predicate<Context> condition, List<Action> actions) {}
    private final Map<String, MeterState.Definition> definitions;
    private final List<Rule> rules;

    private EventRules(Map<String, MeterState.Definition> definitions, List<Rule> rules) {
        this.definitions = Collections.unmodifiableMap(new LinkedHashMap<>(definitions)); this.rules = List.copyOf(rules);
    }
    public Map<String, Set<String>> sources() {
        Map<String, Set<String>> sources = new LinkedHashMap<>();
        for (var rule : rules) for (var action : rule.actions()) sources.computeIfAbsent(rule.event(), key -> new HashSet<>()).addAll(action.value().dependencies());
        Map<String, Set<String>> result = new LinkedHashMap<>();
        sources.forEach((event, values) -> result.put(event, Set.copyOf(values)));
        return Map.copyOf(result);
    }
    public MeterState.Definition definition(String id) { return Objects.requireNonNull(definitions.get(id), "Unknown meter: " + id); }
    public Set<String> meters() { return definitions.keySet(); }
    public boolean empty() { return definitions.isEmpty() && rules.isEmpty(); }
    public MeterState create() { return new MeterState(definitions); }
    public static EventRules compile(JsonObject json, Compiler compiler) {
        Objects.requireNonNull(compiler);
        fields(json, Set.of("meters", "rules"));
        Map<String, MeterState.Definition> definitions = new LinkedHashMap<>();
        if (json.has("meters")) for (var entry : json.getAsJsonObject("meters").entrySet()) {
            MeterState.requireId(entry.getKey()); var meter = entry.getValue().getAsJsonObject();
            fields(meter, Set.of("initial", "min", "max", "decayPerTick", "scope"));
            double minimum = number(meter, "min", 0), maximum = number(meter, "max", 100);
            String scope = meter.has("scope") ? meter.get("scope").getAsString() : "form";
            MeterState.Scope policy = switch (scope) {
                case "character" -> MeterState.Scope.CHARACTER;
                case "combat" -> MeterState.Scope.COMBAT;
                case "form" -> MeterState.Scope.FORM;
                default -> throw new IllegalArgumentException("Unknown meter scope: " + scope);
            };
            definitions.put(entry.getKey(), new MeterState.Definition(number(meter, "initial", minimum), minimum, maximum, number(meter, "decayPerTick", 0), policy));
        }
        if (definitions.size() > 128) throw new IllegalArgumentException("At most 128 meter definitions");
        List<Rule> rules = new ArrayList<>();
        if (json.has("rules")) for (var element : json.getAsJsonArray("rules")) {
            var rule = element.getAsJsonObject(); fields(rule, Set.of("event", "when", "actions"));
            String event = rule.get("event").getAsString(); MeterState.requireId(event);
            Predicate<Context> condition = Objects.requireNonNull(compiler.compile(rule.has("when") ? rule.getAsJsonObject("when").deepCopy() : new JsonObject()));
            List<Action> actions = new ArrayList<>();
            for (var value : rule.getAsJsonArray("actions")) {
                var action = value.getAsJsonObject(); fields(action, Set.of("meter", "operation", "value"));
                String meter = action.get("meter").getAsString(), operation = action.get("operation").getAsString();
                if (!definitions.containsKey(meter)) throw new IllegalArgumentException("Unknown action meter: " + meter);
                if (!Set.of("add", "set").contains(operation)) throw new IllegalArgumentException("Unknown meter operation: " + operation);
                var expression = DynamicValue.compile(action.get("value"));
                for (String dependency : expression.dependencies()) if (dependency.startsWith("meter:") && !definitions.containsKey(dependency.substring(6))) throw new IllegalArgumentException("Unknown source meter: " + dependency);
                actions.add(new Action(meter, operation, expression));
            }
            if (actions.isEmpty() || actions.size() > 64) throw new IllegalArgumentException("Rule needs 1..64 actions");
            rules.add(new Rule(event, condition, List.copyOf(actions)));
        }
        if (rules.size() > 256) throw new IllegalArgumentException("At most 256 event rules");
        return new EventRules(definitions, rules);
    }
    public void dispatch(MeterState state, Context event) {
        if (!state.matches(definitions)) throw new IllegalArgumentException("Meter state belongs to another definition");
        Map<String, Double> context = new HashMap<>();
        event.values().forEach((key, value) -> { if (key.startsWith("meter:")) throw new IllegalArgumentException("Event cannot override meter sources"); context.put(key, DynamicValue.finite(value)); });
        state.snapshot().forEach((id, value) -> context.put("meter:" + id, value));
        Context snapshot = new Context(event.event(), context, event.flags());
        Map<String, Double> updates = new LinkedHashMap<>();
        for (var rule : rules) if (rule.event().equals(event.event()) && rule.condition().test(snapshot)) for (var action : rule.actions()) {
            double value = action.value().evaluate(snapshot.values());
            if (action.operation().equals("add")) value = DynamicValue.finite(updates.getOrDefault(action.meter(), state.value(action.meter())) + value);
            updates.put(action.meter(), definitions.get(action.meter()).bound(value));
        }
        state.commit(updates);
    }
    public record Change(String meter, boolean additive, double value) {}
    public void adjust(MeterState state, List<Change> actions) {
        if (!state.matches(definitions) || actions.size() > 64) throw new IllegalArgumentException("Invalid script command batch");
        Map<String, Double> updates = new LinkedHashMap<>();
        for (var action : actions) {
            var definition = definitions.get(action.meter());
            if (definition == null) throw new IllegalArgumentException("Unknown script meter: " + action.meter());
            double value = DynamicValue.finite(action.value());
            if (action.additive()) value = DynamicValue.finite(updates.getOrDefault(action.meter(), state.value(action.meter())) + value);
            updates.put(action.meter(), definition.bound(value));
        }
        state.commit(updates);
    }
    private static void fields(JsonObject json, Set<String> allowed) {
        for (String field : json.keySet()) if (!allowed.contains(field)) throw new IllegalArgumentException("Unknown runtime field: " + field);
    }
    private static double number(JsonObject json, String key, double fallback) {
        if (!json.has(key)) return fallback;
        var value = json.get(key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Expected numeric field: " + key);
        return DynamicValue.finite(value.getAsDouble());
    }
}
