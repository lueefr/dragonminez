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
    private transient Map<String,String> aliases=Map.of();
    private transient boolean hasForms;
    public boolean hasForms() { return hasForms; }
    private static final ThreadLocal<Set<StatsData>> evaluating=ThreadLocal.withInitial(HashSet::new);
    public static final class Definition {
        public boolean enabled = true;
        public JsonObject when = new JsonObject();
        public Map<String,Double> base = new LinkedHashMap<>();
        public double tp = 1;
        public String stackContent;
        public int stackLimit = 1;
        public String stacking = "multiply";
        public Forms forms = new Forms();
    }
    public static final class Forms {
        public Map<String,String> replace = new LinkedHashMap<>();
        public List<String> block = new ArrayList<>(), grant = new ArrayList<>();
    }
    public List<Definition> active(StatsData data) {
        if(data==null||attributes.isEmpty()||!evaluating.get().add(data))return List.of();
        try{return attributes.values().stream().filter(definition->definition.enabled
                &&(definition.stackContent==null||data.getContentProgress().count(definition.stackContent)>0)
                &&Conditions.test(new Conditions.Context(data,null),definition.when)).toList();}
        finally{evaluating.get().remove(data);if(evaluating.get().isEmpty())evaluating.remove();}
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
        if(config==null||config.attributes==null)throw new IllegalArgumentException("Missing attributes");
        config.attributes.forEach((id,definition)->{
            ContentProgress.requireId(id);
            if(definition==null||definition.when==null||definition.base==null)throw new IllegalArgumentException("Invalid attribute: "+id);
            Conditions.validate(definition.when);
            if(definition.stackContent!=null)ContentProgress.requireId(definition.stackContent);
            if(definition.stackLimit<0||!Set.of("multiply","add","maximum").contains(definition.stacking))throw new IllegalArgumentException("Invalid attribute stacking: "+id);
            if(definition.forms==null||definition.forms.replace==null||definition.forms.block==null||definition.forms.grant==null)throw new IllegalArgumentException("Invalid attribute forms: "+id);
            definition.forms.replace.forEach((source,target)->{Conditions.form(source);Conditions.form(target);});
            definition.forms.block.forEach(Conditions::form);definition.forms.grant.forEach(Conditions::form);
            factor(definition.tp);
            definition.base.forEach((stat,value)->{if(!Set.of("all","STR","SKP","RES","DEF","STM","VIT","PWR","ENE","SPEED").contains(stat))throw new IllegalArgumentException("Unknown base stat: "+stat);factor(value);});
        });
        Map<String,String> aliases=new HashMap<>();
        config.attributes.values().forEach(definition->definition.forms.replace.forEach((source,target)->{
            String previous=aliases.putIfAbsent(target,source);
            if(previous!=null&&!previous.equals(source))throw new IllegalArgumentException("Ambiguous mastery alias: "+target);
        }));
        config.aliases=Map.copyOf(aliases);
        aliases.keySet().forEach(config::canonical);
        config.hasForms=config.attributes.values().stream().anyMatch(definition->!definition.forms.replace.isEmpty()||!definition.forms.block.isEmpty()||!definition.forms.grant.isEmpty());
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
            for(var definition:attributes.values()) {
                if(!definition.enabled||!Conditions.test(new Conditions.Context(data,null),definition.when))continue;
                int count=definition.stackContent==null?1:data.getContentProgress().count(definition.stackContent);
                if(definition.stackLimit>0)count=Math.min(count,definition.stackLimit);
                if(count==0)continue;
                double value=stat.equals("TP")?definition.tp:product(definition.base.getOrDefault("all",1d),definition.base.getOrDefault(stat,1d));
                if(stat.equals("DEF")||stat.equals("STM"))value=product(value,definition.base.getOrDefault("RES",1d));
                total=product(total,stacked(value,count,definition.stacking));
            }
            return total;
        } finally {evaluating.get().remove(data);if(evaluating.get().isEmpty())evaluating.remove();}
    }
    public static AttributeConfig load(Path file) throws IOException {
        if(!Files.exists(file)){Files.createDirectories(file.getParent());Files.writeString(file,"{\n  \"attributes\": {}\n}\n",StandardOpenOption.CREATE_NEW);}
        if(Files.size(file)>1048576)throw new IllegalArgumentException("Attribute configuration exceeds 1 MiB");
        return parse(Files.readString(file));
    }
}
