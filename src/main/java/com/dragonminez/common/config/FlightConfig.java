package com.dragonminez.common.config;

import com.dragonminez.api.flight.FlightMotion.Tuning;
import com.dragonminez.api.rules.Conditions;
import com.google.gson.*;
import java.util.Set;

/** Server-owned motion parameters; administrators can disable the shared engine explicitly. */
public record FlightConfig(boolean enabled, JsonObject when, Tuning tuning, double cost, double turnCost) {
    public static FlightConfig parse(JsonElement value) {
        JsonObject json=value==null?new JsonObject():value.getAsJsonObject();
        if (!Set.of("enabled","when","thrust","drag","braking","gravity","lift","speed","boost","turn","smoothing","banking","cost","turnCost").containsAll(json.keySet())) throw new IllegalArgumentException("Unknown flight setting");
        if (json.has("enabled") && (!json.get("enabled").isJsonPrimitive() || !json.getAsJsonPrimitive("enabled").isBoolean())) throw new IllegalArgumentException("Flight enabled must be boolean");
        var when=json.has("when")?json.getAsJsonObject("when").deepCopy():new JsonObject(); Conditions.validate(when);
        return new FlightConfig(!json.has("enabled")||json.get("enabled").getAsBoolean(),when,
                new Tuning(number(json,"thrust",0.055),number(json,"drag",0.04),number(json,"braking",0.3),number(json,"gravity",0),number(json,"lift",0),number(json,"speed",0.492),number(json,"boost",1.75),number(json,"turn",0.15),number(json,"smoothing",0.35),number(json,"banking",0.2)),number(json,"cost",0),number(json,"turnCost",0));
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
