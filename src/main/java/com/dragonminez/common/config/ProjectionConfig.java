package com.dragonminez.common.config;

import com.dragonminez.api.content.ContentProgress;
import com.dragonminez.api.rules.*;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Optional server-owned energy avatars use ordinary conditions and registered status effects. */
public final class ProjectionConfig {
    public record Effect(String id,int ticks,int amplifier) {}
    public record Attack(String stat,String targeting,double angle,double factor,double radius,double range,int interval,int targets,List<Effect> effects) {}
    public record Appearance(String model,String texture,String animation,String clip,float scale,int color,float opacity,double x,double y,double z,boolean bright) {}
    public record Definition(String id,JsonObject when,int duration,int cooldown,double energy,double stamina,double upkeep,Appearance appearance,Attack attack) {}
    private final Map<String,Definition> definitions=new LinkedHashMap<>();
    public Definition get(String id){return definitions.get(id);}
    public Collection<Definition> all(){return Collections.unmodifiableCollection(definitions.values());}
    public static ProjectionConfig parse(JsonElement json) {
        var result=new ProjectionConfig();if(json==null)return result;
        if(json.getAsJsonObject().size()>64)throw new IllegalArgumentException("At most 64 energy projections");
        json.getAsJsonObject().entrySet().forEach(entry->{
            String id=entry.getKey();ContentProgress.requireId(id);var value=entry.getValue().getAsJsonObject();
            fields(value,"when","durationTicks","cooldownTicks","costs","appearance","attack");
            var when=value.has("when")?value.getAsJsonObject("when").deepCopy():new JsonObject();Conditions.validate(when);
            var costs=value.has("costs")?value.getAsJsonObject("costs"):new JsonObject();fields(costs,"energy","stamina","energyPerTick");
            var visual=value.has("appearance")?value.getAsJsonObject("appearance"):new JsonObject();
            fields(visual,"model","texture","animation","clip","scale","color","opacity","offset","fullbright");
            String color=text(visual,"color","#FFFFFF");if(!color.matches("#[a-fA-F0-9]{6}"))throw new IllegalArgumentException("Projection color must be #RRGGBB");
            double[] offset={0,0,0};
            if(visual.has("offset")){var axes=visual.getAsJsonArray("offset");if(axes.size()!=3)throw new IllegalArgumentException("Projection offset needs three axes");for(int i=0;i<3;i++){var axis=new JsonObject();axis.add("value",axes.get(i));offset[i]=number(axis,"value",0,-32,32);}}
            String clip=text(visual,"clip","base.idle");if(clip.isBlank()||clip.length()>128)throw new IllegalArgumentException("Invalid projection animation clip");
            var appearance=new Appearance(resource(visual,"model","dragonminez:geo/entity/races/human.geo.json"),resource(visual,"texture","minecraft:textures/block/white_wool.png"),resource(visual,"animation","dragonminez:animations/entity/races/movement.animation.json"),clip,(float)number(visual,"scale",1,.1,32),Integer.parseInt(color.substring(1),16),(float)number(visual,"opacity",.5,0,1),offset[0],offset[1],offset[2],bool(visual,"fullbright",true));
            Attack attack=null;
            if(value.has("attack")) {
                var settings=value.getAsJsonObject("attack");fields(settings,"stat","targeting","angle","factor","radius","range","intervalTicks","targets","effects");
                String stat=text(settings,"stat","PWR");if(!Set.of("STR","PWR").contains(stat))throw new IllegalArgumentException("Projection attack stat must be STR or PWR");
                String targeting=text(settings,"targeting","hostile");if(!Set.of("hostile","nonfriendly","aim").contains(targeting))throw new IllegalArgumentException("Unknown projection targeting policy");
                var effects=new ArrayList<Effect>();
                if(settings.has("effects")) {
                    var list=settings.getAsJsonArray("effects");if(list.size()>16)throw new IllegalArgumentException("At most 16 projection effects");
                    for(var element:list){var effect=element.getAsJsonObject();fields(effect,"id","ticks","amplifier");var reward=new JsonObject();reward.add("effect",effect);Rewards.validate(reward);effects.add(new Effect(effect.get("id").getAsString(),effect.get("ticks").getAsInt(),effect.has("amplifier")?effect.get("amplifier").getAsInt():0));}
                }
                attack=new Attack(stat,targeting,number(settings,"angle",45,1,180),number(settings,"factor",.1,0,100),number(settings,"radius",3,.1,32),number(settings,"range",16,.1,64),integer(settings,"intervalTicks",20,1,72000),integer(settings,"targets",1,1,16),List.copyOf(effects));
            }
            result.definitions.put(id,new Definition(id,when,integer(value,"durationTicks",200,1,72000),integer(value,"cooldownTicks",200,0,72000),number(costs,"energy",0,0,1),number(costs,"stamina",0,0,1),number(costs,"energyPerTick",0,0,1),appearance,attack));
        });
        return result;
    }
    private static void fields(JsonObject value,String... allowed){if(!Set.of(allowed).containsAll(value.keySet()))throw new IllegalArgumentException("Unknown projection field");}
    private static String text(JsonObject value,String key,String fallback){if(!value.has(key))return fallback;var item=value.get(key);if(!item.isJsonPrimitive()||!item.getAsJsonPrimitive().isString())throw new IllegalArgumentException("Projection field must be a string: "+key);return item.getAsString();}
    private static String resource(JsonObject value,String key,String fallback){String id=text(value,key,fallback);ContentProgress.requireId(id);ResourceLocation.parse(id);return id;}
    private static boolean bool(JsonObject value,String key,boolean fallback){if(!value.has(key))return fallback;var item=value.get(key);if(!item.isJsonPrimitive()||!item.getAsJsonPrimitive().isBoolean())throw new IllegalArgumentException("Projection field must be boolean: "+key);return item.getAsBoolean();}
    private static double number(JsonObject value,String key,double fallback,double minimum,double maximum){if(!value.has(key))return fallback;var item=value.get(key);if(!item.isJsonPrimitive()||!item.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Projection field must be numeric: "+key);double number=item.getAsDouble();if(!Double.isFinite(number)||number<minimum||number>maximum)throw new IllegalArgumentException("Projection field out of bounds: "+key);return number;}
    private static int integer(JsonObject value,String key,int fallback,int minimum,int maximum){double number=number(value,key,fallback,minimum,maximum);if(number!=Math.rint(number))throw new IllegalArgumentException("Projection ticks/count must be whole: "+key);return (int)number;}
}
