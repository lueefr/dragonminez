package com.dragonminez.api.runtime;

import com.google.gson.*;
import java.util.*;

/** Immutable expressions compiled at load time; evaluation never changes player state. */
public interface DynamicValue {
    double evaluate(Map<String, Double> context);
    Set<String> dependencies();

    static DynamicValue compile(JsonElement json) {
        return parse(json, 0, new int[]{0});
    }

    private static DynamicValue parse(JsonElement json, int depth, int[] count) {
        if (depth > 32 || ++count[0] > 256) throw new IllegalArgumentException("Expression exceeds complexity limit");
        if (json == null || json.isJsonNull()) throw new IllegalArgumentException("Missing numeric expression");
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
            double number = finite(json.getAsDouble());
            return node(Set.of(), context -> number);
        }
        if (!json.isJsonObject()) throw new IllegalArgumentException("Expected number or expression object");
        var object = json.getAsJsonObject();
        if (object.size() != 1) throw new IllegalArgumentException("Expression needs exactly one operation");
        var entry = object.entrySet().iterator().next();
        String operation = entry.getKey();
        if (operation.equals("source")) {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Source must be a string");
            String source = entry.getValue().getAsString();
            if (!source.matches("[a-z][a-zA-Z0-9_.:/-]{0,255}")) throw new IllegalArgumentException("Invalid numeric source");
            return node(Set.of(source), context -> {
                Double value = context.get(source);
                if (value == null) throw new IllegalArgumentException("Missing numeric source: " + source);
                return finite(value);
            });
        }
        if (operation.equals("curve")) {
            var curve = entry.getValue().getAsJsonObject();
            if (!Set.of("input", "points", "interpolation").containsAll(curve.keySet()) || !curve.has("input") || !curve.has("points")) throw new IllegalArgumentException("Invalid curve fields");
            DynamicValue input = parse(curve.get("input"), depth + 1, count);
            String mode = curve.has("interpolation") ? curve.get("interpolation").getAsString() : "linear";
            if (!Set.of("linear", "smooth").contains(mode)) throw new IllegalArgumentException("Unknown curve interpolation");
            var points = curve.getAsJsonArray("points");
            if (points.size() < 2 || points.size() > 64) throw new IllegalArgumentException("Curve needs 2..64 points");
            double[] xs = new double[points.size()], ys = new double[points.size()];
            for (int index = 0; index < points.size(); index++) {
                var point = points.get(index).getAsJsonArray();
                if (point.size() != 2) throw new IllegalArgumentException("Curve point needs input and output");
                xs[index] = number(point.get(0)); ys[index] = number(point.get(1));
                if (index > 0 && xs[index] <= xs[index - 1]) throw new IllegalArgumentException("Curve inputs must increase");
            }
            return node(input.dependencies(), context -> {
                double value = input.evaluate(context);
                if (value <= xs[0]) return ys[0];
                for (int index = 1; index < xs.length; index++) if (value <= xs[index]) {
                    double fraction = finite((value - xs[index - 1]) / finite(xs[index] - xs[index - 1]));
                    if (mode.equals("smooth")) fraction = fraction * fraction * (3 - 2 * fraction);
                    return finite((1 - fraction) * ys[index - 1] + fraction * ys[index]);
                }
                return ys[ys.length - 1];
            });
        }
        if (!Set.of("add", "multiply", "min", "max").contains(operation)) throw new IllegalArgumentException("Unknown numeric operation: " + operation);
        var values = entry.getValue().getAsJsonArray();
        if (values.size() < 2 || values.size() > 64) throw new IllegalArgumentException("Numeric operation needs 2..64 operands");
        List<DynamicValue> operands = new ArrayList<>(); Set<String> sources = new HashSet<>();
        for (var value : values) { var operand = parse(value, depth + 1, count); operands.add(operand); sources.addAll(operand.dependencies()); }
        return node(sources, context -> {
            double result = operands.getFirst().evaluate(context);
            for (int index = 1; index < operands.size(); index++) {
                double value = operands.get(index).evaluate(context);
                result = finite(switch (operation) {
                    case "add" -> result + value;
                    case "multiply" -> result * value;
                    case "min" -> Math.min(result, value);
                    default -> Math.max(result, value);
                });
            }
            return result;
        });
    }

    private static double number(JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Expected numeric point");
        return finite(value.getAsDouble());
    }
    static double finite(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Numeric result must be finite");
        return value;
    }
    private static DynamicValue node(Set<String> sources, java.util.function.ToDoubleFunction<Map<String, Double>> calculation) {
        Set<String> dependencies = Set.copyOf(sources);
        return new DynamicValue() {
            public double evaluate(Map<String, Double> context) { return finite(calculation.applyAsDouble(context)); }
            public Set<String> dependencies() { return dependencies; }
        };
    }
}
