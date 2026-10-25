package com.dragonminez.api.forms;

import com.dragonminez.api.rules.Conditions;
import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.StatsData;
import com.google.gson.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Optional visual overrides. Rules run in declaration order; stack overrides are applied last. */
public final class FormVisuals {
    private static final Map<String,Set<String>> PARTS=new ConcurrentHashMap<>();
    static {
        for(String part:List.of("head","body","left_arm","right_arm","left_leg","right_leg"))registerPart(part,Set.of(part));
        registerPart("tail",Set.of("tail1","tailenrolled"));
        registerPart("left_forearm",Set.of("left_forearm"));registerPart("right_forearm",Set.of("right_forearm"));
    }
    public static void registerPart(String id,Set<String> bones){if(PARTS.putIfAbsent(id,Set.copyOf(bones))!=null)throw new IllegalArgumentException("Duplicate part: "+id);}
    public static Map<String,Set<String>> parts(){return Map.copyOf(PARTS);}
    private static void color(JsonElement v){if(!v.isJsonPrimitive()||!v.getAsString().matches("#[0-9a-fA-F]{6}"))throw new IllegalArgumentException("Expected #RRGGBB, got "+v);}
    private static void bool(JsonElement v){if(!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isBoolean())throw new IllegalArgumentException("Expected boolean, got "+v);}
    public static void validate(JsonObject visual){validate(visual,true);}
    private static void validate(JsonObject visual,boolean rules){
        if(visual==null)return;
        for(var e:visual.entrySet())switch(e.getKey()){
            case "aura" -> {var v=e.getValue();if(v.isJsonPrimitive()&&v.getAsJsonPrimitive().isString()){
                if(!Set.of("sealed","visible").contains(v.getAsString()))throw new IllegalArgumentException("Aura must be boolean, sealed or visible");
            }else bool(v);}
            case "lightning","outline","hair" -> bool(e.getValue());
            case "tail","eyes" -> {if(e.getValue().isJsonPrimitive()&&e.getValue().getAsJsonPrimitive().isBoolean())bool(e.getValue());else color(e.getValue());}
            case "fur" -> {
                if(e.getValue().isJsonPrimitive()){bool(e.getValue());break;}
                for(var f:e.getValue().getAsJsonObject().entrySet())switch(f.getKey()){
                    case "color" -> color(f.getValue());
                    case "texture" -> {String id=f.getValue().getAsString();if(id.startsWith("file:"))com.dragonminez.api.npc.CustomNpcVisuals.texture(id.substring(5));else if(net.minecraft.resources.ResourceLocation.tryParse(id)==null)throw new IllegalArgumentException("Invalid fur texture: "+id);}
                    default -> throw new IllegalArgumentException("Unknown fur option: "+f.getKey());
                }
            }
            case "parts" -> e.getValue().getAsJsonObject().entrySet().forEach(p->{
                if(!PARTS.containsKey(p.getKey()) && Conditions.ready())throw new IllegalArgumentException("Unknown body part: "+p.getKey());
                var value=p.getValue();if(value.isJsonPrimitive()){bool(value);return;}
                var scale=value.getAsJsonArray();if(scale.size()!=3)throw new IllegalArgumentException("Part scale needs [x,y,z]");
                for(var axis:scale){double n=axis.getAsDouble();if(!Double.isFinite(n)||n<0.05||n>8)throw new IllegalArgumentException("Part scale must be 0.05..8");}
            });
            case "rules" -> {
                if(!rules)throw new IllegalArgumentException("Visual rules cannot be nested in set");
                var list=e.getValue().getAsJsonArray();if(list.size()>128)throw new IllegalArgumentException("At most 128 visual rules per form");
                for(var item:list){var rule=item.getAsJsonObject();if(!rule.keySet().equals(Set.of("if","set")))throw new IllegalArgumentException("Visual rule needs if and set");Conditions.validate(rule.getAsJsonObject("if"));validate(rule.getAsJsonObject("set"),false);}
            }
            default -> throw new IllegalArgumentException("Unknown visual option: "+e.getKey());
        }
    }
    private record Cached(int tick,long config,long history,FormConfig.FormData base,FormConfig.FormData stack,JsonObject value) {}
    private static final Map<StatsData,Cached> CACHE=Collections.synchronizedMap(new WeakHashMap<>());
    public static JsonObject resolve(StatsData data){
        if(data==null)return new JsonObject();
        var c=data.getCharacter();int tick=data.getPlayer()==null?-1:data.getPlayer().tickCount;
        long config=ConfigManager.getFormConfigGeneration(),history=data.getFormHistory().revision();var cached=CACHE.get(data);
        var base=c.getActiveFormData();var stack=c.getActiveStackFormData();
        if(tick>=0&&cached!=null&&cached.tick()==tick&&cached.config()==config&&cached.history()==history&&cached.base()==base&&cached.stack()==stack)return cached.value();
        var value=compute(data);if(tick>=0)CACHE.put(data,new Cached(tick,config,history,base,stack,value));return value;
    }
    private static JsonObject compute(StatsData data){
        JsonObject result=new JsonObject();if(data==null)return result;var c=data.getCharacter();
        apply(result,data,c.getActiveFormGroup()+"."+c.getActiveForm(),c.getActiveFormData());
        apply(result,data,c.getActiveStackFormGroup()+"."+c.getActiveStackForm(),c.getActiveStackFormData());return result;
    }
    private static void apply(JsonObject result,StatsData data,String id,FormConfig.FormData form){
        if(form==null||form.getVisual()==null)return;var visual=form.getVisual();merge(result,visual);
        if(visual.has("rules"))for(var item:visual.getAsJsonArray("rules")){
            var rule=item.getAsJsonObject();if(Conditions.test(new Conditions.Context(data,id),rule.getAsJsonObject("if")))merge(result,rule.getAsJsonObject("set"));
        }
    }
    private static void merge(JsonObject target,JsonObject values){values.entrySet().forEach(e->{
        if(e.getKey().equals("rules"))return;
        if(e.getValue().isJsonObject()&&target.has(e.getKey())&&target.get(e.getKey()).isJsonObject())merge(target.getAsJsonObject(e.getKey()),e.getValue().getAsJsonObject());
        else target.add(e.getKey(),e.getValue().deepCopy());
    });}
    public static boolean flag(StatsData data,String name,boolean fallback){var value=resolve(data).get(name);if(name.equals("aura")&&value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString())return value.getAsString().equals("visible");return value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isBoolean()?value.getAsBoolean():fallback;}
    public static boolean sealed(StatsData data){var value=resolve(data).get("aura");return value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString()&&value.getAsString().equals("sealed");}
    public static float[] color(StatsData data,String name,float[] fallback){
        var value=resolve(data).get(name);if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString())return fallback;
        return rgb(value.getAsString());
    }
    public static float[] rgb(String color){int value=Integer.parseInt(color.substring(1),16);return new float[]{(value>>16&255)/255f,(value>>8&255)/255f,(value&255)/255f};}
    public static JsonElement part(StatsData data,String bone){
        var visual=resolve(data);var parts=visual.getAsJsonObject("parts");if(parts==null)return null;
        for(var entry:PARTS.entrySet())if(entry.getValue().contains(bone)&&parts.has(entry.getKey()))return parts.get(entry.getKey());return null;
    }
    private FormVisuals(){}
}
