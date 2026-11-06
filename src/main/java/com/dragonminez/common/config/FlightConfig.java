package com.dragonminez.common.config;

import com.dragonminez.api.flight.FlightMotion.Tuning;
import com.dragonminez.api.rules.Conditions;
import com.google.gson.*;
import java.util.*;
import com.dragonminez.common.stats.StatsData;

/** Server-owned motion parameters; administrators can disable the shared engine explicitly. */
public record FlightConfig(boolean enabled, JsonObject when, Tuning tuning, double cost, double turnCost, List<FlightConfig> profiles) {
    private static final Set<String> FIELDS = Set.of("enabled","when","thrust","drag","braking","gravity","lift","speed","boost","turn","smoothing","banking","cost","turnCost","pitchRate","yawRate","rollRate","profiles");
    public FlightConfig { profiles = List.copyOf(profiles); }
    public FlightConfig(boolean enabled, JsonObject when, Tuning tuning, double cost, double turnCost) {
        this(enabled, when, tuning, cost, turnCost, List.of());
    }
    public FlightConfig resolve(StatsData data) {
        for (var profile : profiles) if (Conditions.test(new Conditions.Context(data, null), profile.when())) return profile;
        return this;
    }
    public double charge(com.dragonminez.api.flight.FlightMotion.Controls controls) {
        return cost + turnCost * Math.min(1, Math.abs(controls.pitch()) + Math.abs(controls.yaw()) + Math.abs(controls.roll()));
    }
    public static FlightConfig parse(JsonElement value) {
        JsonObject json=value==null?new JsonObject():value.getAsJsonObject();
        if (!FIELDS.containsAll(json.keySet())) throw new IllegalArgumentException("Unknown flight setting");
        if (json.has("enabled") && (!json.get("enabled").isJsonPrimitive() || !json.getAsJsonPrimitive("enabled").isBoolean())) throw new IllegalArgumentException("Flight enabled must be boolean");
        var when=json.has("when")?json.getAsJsonObject("when").deepCopy():new JsonObject(); Conditions.validate(when);
        double turn = number(json, "turn", .15);
        List<FlightConfig> profiles = new ArrayList<>();
        if (json.has("profiles")) {
            if (!json.get("profiles").isJsonArray() || json.getAsJsonArray("profiles").size() > 32) throw new IllegalArgumentException("Flight profiles must be an array of at most 32 entries");
            var base = json.deepCopy(); base.remove("profiles"); base.remove("when");
            for (var entry : json.getAsJsonArray("profiles")) {
                var profile = entry.getAsJsonObject();
                if (profile.has("profiles") || profile.has("enabled") || !FIELDS.containsAll(profile.keySet())) throw new IllegalArgumentException("Flight profile can only override conditions and motion settings");
                var merged = base.deepCopy();
                profile.entrySet().forEach(field -> merged.add(field.getKey(), field.getValue().deepCopy()));
                profiles.add(parse(merged));
            }
        }
        return new FlightConfig(!json.has("enabled")||json.get("enabled").getAsBoolean(),when,
                new Tuning(number(json,"thrust",0.055),number(json,"drag",0.04),number(json,"braking",0.3),number(json,"gravity",0),number(json,"lift",0),number(json,"speed",0.492),number(json,"boost",1.75),turn,number(json,"smoothing",0.35),number(json,"banking",0.2),
                        number(json,"pitchRate",turn),number(json,"yawRate",turn),number(json,"rollRate",turn)),number(json,"cost",0),number(json,"turnCost",0),profiles);
    }
    private static double number(JsonObject json,String key,double fallback) {
        if (!json.has(key)) return fallback;
        var value=json.get(key);
        if (!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Flight setting must be numeric: "+key);
        double result=value.getAsDouble();
        if (!Double.isFinite(result)||result<0||result>1000000) throw new IllegalArgumentException("Flight setting outside bounds: "+key);
        return result;
    }
}
