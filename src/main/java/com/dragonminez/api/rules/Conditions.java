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
                (c,v) -> mastery(c.data(),v.getAsJsonObject().get("form").getAsString()) >= v.getAsJsonObject().get("min").getAsDouble());
        add("race", v -> id(v.getAsString()), (c,v) -> c.data().getCharacter().getRaceName().equals(v.getAsString()));
        add("used", v -> form(v.getAsString()), (c,v) -> c.data().getFormHistory().used(c.data().getCharacter().getRaceName(),v.getAsString()));
        bool("first", c -> c.form()!=null && (!c.data().getFormHistory().used(c.data().getCharacter().getRaceName(),c.form()) || c.data().getFormHistory().first(c.data().getCharacter().getRaceName(),c.form())));
        bool("flying", c -> c.data().getSkills().isSkillActive("fly"));
        bool("charging", c -> c.data().getStatus().isActionCharging());
        bool("aura", c -> c.data().getStatus().isAuraActive() || c.data().getStatus().isPermanentAura() || c.data().getStatus().isForcedAura());
        bool("grounded", c -> c.data().getPlayer()!=null && c.data().getPlayer().onGround());
        bool("sneaking", c -> c.data().getPlayer()!=null && c.data().getPlayer().isShiftKeyDown());
        bool("water", c -> c.data().getPlayer()!=null && c.data().getPlayer().isInWater());
        bool("night", c -> c.data().getPlayer()!=null && c.data().getPlayer().level().isNight());
    }
    private static Component describeBuiltin(Context context,String key,JsonElement value) {
        if(key.equals("all")||key.equals("any")) {
            var text=Component.empty();for(var item:value.getAsJsonArray())for(var condition:item.getAsJsonObject().entrySet()){
                if(!text.getString().isEmpty())text.append(key.equals("all")?" AND ":" OR ");text.append(type(condition.getKey()).describe(context,condition.getValue()));
            }return text;
        }
        if(key.equals("not")){var text=Component.empty();value.getAsJsonObject().entrySet().forEach(e->text.append(type(e.getKey()).describe(context,e.getValue())));return Component.translatable("rules.dragonminez.not",text);}
        if(key.equals("mastery")){var o=value.getAsJsonObject();return Component.translatable("rules.dragonminez.mastery",formName(context,o.get("form").getAsString()),o.get("min").getAsDouble());}
        if(key.equals("skill")){var o=value.getAsJsonObject();return Component.translatable("rules.dragonminez.skill",Component.translatable("skill.dragonminez."+o.get("id").getAsString()),o.get("level").getAsInt());}
        if(key.equals("used")) return Component.translatable("rules.dragonminez.used",formName(context,value.getAsString()));
        if(key.equals("content")) return Component.translatable("rules.dragonminez.content",Component.translatable(value.getAsString()));
        if(key.equals("race")) return Component.translatable("rules.dragonminez.race",Component.translatable("race.dragonminez."+value.getAsString()));
        return Component.translatable("rules.dragonminez."+key,value.isJsonPrimitive()?value.getAsString():value.toString());
    }
    private static Component formName(Context context,String ref) {
        if (context != null && ref.matches("[a-z0-9_/-]+\\.[a-z0-9_-]+"))
            return com.dragonminez.api.forms.FormHair.name(context.data().getCharacter().getRaceName(),ref);
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
    public static List<Component> missing(Context context, JsonObject conditions) {
        if (conditions==null) return List.of();
        return conditions.entrySet().stream().filter(e -> !type(e.getKey()).test(context,e.getValue())).map(e -> type(e.getKey()).describe(context,e.getValue())).toList();
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
