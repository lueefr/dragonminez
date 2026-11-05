package com.dragonminez.api.runtime;

import com.google.gson.*;
import java.util.*;

/** Owned by one character. Temporary scopes never enter the persistent snapshot. */
public final class MeterState {
    public enum Scope { CHARACTER, COMBAT, FORM }
    public record Definition(double initial, double minimum, double maximum, double decay, Scope scope) {
        public Definition {
            DynamicValue.finite(initial); DynamicValue.finite(minimum); DynamicValue.finite(maximum); DynamicValue.finite(decay);
            if (minimum > maximum || initial < minimum || initial > maximum || decay < 0 || scope == null) throw new IllegalArgumentException("Invalid meter definition");
        }
        double bound(double value) { return Math.clamp(DynamicValue.finite(value), minimum, maximum); }
    }
    private final Map<String, Definition> definitions;
    private final Map<String, Double> values = new LinkedHashMap<>();

    public MeterState(Map<String, Definition> definitions) {
        definitions.forEach((id, definition) -> { requireId(id); Objects.requireNonNull(definition); });
        this.definitions = Collections.unmodifiableMap(new LinkedHashMap<>(definitions));
        definitions.forEach((id, definition) -> values.put(id, definition.initial()));
    }
    static void requireId(String id) {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IllegalArgumentException("Expected namespaced meter ID");
    }
    boolean matches(Map<String, Definition> expected) { return definitions.equals(expected); }
    public double value(String id) { require(id); return values.get(id); }
    public void set(String id, double value) { values.put(id, require(id).bound(value)); }
    public void add(String id, double amount) { set(id, DynamicValue.finite(value(id) + DynamicValue.finite(amount))); }
    public void reset(Scope scope) { definitions.forEach((id, definition) -> { if (definition.scope() == scope) values.put(id, definition.initial()); }); }
    public Map<String, Double> snapshot() { return Map.copyOf(values); }
    public void tick() {
        Map<String, Double> next = new LinkedHashMap<>();
        definitions.forEach((id, definition) -> {
            double current = value(id), initial = definition.initial(), decay = definition.decay();
            next.put(id, current > initial ? Math.max(initial, current - decay) : Math.min(initial, current + decay));
        });
        commit(next);
    }
    void commit(Map<String, Double> updates) {
        Map<String, Double> next = new LinkedHashMap<>();
        updates.forEach((id, value) -> next.put(id, require(id).bound(value)));
        values.putAll(next);
    }
    private Definition require(String id) {
        Definition definition = definitions.get(id);
        if (definition == null) throw new IllegalArgumentException("Unknown meter: " + id);
        return definition;
    }
    public JsonObject save() {
        var result = new JsonObject();
        definitions.forEach((id, definition) -> { if (definition.scope() == Scope.CHARACTER) result.addProperty(id, value(id)); });
        return result;
    }
    public void load(JsonObject saved) {
        Map<String, Double> next = new LinkedHashMap<>();
        definitions.forEach((id, definition) -> {
            JsonElement value = saved.get(id);
            double number = definition.initial();
            if (definition.scope() == Scope.CHARACTER && value != null) {
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Saved meter must be numeric: " + id);
                number = value.getAsDouble();
            }
            next.put(id, definition.bound(number));
        });
        commit(next);
    }
}
