package com.dragonminez.common.config;

import com.dragonminez.api.rules.Conditions;
import com.dragonminez.api.runtime.EventRules;
import com.google.gson.*;
import java.util.*;

/** Server-declared views of synchronized meters; rendering never advances them. */
public final class MeterHudConfig {
    public record Bar(String meter, String label, String anchor, int x, int y, int width, int height,
                      int color, int background, boolean showValue, JsonObject when, double minimum, double maximum) {
        public double fraction(double value) {
            return maximum <= minimum || !Double.isFinite(value) ? 0 : Math.clamp((value-minimum)/(maximum-minimum),0,1);
        }
        public int[] bounds(int screenWidth, int screenHeight) {
            int w=Math.min(width,Math.max(1,screenWidth)), h=Math.min(height,Math.max(1,screenHeight));
            int left=anchor.endsWith("right")?screenWidth-w-x:anchor.equals("center")?(screenWidth-w)/2+x:x;
            int top=anchor.startsWith("bottom")?screenHeight-h-y:anchor.equals("center")?(screenHeight-h)/2+y:y;
            return new int[]{Math.clamp(left,0,Math.max(0,screenWidth-w)),Math.clamp(top,0,Math.max(0,screenHeight-h)),w,h};
        }
    }
    private MeterHudConfig() {}
    public static List<Bar> parse(JsonElement value, EventRules rules) {
        if(value==null)return List.of();
        var entries=value.getAsJsonArray();
        if(entries.size()>16)throw new IllegalArgumentException("At most 16 meter HUD bars");
        var result=new ArrayList<Bar>();
        for(var entry:entries){
            var json=entry.getAsJsonObject();
            for(String key:json.keySet())if(!Set.of("meter","label","anchor","x","y","width","height","color","background","showValue","when").contains(key))throw new IllegalArgumentException("Unknown meter HUD field: "+key);
            String meter=json.get("meter").getAsString(), label=text(json,"label","");
            if(!rules.meters().contains(meter)||label.length()>256)throw new IllegalArgumentException("Invalid meter HUD reference/label");
            String anchor=text(json,"anchor","top_left");
            if(!Set.of("top_left","top_right","bottom_left","bottom_right","center").contains(anchor))throw new IllegalArgumentException("Invalid meter HUD anchor");
            if(json.has("showValue") && (!json.get("showValue").isJsonPrimitive() || !json.getAsJsonPrimitive("showValue").isBoolean()))throw new IllegalArgumentException("Meter HUD showValue must be boolean");
            var when=json.has("when")?json.getAsJsonObject("when").deepCopy():new JsonObject();Conditions.validate(when);
            var definition=rules.definition(meter);
            result.add(new Bar(meter,label,anchor,number(json,"x",8,-4096,4096),number(json,"y",8,-4096,4096),
                    number(json,"width",120,8,512),number(json,"height",8,2,64),color(json,"color","#66CCFF"),color(json,"background","#202020"),
                    !json.has("showValue")||json.get("showValue").getAsBoolean(),when,definition.minimum(),definition.maximum()));
        }
        return List.copyOf(result);
    }
    private static String text(JsonObject json,String key,String fallback){return json.has(key)?json.get(key).getAsString():fallback;}
    private static int color(JsonObject json,String key,String fallback){
        String value=text(json,key,fallback);
        if(!value.matches("#[0-9a-fA-F]{6}"))throw new IllegalArgumentException("Meter HUD colors need #RRGGBB");
        return 0xFF000000|Integer.parseInt(value.substring(1),16);
    }
    private static int number(JsonObject json,String key,int fallback,int min,int max){
        if(!json.has(key))return fallback;
        var value=json.get(key);
        if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Meter HUD number required: "+key);
        double number=value.getAsDouble();
        if(!Double.isFinite(number)||number!=Math.rint(number)||number<min||number>max)throw new IllegalArgumentException("Meter HUD number outside bounds: "+key);
        return (int)number;
    }
}
