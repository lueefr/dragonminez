package com.dragonminez.api.forms;

import com.dragonminez.api.runtime.DynamicValue;
import com.dragonminez.api.script.LuaBindings;
import com.dragonminez.common.config.ConfigManager;
import com.google.gson.JsonElement;
import java.util.*;

/** Compiled presentation expressions read synchronized snapshots and cannot change gameplay. */
public final class VisualValue {
    private static final Map<JsonElement,Compiled> CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final class Compiled {
        private final DynamicValue expression;
        private final long generation;
        private boolean failed;
        private Compiled(DynamicValue expression) { this.expression=expression; generation=ConfigManager.getFormConfigGeneration(); }
    }
    private VisualValue() {}
    private static synchronized Compiled compile(JsonElement value) {
        var cached = CACHE.get(value);
        if (cached != null && cached.generation == ConfigManager.getFormConfigGeneration()) return cached;
        var expression = DynamicValue.compile(value);
        for (String source : expression.dependencies()) {
            if (source.startsWith("meter:")) com.dragonminez.api.content.ContentProgress.requireId(source.substring(6));
            else if (!LuaBindings.SOURCES.contains(source) || source.startsWith("event.")) throw new IllegalArgumentException("Unknown presentation source: " + source);
        }
        var result = new Compiled(expression); CACHE.put(value, result); return result;
    }
    public static void validate(JsonElement value) { compile(value); }
    public static double evaluate(JsonElement value, Map<String,Double> numbers, double fallback, double minimum, double maximum) {
        if (value == null) return fallback;
        var compiled = compile(value);
        if (compiled.failed) return fallback;
        try { return Math.clamp(compiled.expression.evaluate(numbers), minimum, maximum); }
        catch (RuntimeException ex) {
            compiled.failed = true;
            com.dragonminez.LogUtil.error(com.dragonminez.Env.COMMON, "Presentation expression failed: {}. Default used until reload.", ex.getMessage());
            return fallback;
        }
    }
}
