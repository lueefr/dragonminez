package com.dragonminez.common.config;

import com.dragonminez.api.rules.*;
import com.dragonminez.api.content.ContentProgress;
import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Optional rules.json; absent files have no rewards or additional purchase restrictions. */
public final class RuleConfig {
    private Map<String,List<JsonObject>> content=new LinkedHashMap<>();
    private Map<String,Map<String,JsonObject>> buy=new LinkedHashMap<>();
    public List<JsonObject> rewards(String id){return content.getOrDefault(id,List.of());}
    public List<JsonObject> requirements(String skill,int level){
        var levels=buy.getOrDefault(skill,Map.of());var result=new ArrayList<JsonObject>();
        if(levels.containsKey("*"))result.add(levels.get("*"));if(levels.containsKey(""+level))result.add(levels.get(""+level));return result;
    }
    public static RuleConfig parse(String json){
        RuleConfig config=new GsonBuilder().registerTypeAdapterFactory(new com.dragonminez.common.config.schema.ContentAdapters()).create().fromJson(json,RuleConfig.class);if(config==null||config.content==null||config.buy==null)throw new IllegalArgumentException("Invalid rules.json");
        config.content.forEach((id,list)->{ContentProgress.requireId(id);if(list==null)throw new IllegalArgumentException("Null rewards for "+id);list.forEach(Rewards::validate);});
        config.buy.forEach((id,levels)->{Conditions.id(id);if(levels==null)throw new IllegalArgumentException("Null purchase rules for "+id);levels.forEach((level,condition)->{if(!level.equals("*") && (!level.matches("[1-9][0-9]{0,5}")))throw new IllegalArgumentException("Invalid target skill level: "+level);if(condition==null)throw new IllegalArgumentException("Null purchase condition");Conditions.validate(condition);});});
        config.content.keySet().forEach(id -> config.checkCycle(id,new LinkedHashSet<>(),new HashSet<>()));return config;
    }
    private void checkCycle(String id,Set<String> visiting,Set<String> done){
        if(done.contains(id))return;if(visiting.size()>=64)throw new IllegalArgumentException("Content reward chain exceeds 64 entries");if(!visiting.add(id))throw new IllegalArgumentException("Content reward cycle: "+visiting+" -> "+id);
        for(var reward:rewards(id))if(reward.has("content"))checkCycle(reward.get("content").getAsString(),visiting,done);
        visiting.remove(id);done.add(id);
    }
    public static RuleConfig load(Path file)throws IOException{
        if(!Files.exists(file))return new RuleConfig();if(Files.size(file)>1048576)throw new IllegalArgumentException(file+": exceeds 1 MiB");
        try{var flat=com.dragonminez.common.config.schema.ContentMigration.load(file,
            com.dragonminez.common.config.schema.ContentSchema.Kind.RULES,tree->parse(tree.toString()));return parse(flat.toString());}catch(RuntimeException ex){throw new IllegalArgumentException(file+": "+ex.getMessage(),ex);}
    }
}
