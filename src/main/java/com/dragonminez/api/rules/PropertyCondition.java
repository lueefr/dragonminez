package com.dragonminez.api.rules;

import com.dragonminez.common.config.ConfigManager;
import com.google.gson.*;
import java.util.Set;

public final class PropertyCondition {
    private PropertyCondition() {}
    public static void validate(JsonElement value) {
        var object=value.getAsJsonObject();com.dragonminez.api.content.ContentProgress.requireId(object.get("id").getAsString());
        if(object.has("equals")&&object.get("equals").isJsonObject()||object.has("contains")&&!object.get("contains").isJsonPrimitive())throw new IllegalArgumentException("Property comparison must use scalar values or arrays");
        if(object.size()<2)throw new IllegalArgumentException("Property condition needs a comparison");
        for(var entry:object.entrySet()) {
            if(!Set.of("id","equals","contains","present","min","max").contains(entry.getKey()))throw new IllegalArgumentException("Unknown property comparison: "+entry.getKey());
            if(entry.getKey().equals("present")&&(!entry.getValue().isJsonPrimitive()||!entry.getValue().getAsJsonPrimitive().isBoolean()))throw new IllegalArgumentException("Property presence must be boolean");
            if(Set.of("min","max").contains(entry.getKey())&&!Double.isFinite(entry.getValue().getAsDouble()))throw new IllegalArgumentException("Property bound must be finite");
        }
        if(object.has("min")&&object.has("max")&&object.get("min").getAsDouble()>object.get("max").getAsDouble())throw new IllegalArgumentException("Inverted property bounds");
    }
    public static net.minecraft.network.chat.Component describe(JsonElement condition) {
        var object=condition.getAsJsonObject();var name=net.minecraft.network.chat.Component.translatable(object.get("id").getAsString());
        var result=net.minecraft.network.chat.Component.empty();
        for(String comparison:java.util.List.of("present","equals","contains","min","max"))if(object.has(comparison)) {
            if(!result.getSiblings().isEmpty())result.append(net.minecraft.network.chat.Component.translatable("gui.dragonminez.text.separator"));
            var value=object.get(comparison);
            if(comparison.equals("present"))result.append(net.minecraft.network.chat.Component.translatable("rules.dragonminez.property."+(value.getAsBoolean()?"present":"absent"),name));
            else result.append(net.minecraft.network.chat.Component.translatable("rules.dragonminez.property."+comparison,name,valueText(value)));
        }
        return result;
    }
    private static net.minecraft.network.chat.Component valueText(JsonElement value) {
        if(value.isJsonNull())return net.minecraft.network.chat.Component.translatable("rules.dragonminez.property.unset");
        if(value.isJsonArray()) {
            var result=net.minecraft.network.chat.Component.empty();
            for(var entry:value.getAsJsonArray()){if(!result.getSiblings().isEmpty())result.append(net.minecraft.network.chat.Component.translatable("gui.dragonminez.text.separator"));result.append(valueText(entry));}
            return result;
        }
        if(value.isJsonObject())throw new IllegalArgumentException("Property comparison must use scalar values or arrays");
        if(value.getAsJsonPrimitive().isBoolean())return net.minecraft.network.chat.Component.translatable("rules.dragonminez.boolean."+value.getAsBoolean());
        return value.getAsJsonPrimitive().isNumber()?net.minecraft.network.chat.Component.translatable("gui.dragonminez.text.value",value.getAsString()):net.minecraft.network.chat.Component.translatable(value.getAsString());
    }
    public static boolean test(Conditions.Context context,JsonElement condition) {
        var object=condition.getAsJsonObject();var value=ConfigManager.getAttributes().property(context.data(),object.get("id").getAsString());
        boolean present=!value.isJsonNull();
        if(object.has("present")&&object.get("present").getAsBoolean()!=present)return false;
        if(object.has("equals")&&!object.get("equals").equals(value))return false;
        if(object.has("contains")&&(!value.isJsonArray()||!value.getAsJsonArray().contains(object.get("contains"))))return false;
        if(object.has("min")||object.has("max")) {
            if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())return false;
            double number=value.getAsDouble();
            if(object.has("min")&&number<object.get("min").getAsDouble()||object.has("max")&&number>object.get("max").getAsDouble())return false;
        }
        return true;
    }
}
