package com.dragonminez.api.rules;

import com.dragonminez.common.stats.StatsData;
import com.google.gson.*;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** JSON object keys are ANDed. Extension validators run at config load, never after a purchase debit. */
public final class Conditions {
    public record Context(StatsData data, String form) {}
    public interface Type {
        void validate(JsonElement value);
        boolean test(Context context, JsonElement value);
        Component describe(JsonElement value);
        default Component describe(Context context, JsonElement value) { return describe(value); }
    }
    private static volatile boolean ready;
    public static boolean ready() { return ready; }
    public static void finishRegistration() { ready=true; }
    private static final Map<String, Type> TYPES = new ConcurrentHashMap<>();
    public static void register(String key, Type type) {
        if (TYPES.putIfAbsent(key, Objects.requireNonNull(type)) != null) throw new IllegalArgumentException("Duplicate condition: " + key);
    }
    private interface Test { boolean test(Context context, JsonElement value); }
    private static void add(String key, java.util.function.Consumer<JsonElement> validator, Test test) {
        register(key, new Type() {
            public void validate(JsonElement value) { validator.accept(value); }
            public boolean test(Context context, JsonElement value) { return test.test(context, value); }
            public Component describe(JsonElement value) { return describeBuiltin(null,key,value); }
            public Component describe(Context context, JsonElement value) { return describeBuiltin(context,key,value); }
        });
    }
    static {
        add("all", Conditions::validateList, (c,v) -> v.getAsJsonArray().asList().stream().allMatch(e -> test(c,e.getAsJsonObject())));
        add("any", Conditions::validateList, (c,v) -> v.getAsJsonArray().asList().stream().anyMatch(e -> test(c,e.getAsJsonObject())));
        add("not", v -> validate(v.getAsJsonObject()), (c,v) -> !test(c,v.getAsJsonObject()));
        add("content", v -> com.dragonminez.api.content.ContentProgress.requireId(v.getAsString()), (c,v) -> c.data().getContentProgress().hasCompleted(v.getAsString()));
        add("level", v -> positive(v.getAsDouble()), (c,v) -> c.data().getLevel() >= v.getAsDouble());
        add("skill", v -> { var o=v.getAsJsonObject(); id(o.get("id").getAsString()); positive(o.get("level").getAsDouble()); },
                (c,v) -> c.data().getSkills().getSkillLevel(v.getAsJsonObject().get("id").getAsString()) >= v.getAsJsonObject().get("level").getAsInt());
        add("mastery", v -> { var o=v.getAsJsonObject(); form(o.get("form").getAsString()); positive(o.get("min").getAsDouble()); },
                (c,v) -> mastery(c,v.getAsJsonObject().get("form").getAsString()) >= v.getAsJsonObject().get("min").getAsDouble());
        add("race", v -> id(v.getAsString()), (c,v) -> c.data().getCharacter().getRaceName().equals(v.getAsString()));
        add("used", v -> form(v.getAsString()), (c,v) -> c.data().getFormHistory().used(c.data().getCharacter().getRaceName(),qualify(c,v.getAsString())));
        bool("first", c -> c.form()!=null && (!c.data().getFormHistory().used(c.data().getCharacter().getRaceName(),c.form()) || c.data().getFormHistory().first(c.data().getCharacter().getRaceName(),c.form())));
        bool("flying", c -> c.data().getSkills().isSkillActive("fly"));
        bool("charging", c -> c.data().getStatus().isActionCharging());
        bool("chargingKi",c->c.data().getStatus().isChargingKi());
        bool("transforming",c->c.data().getStatus().isActionCharging() &&
                (c.data().getStatus().getSelectedAction()==com.dragonminez.common.stats.extras.ActionMode.FORM ||
                 c.data().getStatus().getSelectedAction()==com.dragonminez.common.stats.extras.ActionMode.STACK));
        bool("aura", c -> c.data().getStatus().isAuraActive() || c.data().getStatus().isPermanentAura() || c.data().getStatus().isForcedAura());
        bool("grounded", c -> c.data().getPlayer()!=null && c.data().getPlayer().onGround());
        bool("sneaking", c -> c.data().getPlayer()!=null && c.data().getPlayer().isShiftKeyDown());
        bool("water", c -> c.data().getPlayer()!=null && c.data().getPlayer().isInWater());
        bool("night", c -> c.data().getPlayer()!=null && c.data().getPlayer().level().isNight());
        range("healthPercent",0,100,c->c.data().getPlayer()==null?Double.NaN:percent(c.data().getPlayer().getHealth(),c.data().getPlayer().getMaxHealth()));
        range("energyPercent",0,100,c->percent(c.data().getResources().getCurrentEnergy(),c.data().getMaxEnergy()));
        range("staminaPercent",0,100,c->percent(c.data().getResources().getCurrentStamina(),c.data().getMaxStamina()));
        range("powerRelease",0,1000,c->c.data().getResources().getPowerRelease());
        range("altitude",-2048,2048,c->c.data().getPlayer()==null?Double.NaN:c.data().getPlayer().getY());
        range("timeOfDay",0,23999,c->c.data().getPlayer()==null?Double.NaN:Math.floorMod(c.data().getPlayer().level().getDayTime(),24000));
        add("dimension",v->resource(v.getAsString()),(c,v)->c.data().getPlayer()!=null&&c.data().getPlayer().level().dimension().location().toString().equals(v.getAsString()));
        add("biome",v->resource(v.getAsString()),(c,v)->c.data().getPlayer()!=null&&c.data().getPlayer().level().getBiome(c.data().getPlayer().blockPosition()).is(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,net.minecraft.resources.ResourceLocation.parse(v.getAsString()))));
        add("activeForm",v->form(v.getAsString()),(c,v)->qualify(c,v.getAsString()).equals(c.data().getCharacter().getActiveFormGroup()+"."+c.data().getCharacter().getActiveForm()));
        add("stackForm",v->form(v.getAsString()),(c,v)->qualify(c,v.getAsString()).equals(c.data().getCharacter().getActiveStackFormGroup()+"."+c.data().getCharacter().getActiveStackForm()));
        add("heldItem",v->resource(v.getAsString()),(c,v)->c.data().getPlayer()!=null&&net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(c.data().getPlayer().getMainHandItem().getItem()).toString().equals(v.getAsString()));
        add("effect",v->{var o=v.getAsJsonObject();resource(o.get("id").getAsString());if(o.has("minAmplifier")){double n=o.get("minAmplifier").getAsDouble();if(!Double.isFinite(n)||n<0||n>255||n!=Math.rint(n))throw new IllegalArgumentException("Effect amplifier must be an integer from 0 to 255");}},(c,v)->{
            var player=c.data().getPlayer();if(player==null)return false;var o=v.getAsJsonObject();
            var holder=net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(net.minecraft.resources.ResourceLocation.parse(o.get("id").getAsString()));
            if(holder.isEmpty())return false;var effect=player.getEffect(holder.get());return effect!=null&&effect.getAmplifier()>=(o.has("minAmplifier")?o.get("minAmplifier").getAsInt():0);
        });
        bool("raining",c->c.data().getPlayer()!=null&&c.data().getPlayer().level().isRainingAt(c.data().getPlayer().blockPosition()));
        bool("thundering",c->c.data().getPlayer()!=null&&c.data().getPlayer().level().isThundering());
        bool("sprinting",c->c.data().getPlayer()!=null&&c.data().getPlayer().isSprinting());
        bool("swimming",c->c.data().getPlayer()!=null&&c.data().getPlayer().isSwimming());
        bool("underwater",c->c.data().getPlayer()!=null&&c.data().getPlayer().isUnderWater());
        bool("burning",c->c.data().getPlayer()!=null&&c.data().getPlayer().isOnFire());
        bool("gliding",c->c.data().getPlayer()!=null&&c.data().getPlayer().isFallFlying());
        bool("moving",c->{var player=c.data().getPlayer();if(player==null)return false;var velocity=player.getDeltaMovement();return velocity.horizontalDistanceSqr()>0.0025||!player.onGround()&&velocity.y*velocity.y>0.0025;});

    }
    private static double percent(double current,double max){return max>0?Math.clamp(current*100.0/max,0,100):0;}
    private static void resource(String value){if(!value.contains(":")||net.minecraft.resources.ResourceLocation.tryParse(value)==null)throw new IllegalArgumentException("Expected a namespaced resource id: "+value);}
    private static void range(String key,double lower,double upper,java.util.function.ToDoubleFunction<Context> getter) {
        add(key,value->{
            var o=value.getAsJsonObject();if(o.size()==0||o.keySet().stream().anyMatch(k->!k.equals("min")&&!k.equals("max")))throw new IllegalArgumentException(key+" expects min and/or max");
            double min=o.has("min")?o.get("min").getAsDouble():lower,max=o.has("max")?o.get("max").getAsDouble():upper;
            if(!Double.isFinite(min)||!Double.isFinite(max)||min<lower||max>upper||min>max)throw new IllegalArgumentException(key+" range must be ordered within "+lower+".."+upper);
        },(context,value)->{
            var o=value.getAsJsonObject();double n=getter.applyAsDouble(context);
            return Double.isFinite(n)&&n>=(o.has("min")?o.get("min").getAsDouble():lower)&&n<=(o.has("max")?o.get("max").getAsDouble():upper);
        });
    }
    private static Component describeBuiltin(Context context,String key,JsonElement value) {
        if(key.equals("all")||key.equals("any")) {
            var text=Component.empty();
            for(var item:value.getAsJsonArray()){
                if(!text.getSiblings().isEmpty())text.append(Component.translatable("rules.dragonminez."+key+"_separator"));
                text.append(Component.translatable("rules.dragonminez.group",describe(context,item.getAsJsonObject())));
            }
            return text;
        }
        if(key.equals("not"))return Component.translatable("rules.dragonminez.not",describe(context,value.getAsJsonObject()));
        if(java.util.Set.of("healthPercent","energyPercent","staminaPercent","powerRelease","altitude","timeOfDay").contains(key)) {
            var o=value.getAsJsonObject();return Component.translatable("rules.dragonminez."+key,o.has("min")?o.get("min").getAsString():"−∞",o.has("max")?o.get("max").getAsString():"∞");
        }
        if(key.equals("activeForm")||key.equals("stackForm"))return Component.translatable("rules.dragonminez."+key,formName(context,value.getAsString()));
        if(key.equals("dimension")||key.equals("biome")){var id=net.minecraft.resources.ResourceLocation.parse(value.getAsString());return Component.translatable("rules.dragonminez."+key,Component.translatable(key+"."+id.getNamespace()+"."+id.getPath().replace('/','.')));}
        if(key.equals("heldItem"))return Component.translatable("rules.dragonminez.heldItem",net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(value.getAsString())).getDescription());
        if(key.equals("effect")){var o=value.getAsJsonObject();var id=net.minecraft.resources.ResourceLocation.parse(o.get("id").getAsString());return Component.translatable("rules.dragonminez.effect",Component.translatable("effect."+id.getNamespace()+"."+id.getPath()),o.has("minAmplifier")?o.get("minAmplifier").getAsInt()+1:1);}
        if(key.equals("mastery")){var o=value.getAsJsonObject();return Component.translatable("rules.dragonminez.mastery",formName(context,o.get("form").getAsString()),o.get("min").getAsDouble());}
        if(key.equals("skill")){var o=value.getAsJsonObject();return Component.translatable("rules.dragonminez.skill",Component.translatable("skill.dragonminez."+o.get("id").getAsString()),o.get("level").getAsInt());}
        if(key.equals("used")) return Component.translatable("rules.dragonminez.used",formName(context,value.getAsString()));
        if(key.equals("content")) return Component.translatable("rules.dragonminez.content",Component.translatable(value.getAsString()));
        if(key.equals("race")) return Component.translatable("rules.dragonminez.race",Component.translatable("race.dragonminez."+value.getAsString()));
        if(value.isJsonPrimitive()&&value.getAsJsonPrimitive().isBoolean())return Component.translatable("rules.dragonminez."+key,Component.translatable("rules.dragonminez.boolean."+value.getAsBoolean()));
        return Component.translatable("rules.dragonminez."+key,value.isJsonPrimitive()?value.getAsString():value.toString());
    }
    private static Component formName(Context context,String ref) {
        if (context != null && ref.matches("[a-z0-9_/-]+\\.[a-z0-9_-]+"))
            return com.dragonminez.api.forms.FormAccess.name(context.data(),qualify(context,ref));
        return Component.translatable(ref);
    }
    private static void bool(String key, java.util.function.Predicate<Context> test) {
        add(key, v -> { if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException(key + " must be boolean"); }, (c,v) -> test.test(c)==v.getAsBoolean());
    }
    public static void validate(JsonObject conditions) { validate(conditions,0); }
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private static void validate(JsonObject conditions, int unused) {
        if (conditions==null) return;
        int depth=DEPTH.get(); if (depth>=32) throw new IllegalArgumentException("Conditions exceed 32 nesting levels");
        DEPTH.set(depth+1);
        try { conditions.entrySet().forEach(e -> {var type=TYPES.get(e.getKey());if(type!=null)type.validate(e.getValue());else if(ready)throw new IllegalArgumentException("Unknown condition: "+e.getKey());}); }
        finally { DEPTH.set(depth); }
    }
    private static void validateList(JsonElement value) { for (JsonElement item : value.getAsJsonArray()) validate(item.getAsJsonObject()); }
    private static Type type(String key) { Type result=TYPES.get(key); if(result==null) throw new IllegalArgumentException("Unknown condition: " + key); return result; }
    public static boolean test(Context context, JsonObject conditions) {
        return conditions==null || conditions.entrySet().stream().allMatch(e -> type(e.getKey()).test(context,e.getValue()));
    }
    public static Component describe(Context context,JsonObject conditions){
        var result=Component.empty();if(conditions==null)return result;
        for(var entry:conditions.entrySet()){
            if(!result.getSiblings().isEmpty())result.append(Component.translatable("rules.dragonminez.all_separator"));
            result.append(type(entry.getKey()).describe(context,entry.getValue()));
        }
        return result;
    }
    public static List<Component> missing(Context context, JsonObject conditions) {
        if (conditions==null) return List.of();
        List<Component> result=new ArrayList<>();
        for(var entry:conditions.entrySet()){
            if(type(entry.getKey()).test(context,entry.getValue()))continue;
            if(entry.getKey().equals("all"))for(var item:entry.getValue().getAsJsonArray())result.addAll(missing(context,item.getAsJsonObject()));
            else result.add(type(entry.getKey()).describe(context,entry.getValue()));
        }
        return List.copyOf(result);
    }
    private static String qualify(Context context,String reference){
        if(context==null||context.form()==null)return reference;
        String[] parts=form(reference);int dot=context.form().indexOf('.');
        if(dot<=0)return reference;
        String owner=context.form().substring(0,dot);
        return com.dragonminez.common.util.FusionForms.qualifyLinkedGroup(owner,parts[0])+"."+parts[1];
    }
    private static double mastery(Context context,String reference){
        String qualified=qualify(context,reference);int dot=qualified.indexOf('.');
        String group=qualified.substring(0,dot),form=qualified.substring(dot+1);
        return Math.max(context.data().getCharacter().getFormMasteries().getMastery(group,form),
                context.data().getCharacter().getStackFormMasteries().getMastery(group,form));
    }
    public static double mastery(StatsData data,String ref) {
        String[] parts=form(ref);
        return Math.max(data.getCharacter().getFormMasteries().getMastery(parts[0],parts[1]),data.getCharacter().getStackFormMasteries().getMastery(parts[0],parts[1]));
    }
    public static String[] form(String ref) {
        if(ref==null || !ref.matches("[a-z0-9_/-]+\\.[a-z0-9_-]+")) throw new IllegalArgumentException("Expected group.form, got " + ref);
        return ref.split("\\.",2);
    }
    public static void id(String id) { if (id==null || !id.matches("[a-z0-9_:/.-]{1,128}")) throw new IllegalArgumentException("Invalid id: " + id); }
    public static void positive(double value) { if (!Double.isFinite(value) || value<0) throw new IllegalArgumentException("Expected a finite nonnegative value"); }
    private Conditions() {}
}
