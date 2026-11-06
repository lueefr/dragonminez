package com.dragonminez.common.config;

import com.dragonminez.api.rules.Conditions;
import com.dragonminez.api.runtime.EventRules;
import com.dragonminez.common.stats.StatsData;
import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.*;

/** Compiled server rules; content IDs and effect names remain entirely data driven. */
public final class RuntimeConfig {
    private final JsonObject source;
    private final EventRules rules;
    private final Map<String, JsonObject> conditions;
    private final int combatTicks;
    private record Script(String id, Set<String> events) {}
    private final List<Script> scripts = new ArrayList<>();
    private final Map<String, com.dragonminez.api.script.LuaProgram> programs = new LinkedHashMap<>();
    public record Commands(List<com.dragonminez.api.runtime.EventRules.Change> meters,List<JsonObject> rewards) {}
    private record Gameplay(String event, JsonObject when, List<JsonObject> actions) {}
    private final List<Gameplay> gameplay=new ArrayList<>();
    public Commands scriptBatch(String event, Map<String,Double> values) {
        var meters=new ArrayList<com.dragonminez.api.runtime.EventRules.Change>();var rewards=new ArrayList<JsonObject>();
        for(var script:scripts)if(script.events().contains(event)&&programs.containsKey(script.id())){
            var batch=programs.get(script.id()).evaluateBatch(event,values);
            for(var change:batch.meters())meters.add(new com.dragonminez.api.runtime.EventRules.Change(change.meter(),change.additive(),change.value()));
            for(var reward:batch.rewards()){com.dragonminez.api.rules.Rewards.validate(reward);rewards.add(reward);}
            if(meters.size()+rewards.size()>64)throw new IllegalArgumentException("At most 64 combined script commands");
        }
        return new Commands(List.copyOf(meters),List.copyOf(rewards));
    }
    public int scriptCount() { return scripts.size(); }
    public List<JsonObject> content() {
        var result = new ArrayList<JsonObject>();
        programs.values().forEach(program -> result.addAll(program.content()));
        if (result.size() > 128) throw new IllegalArgumentException("At most 128 script content definitions");
        return List.copyOf(result);
    }
    public List<com.dragonminez.api.runtime.EventRules.Change> scriptActions(String event,Map<String,Double> values){return scriptBatch(event,values).meters();}
    public List<JsonObject> actions(String event,StatsData data){
        var result=new ArrayList<JsonObject>();
        for(var rule:gameplay)if(rule.event().equals(event)&&Conditions.test(new Conditions.Context(data,null),rule.when()))result.addAll(rule.actions());
        if(result.size()>64)throw new IllegalArgumentException("At most 64 gameplay actions per event");
        return result;
    }
    private List<MeterHudConfig.Bar> hud = List.of();
    public List<MeterHudConfig.Bar> hud() { return hud; }
    private FlightConfig flight;
    public FlightConfig flight() { return flight; }
    private ReactionConfig reactions;
    public ReactionConfig reactions() { return reactions; }
    private BehaviorConfig behaviors;
    public BehaviorConfig behaviors() { return behaviors; }
    private RuntimeConfig(JsonObject source, EventRules rules, Map<String, JsonObject> conditions, int combatTicks) {
        this.source = source.deepCopy(); this.rules = rules; this.conditions = Map.copyOf(conditions); this.combatTicks = combatTicks;
    }
    public EventRules rules() { return rules; }
    public int combatTicks() { return combatTicks; }
    public boolean active() { return !rules.empty() || !scripts.isEmpty() || !gameplay.isEmpty(); }
    public void validate() { parse(json()); }
    public String json() { return new GsonBuilder().setPrettyPrinting().create().toJson(source); }
    public Map<String, Boolean> flags(StatsData data) {
        Map<String, Boolean> flags = new LinkedHashMap<>();
        conditions.forEach((key, condition) -> flags.put(key, Conditions.test(new Conditions.Context(data, null), condition)));
        return flags;
    }
    public static RuntimeConfig empty() { return parse("{}"); }
    public static RuntimeConfig parse(String json) {
        var source = JsonParser.parseString(json).getAsJsonObject();
        var engine = source.deepCopy();
        var gameplay = engine.remove("gameplay");
        var hud = engine.remove("hud");
        var flight = engine.remove("flight");
        var behavior = engine.remove("behaviors");
        var reactions = engine.remove("reactions");
        var scripts = engine.remove("scripts");
        int combatTicks = 200;
        if (engine.has("combatTicks")) {
            var value = engine.remove("combatTicks");
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Combat timeout must be numeric");
            double number = value.getAsDouble();
            if (!Double.isFinite(number) || number < 1 || number > 72000 || number != Math.rint(number)) throw new IllegalArgumentException("Combat timeout must be 1..72000 ticks");
            combatTicks = (int) number;
        }
        Map<String, JsonObject> conditions = new LinkedHashMap<>();
        var rules = EventRules.compile(engine, condition -> {
            Conditions.validate(condition);
            String key = Integer.toString(conditions.size()); conditions.put(key, condition.deepCopy());
            return context -> Boolean.TRUE.equals(context.flags().get(key));
        });
        rules.sources().forEach((event, sources) -> {
            checkEvent(event);
            for (String value : sources) if (!value.startsWith("meter:") && !Set.of("attack.charging", "attack.active", "attack.recovery", "attack.ticks", "attack.charge").contains(value) && (!Set.of("event.damage", "event.originalDamage", "event.blockedDamage").contains(value) || !Set.of("dmz:damage_received", "dmz:damage_dealt", "dmz:blocked", "dmz:countered").contains(event))) throw new IllegalArgumentException("Numeric source unavailable in event " + event + ": " + value);
        });
        conditions.values().forEach(condition -> checkMeters(condition, rules.meters()));
        var result = new RuntimeConfig(source, rules, conditions, combatTicks);
        if(gameplay!=null){
            if(gameplay.getAsJsonArray().size()>128)throw new IllegalArgumentException("At most 128 gameplay event rules");
            for(var entry:gameplay.getAsJsonArray()){
                var rule=entry.getAsJsonObject();if(!Set.of("event","when","actions").containsAll(rule.keySet()))throw new IllegalArgumentException("Unknown gameplay rule field");
                String event=rule.get("event").getAsString();com.dragonminez.api.content.ContentProgress.requireId(event);checkEvent(event);
                var when=rule.has("when")?rule.getAsJsonObject("when").deepCopy():new JsonObject();Conditions.validate(when);checkMeters(when,rules.meters());
                var actions=new ArrayList<JsonObject>();
                for(var value:rule.getAsJsonArray("actions")){var action=value.getAsJsonObject().deepCopy();com.dragonminez.api.rules.Rewards.validate(action);actions.add(action);}
                if(actions.size()>64)throw new IllegalArgumentException("At most 64 gameplay rule actions");
                result.gameplay.add(new Gameplay(event,when,List.copyOf(actions)));
            }
        }
        result.hud = MeterHudConfig.parse(hud, rules);
        for(var bar : result.hud) checkMeters(bar.when(), rules.meters());
        result.flight = FlightConfig.parse(flight);
        checkMeters(result.flight.when(), rules.meters());
        for (var profile : result.flight.profiles()) checkMeters(profile.when(), rules.meters());
        result.behaviors = BehaviorConfig.parse(behavior, rules.meters());
        result.reactions = ReactionConfig.parse(reactions, rules.meters());
        if (scripts != null) for (var entry : scripts.getAsJsonArray()) {
            var definition = entry.getAsJsonObject();
            if (!Set.of("id", "events").containsAll(definition.keySet()) || !definition.has("id")) throw new IllegalArgumentException("Script needs id and optional events");
            String id = definition.get("id").getAsString(); com.dragonminez.api.content.ContentProgress.requireId(id);
            if (result.scripts.stream().anyMatch(script -> script.id().equals(id))) throw new IllegalArgumentException("Duplicate script ID");
            Set<String> events = new LinkedHashSet<>();
            if (definition.has("events")) for (var event : definition.getAsJsonArray("events")) { String name = event.getAsString(); com.dragonminez.api.content.ContentProgress.requireId(name); checkEvent(name); events.add(name); }
            if (events.size() > 32) throw new IllegalArgumentException("At most 32 event subscriptions");
            result.scripts.add(new Script(id, Set.copyOf(events)));
        }
        if (result.scripts.size() > 32) throw new IllegalArgumentException("At most 32 runtime scripts");
        return result;
    }
    private static void checkEvent(String event) {
        if (event.startsWith("dmz:") && !com.dragonminez.api.script.LuaBindings.EVENTS.contains(event)) throw new IllegalArgumentException("Unknown native runtime event: " + event);
    }
    private static void checkMeters(JsonObject condition, Set<String> meters) {
        for (var entry : condition.entrySet()) {
            if (entry.getKey().equals("meter") && !meters.contains(entry.getValue().getAsJsonObject().get("id").getAsString())) throw new IllegalArgumentException("Unknown meter in condition");
            if (entry.getKey().equals("not")) checkMeters(entry.getValue().getAsJsonObject(), meters);
            if (Set.of("all", "any").contains(entry.getKey())) for (var value : entry.getValue().getAsJsonArray()) checkMeters(value.getAsJsonObject(), meters);
        }
    }
    private void loadScripts(Path root) throws IOException {
        if (scripts.isEmpty()) return;
        Path directory = root.toRealPath();
        Map<String, com.dragonminez.api.script.LuaProgram> next = new LinkedHashMap<>();
        for (var script : scripts) {
            String[] id = script.id().split(":", 2);
            Path file = directory.resolve(id[0]).resolve(id[1] + ".lua").normalize().toRealPath();
            if (!file.startsWith(directory) || Files.size(file) > 65536) throw new IllegalArgumentException("Script path escapes its directory or exceeds size limit");
            next.put(script.id(), new com.dragonminez.api.script.LuaProgram(script.id(), Files.readString(file, StandardCharsets.UTF_8)));
        }
        programs.putAll(next);
        content();
    }
    public static RuntimeConfig load(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.writeString(path, "{\n  \"combatTicks\": 200,\n  \"meters\": {},\n  \"rules\": []\n}\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        }
        if (Files.size(path) > 1048576) throw new IllegalArgumentException("Runtime configuration exceeds 1 MiB");
        var config = parse(Files.readString(path, StandardCharsets.UTF_8));
        config.loadScripts(path.getParent().getParent().resolve("scripts"));
        return config;
    }
}
