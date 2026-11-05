package com.dragonminez.common.config;

import com.dragonminez.api.content.ContentProgress;
import com.dragonminez.api.rules.Conditions;
import com.dragonminez.common.stats.StatsData;
import com.google.gson.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

public final class AttributeConfig {
    private Map<String,Definition> attributes = new LinkedHashMap<>();
    private List<Detection> detection = new ArrayList<>();
    private transient Map<String,String> aliases=Map.of();
    private transient boolean hasForms, hasSnapshots;
    public boolean hasForms() { return hasForms; }
    private static final ThreadLocal<Set<StatsData>> selecting=ThreadLocal.withInitial(HashSet::new);
    private static final ThreadLocal<Set<StatsData>> evaluating=ThreadLocal.withInitial(HashSet::new);
    public static final class Definition {
        public boolean enabled = true;
        public JsonObject when = new JsonObject();
        public Map<String,Double> base = new LinkedHashMap<>();
        public double tp = 1;
        public Composition composition = new Composition();
        public Map<String,JsonElement> properties = new LinkedHashMap<>();
        public String stackContent;
        public int stackLimit = 1;
        public String stacking = "multiply";
        public Forms forms = new Forms();
    }
    public static final class Composition {
        public List<Source> inherits = new ArrayList<>();
    }
    public static final class Source {
        public String form;
        public String id;
        public String mode = "live";
        public String capture = "once";
        public double factor = 1;
        public Set<String> stats = new LinkedHashSet<>();
        private transient boolean missing, failed;
    }
    public static final class Detection {
        public Set<String> channels = new LinkedHashSet<>();
        public JsonObject target = new JsonObject(), observer = new JsonObject();
        public String mode = "restrict";
    }
    private record Query(StatsData data,String key) {}
    private static final ThreadLocal<Set<Query>> queries=ThreadLocal.withInitial(HashSet::new);
    public static final class Forms {
        public Map<String,String> replace = new LinkedHashMap<>();
        public List<String> block = new ArrayList<>(), grant = new ArrayList<>();
    }
    public List<Definition> active(StatsData data) {
        if(data==null||attributes.isEmpty()||!selecting.get().add(data))return List.of();
        try{return attributes.values().stream().filter(definition->definition.enabled
                &&(definition.stackContent==null||data.getContentProgress().count(definition.stackContent)>0)
                &&Conditions.test(new Conditions.Context(data,null),definition.when)).toList();}
        finally{selecting.get().remove(data);if(selecting.get().isEmpty())selecting.remove();}
    }
    public String canonical(String reference) {
        if(!aliases.containsKey(reference))return reference;
        Set<String> visited=new HashSet<>();String current=reference;
        while(visited.add(current)) {
            String next=aliases.get(current);
            if(next==null)return current;
            current=next;
        }
        throw new IllegalArgumentException("Cyclic form mastery alias: "+reference);
    }

    public static AttributeConfig parse(String json) {
        var config=new Gson().fromJson(json,AttributeConfig.class);
        if(config==null||config.attributes==null||config.detection==null)throw new IllegalArgumentException("Missing attributes");
        config.attributes.forEach((id,definition)->{
            ContentProgress.requireId(id);
            if(definition==null||definition.when==null||definition.base==null)throw new IllegalArgumentException("Invalid attribute: "+id);
            Conditions.validate(definition.when);
            if(definition.composition==null||definition.composition.inherits==null||definition.properties==null)throw new IllegalArgumentException("Missing attribute composition/properties: "+id);
            definition.properties.forEach((key,value)->{ContentProgress.requireId(key);propertyValue(value);});
            Set<String> sources = new HashSet<>();
            for(var source:definition.composition.inherits) {
                if(source==null||source.stats==null)throw new IllegalArgumentException("Invalid base form source: "+id);
                Conditions.form(source.form);factor(source.factor);
                if (!Set.of("live", "snapshot").contains(source.mode) || !Set.of("once", "activation").contains(source.capture)) throw new IllegalArgumentException("Invalid inheritance mode/capture: " + id);
                if (source.mode.equals("snapshot")) {
                    ContentProgress.requireId(source.id);
                    if (!sources.add(source.id)) throw new IllegalArgumentException("Duplicate snapshot source: " + source.id);
                }
                for(String stat:source.stats)if(!Set.of("STR","SKP","RES","DEF","STM","VIT","PWR","ENE","SPEED").contains(stat))throw new IllegalArgumentException("Unknown source stat: "+stat);
            }
            if(definition.stackContent!=null)ContentProgress.requireId(definition.stackContent);
            if(definition.stackLimit<0||!Set.of("multiply","add","maximum").contains(definition.stacking))throw new IllegalArgumentException("Invalid attribute stacking: "+id);
            if(definition.forms==null||definition.forms.replace==null||definition.forms.block==null||definition.forms.grant==null)throw new IllegalArgumentException("Invalid attribute forms: "+id);
            definition.forms.replace.forEach((source,target)->{Conditions.form(source);Conditions.form(target);});
            definition.forms.block.forEach(Conditions::form);definition.forms.grant.forEach(Conditions::form);
            factor(definition.tp);
            definition.base.forEach((stat,value)->{if(!Set.of("all","STR","SKP","RES","DEF","STM","VIT","PWR","ENE","SPEED").contains(stat))throw new IllegalArgumentException("Unknown base stat: "+stat);factor(value);});
        });
        if (config.attributes.values().stream().mapToInt(value -> value.composition.inherits.size()).sum() > 256) throw new IllegalArgumentException("At most 256 attribute form sources");
        for(var rule:config.detection) {
            if(rule==null||rule.channels==null||rule.target==null||rule.observer==null||!Set.of("restrict","replace").contains(rule.mode))throw new IllegalArgumentException("Invalid detection rule");
            for(String channel:rule.channels)if(!Set.of("ki_sense","lock_on","instant_transmission","scouter").contains(channel))ContentProgress.requireId(channel);
            Conditions.validate(rule.target);Conditions.validate(rule.observer);
        }
        Map<String,String> aliases=new HashMap<>();
        config.attributes.values().forEach(definition->definition.forms.replace.forEach((source,target)->{
            String previous=aliases.putIfAbsent(target,source);
            if(previous!=null&&!previous.equals(source))throw new IllegalArgumentException("Ambiguous mastery alias: "+target);
        }));
        config.aliases=Map.copyOf(aliases);
        aliases.keySet().forEach(config::canonical);
        config.hasForms=config.attributes.values().stream().anyMatch(definition->!definition.forms.replace.isEmpty()||!definition.forms.block.isEmpty()||!definition.forms.grant.isEmpty());
        config.hasSnapshots=config.attributes.values().stream().anyMatch(value->value.composition.inherits.stream().anyMatch(source->source.mode.equals("snapshot")));
        return config;
    }
    private static void factor(Double value) {
        if(value==null||!Double.isFinite(value)||value<0)throw new IllegalArgumentException("Attribute factors must be finite and nonnegative");
    }
    private static double product(double first,double second) { return first==0||second==0?0:Math.min(Double.MAX_VALUE,first*second); }
    private static double stacked(double value,int count,String mode) {
        return Math.min(Double.MAX_VALUE,switch(mode) {
            case "add" -> Math.max(0,1+(value-1)*count);
            case "maximum" -> value;
            default -> Math.pow(value,count);
        });
    }
    public double multiplier(StatsData data,String stat) {
        if(attributes.isEmpty()||!evaluating.get().add(data))return 1;
        try {
            double total=1;
            for(var entry:attributes.entrySet()) {
                var definition = entry.getValue();
                if(!definition.enabled||!Conditions.test(new Conditions.Context(data,null),definition.when))continue;
                int count=definition.stackContent==null?1:data.getContentProgress().count(definition.stackContent);
                if(definition.stackLimit>0)count=Math.min(count,definition.stackLimit);
                if(count==0)continue;
                double value=stat.equals("TP")?definition.tp:product(definition.base.getOrDefault("all",1d),definition.base.getOrDefault(stat,1d));
                if(stat.equals("DEF")||stat.equals("STM"))value=product(value,definition.base.getOrDefault("RES",1d));
                if(!stat.equals("TP"))for(var source:definition.composition.inherits) {
                    if(!source.stats.isEmpty()&&!source.stats.contains(stat)&&!((stat.equals("DEF")||stat.equals("STM"))&&source.stats.contains("RES")))continue;
                    value=product(value,source.mode.equals("snapshot") ? captured(data,entry.getKey(),source,stat) : sourceValue(data,source,stat));
                }
                total=product(total,stacked(value,count,definition.stacking));
            }
            return total;
        } finally {evaluating.get().remove(data);if(evaluating.get().isEmpty())evaluating.remove();}
    }
    private static double captured(StatsData data, String id, Source source, String stat) {
        String key = id + "/" + source.id;
        return data.getSnapshots().has(key) ? product(source.factor,data.getSnapshots().value(key,stat)) : 1;
    }
    public void capture(StatsData data) {
        if (!(data.getPlayer() instanceof net.minecraft.server.level.ServerPlayer)) return;
        if (!hasSnapshots) { data.getSnapshots().refresh(Set.of()); return; }
        Set<String> active = new HashSet<>();
        for (var entry : attributes.entrySet()) {
            var definition = entry.getValue();
            if (!definition.enabled || (definition.stackContent != null && data.getContentProgress().count(definition.stackContent) == 0)
                    || !Conditions.test(new Conditions.Context(data,null),definition.when)) continue;
            for (var source : definition.composition.inherits) if (source.mode.equals("snapshot")) {
                String id = entry.getKey() + "/" + source.id;
                if (source.failed) continue;
                try {
                    if (!data.getSnapshots().has(id) || (source.capture.equals("activation") && !data.getSnapshots().active(id))) {
                        String[] reference = Conditions.form(source.form);
                        var form = ConfigManager.getForm(data.getCharacter().getRaceName(), reference[0], reference[1]);
                        if (form == null) form = ConfigManager.getStackForm(reference[0], reference[1]);
                        if (form == null) {
                            if (!source.missing) { source.missing = true; com.dragonminez.LogUtil.warn(com.dragonminez.Env.COMMON, "Snapshot source {} is unavailable for race {}", source.form, data.getCharacter().getRaceName()); }
                            continue;
                        }
                        Map<String, Double> factors = new LinkedHashMap<>();
                        for (String stat : com.dragonminez.common.progression.PowerSnapshots.STATS) factors.put(stat, data.getFormComposition().multiplier(data, reference[0], form, stat));
                        data.getSnapshots().capture(id, factors);
                    }
                    active.add(id);
                } catch (RuntimeException ex) {
                    source.failed = true;
                    com.dragonminez.LogUtil.error(com.dragonminez.Env.COMMON, "Snapshot source {} failed: {}. Capture disabled until reload.", id, ex.getMessage());
                }
            }
        }
        data.getSnapshots().refresh(active);
    }
    private static void propertyValue(JsonElement value) {
        if(value==null||value.isJsonNull()||value.isJsonObject())throw new IllegalArgumentException("Properties must be scalar values or arrays");
        if(value.isJsonArray())for(var entry:value.getAsJsonArray()) {
            if(!entry.isJsonPrimitive())throw new IllegalArgumentException("Property arrays must contain scalar values");
            propertyValue(entry);
        }
        else if(value.getAsJsonPrimitive().isNumber()&&!Double.isFinite(value.getAsDouble()))throw new IllegalArgumentException("Property number must be finite");
    }
    private static double sourceValue(StatsData data,Source source,String stat) {
        String[] reference=Conditions.form(source.form);
        var form=ConfigManager.getForm(data.getCharacter().getRaceName(),reference[0],reference[1]);
        if(form==null)form=ConfigManager.getStackForm(reference[0],reference[1]);
        if(form==null) {
            if(!source.missing){source.missing=true;com.dragonminez.LogUtil.warn(com.dragonminez.Env.COMMON,"Attribute source {} is unavailable for race {}",source.form,data.getCharacter().getRaceName());}
            return 1;
        }
        return product(source.factor,data.getFormComposition().multiplier(data,reference[0],form,stat));
    }
    public JsonElement property(StatsData data,String key) {
        if(data==null)return JsonNull.INSTANCE;
        var query=new Query(data,key);
        if(!queries.get().add(query))return JsonNull.INSTANCE;
        try {
            JsonElement value=JsonNull.INSTANCE;
            for(var definition:attributes.values())if(definition.enabled&&definition.properties.containsKey(key)
                    &&(definition.stackContent==null||data.getContentProgress().count(definition.stackContent)>0)
                    &&Conditions.test(new Conditions.Context(data,null),definition.when))value=definition.properties.get(key);
            return value;
        } finally {queries.get().remove(query);if(queries.get().isEmpty())queries.remove();}
    }
    public boolean detects(StatsData observer,StatsData target,String channel,boolean fallback) {
        if(observer==target)return true;
        if(target==null)return fallback;
        boolean result=fallback;
        for(var rule:detection) {
            if(!rule.channels.isEmpty()&&!rule.channels.contains(channel))continue;
            if(!Conditions.test(new Conditions.Context(target,null),rule.target))continue;
            boolean allowed=observer!=null&&Conditions.test(new Conditions.Context(observer,null),rule.observer);
            result=rule.mode.equals("replace")?allowed:result&&allowed;
        }
        return result;
    }
    public static AttributeConfig load(Path file) throws IOException {
        if(!Files.exists(file)){Files.createDirectories(file.getParent());Files.writeString(file,"{\n  \"attributes\": {}\n}\n",StandardOpenOption.CREATE_NEW);}
        if(Files.size(file)>1048576)throw new IllegalArgumentException("Attribute configuration exceeds 1 MiB");
        return parse(Files.readString(file));
    }
}
