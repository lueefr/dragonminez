package com.dragonminez.api.runtime;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RuntimeTest {
    private static JsonObject object(String value) { return JsonParser.parseString(value).getAsJsonObject(); }
    private static EventRules compile(String value) {
        return EventRules.compile(object(value), condition -> {
            if (!condition.isEmpty()) throw new IllegalArgumentException("Unknown test condition");
            return context -> true;
        });
    }
    @Test void forceCurve() {
        var expression = DynamicValue.compile(object("""
            {"curve":{"input":{"source":"event.powerRatio"},"interpolation":"smooth","points":[[0.5,0],[1,0.5],[2,1]]}}
            """));
        assertEquals(Set.of("event.powerRatio"), expression.dependencies());
        assertEquals(0, expression.evaluate(Map.of("event.powerRatio", 0.1)));
        assertEquals(0.5, expression.evaluate(Map.of("event.powerRatio", 1.0)));
        assertEquals(1, expression.evaluate(Map.of("event.powerRatio", 3.0)));
        double previous = 0;
        for (double ratio = 0.5; ratio <= 2; ratio += 0.01) {
            double chance = expression.evaluate(Map.of("event.powerRatio", ratio));
            assertTrue(chance >= previous && chance <= 1); previous = chance;
        }
    }
    @Test void rejectsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> DynamicValue.compile(object("{\"curve\":{\"input\":1,\"points\":[[1,0],[1,1]]}}")));
        assertThrows(IllegalArgumentException.class, () -> DynamicValue.compile(object("{\"source\":\"event.damage\"}")).evaluate(Map.of()));
        assertThrows(IllegalArgumentException.class, () -> DynamicValue.compile(object("{\"multiply\":[1e308,1e308]}")).evaluate(Map.of()));
        assertThrows(IllegalArgumentException.class, () -> compile("{\"meters\":{\"invalid\":{}}}"));
        assertThrows(IllegalArgumentException.class, () -> compile("{\"meters\":{\"example:focus\":{\"max\":-1}}}"));
        String nested = "1";
        for (int index = 0; index < 35; index++) nested = "{\"add\":[1," + nested + "]}";
        String expression = nested;
        assertThrows(IllegalArgumentException.class, () -> DynamicValue.compile(JsonParser.parseString(expression)));
    }
    @Test void eventSnapshot() {
        var rules = compile("""
            {"meters":{"example:spirit":{},"example:seen":{}},"rules":[
              {"event":"example:damage","actions":[{"meter":"example:spirit","operation":"add","value":{"source":"event.damage"}}]},
              {"event":"example:damage","actions":[{"meter":"example:seen","operation":"set","value":{"source":"meter:example:spirit"}}]}
            ]}
            """);
        var state = rules.create();
        rules.dispatch(state, new EventRules.Context("example:damage", Map.of("event.damage", 10.0)));
        assertEquals(10, state.value("example:spirit")); assertEquals(0, state.value("example:seen"));
        rules.dispatch(state, new EventRules.Context("example:damage", Map.of("event.damage", 5.0)));
        assertEquals(15, state.value("example:spirit")); assertEquals(10, state.value("example:seen"));
    }
    @Test void eventRollback() {
        var rules = compile("""
            {"meters":{"example:spirit":{}},"rules":[
              {"event":"example:damage","actions":[{"meter":"example:spirit","operation":"add","value":10}]},
              {"event":"example:damage","actions":[{"meter":"example:spirit","operation":"add","value":{"source":"event.missing"}}]}
            ]}
            """);
        var state = rules.create();
        assertThrows(IllegalArgumentException.class, () -> rules.dispatch(state, new EventRules.Context("example:damage", Map.of())));
        assertEquals(0, state.value("example:spirit"));
        assertThrows(IllegalArgumentException.class, () -> rules.dispatch(state, new EventRules.Context("example:damage", Map.of("meter:example:spirit", 99.0))));
    }
    @Test void rejectsForeign() {
        var first = compile("{\"meters\":{\"example:focus\":{\"max\":100}}}");
        var second = compile("{\"meters\":{\"example:focus\":{\"max\":200}}}");
        assertThrows(IllegalArgumentException.class, () -> first.dispatch(second.create(), new EventRules.Context("example:test", Map.of())));
    }
    @Test void persistenceScopes() {
        var rules = compile("""
            {"meters":{
              "example:permanent":{"scope":"character","initial":5,"decayPerTick":2},
              "example:combat":{"scope":"combat"},"example:form":{"scope":"form"}
            }}
            """);
        var state = rules.create();
        state.set("example:permanent", 20); state.set("example:combat", 50); state.set("example:form", 50);
        state.tick(); assertEquals(18, state.value("example:permanent"));
        assertEquals(Set.of("example:permanent"), state.save().keySet());
        var restored = rules.create(); restored.load(state.save());
        assertEquals(18, restored.value("example:permanent")); assertEquals(0, restored.value("example:combat"));
        state.reset(MeterState.Scope.FORM);
        assertEquals(0, state.value("example:form")); assertEquals(50, state.value("example:combat"));
        assertThrows(IllegalArgumentException.class, () -> restored.load(object("{\"example:permanent\":\"broken\"}")));
        assertEquals(18, restored.value("example:permanent"));
    }
}
