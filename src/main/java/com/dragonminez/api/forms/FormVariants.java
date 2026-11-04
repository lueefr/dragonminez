package com.dragonminez.api.forms;

import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.StatsData;
import com.google.gson.Gson;
import java.util.*;

public final class FormVariants {
    public record Ref(String group,String form) {
        public String key(){return group+"."+form;}
    }
    private record Copy(FormConfig.FormData source,FormConfig.FormData target) {}
    private static final Map<Copy,FormConfig.FormData> copies=new java.util.concurrent.ConcurrentHashMap<>();
    private FormVariants() {}
    private static Ref parse(String reference) {
        int dot=reference.lastIndexOf('.');
        return new Ref(reference.substring(0,dot),reference.substring(dot+1));
    }
    public static Ref resolve(StatsData data,String group,String form) {
        if(group==null||form==null||group.isBlank()||form.isBlank()||!ConfigManager.getAttributes().hasForms())return new Ref(group,form);
        Ref current=new Ref(group,form);Set<String> visited=new HashSet<>();
        var active=ConfigManager.getAttributes().active(data);
        while(visited.add(current.key())) {
            String next=current.key();
            for(var definition:active)next=definition.forms.replace.getOrDefault(next,next);
            if(next.equals(current.key()))return current;
            current=parse(next);
        }
        throw new IllegalArgumentException("Cyclic active form replacement: "+group+"."+form);
    }
    public static Ref slot(StatsData data,String group,String form) {
        if(group==null||form==null||group.isBlank()||form.isBlank()||!ConfigManager.getAttributes().hasForms())return new Ref(group,form);
        Ref current=new Ref(group,form);Set<String> visited=new HashSet<>();
        var active=ConfigManager.getAttributes().active(data);
        while(visited.add(current.key())) {
            String next=current.key();
            for(var definition:active)for(var entry:definition.forms.replace.entrySet())
                if(entry.getValue().equals(next)){next=entry.getKey();break;}
            if(next.equals(current.key()))return current;
            current=parse(next);
        }
        throw new IllegalArgumentException("Cyclic form slot: "+group+"."+form);
    }
    public static boolean hidden(StatsData data,String group,String form) {
        return !slot(data,group,form).key().equals(group+"."+form);
    }
    public static boolean blocked(StatsData data,String group,String form) {
        if(!ConfigManager.getAttributes().hasForms())return false;
        var source=slot(data,group,form);
        String target=resolve(data,source.group(),source.form()).key();
        return ConfigManager.getAttributes().active(data).stream().anyMatch(definition->definition.forms.block.contains(source.key())||definition.forms.block.contains(target));
    }
    public static boolean granted(StatsData data,String group,String form) {
        if(!ConfigManager.getAttributes().hasForms())return false;
        String original=group+"."+form,target=resolve(data,group,form).key();
        return ConfigManager.getAttributes().active(data).stream().anyMatch(definition->definition.forms.grant.contains(original)||definition.forms.grant.contains(target));
    }
    public static FormConfig.FormData data(StatsData data,String group,String form,boolean stack) {
        var target=resolve(data,group,form);
        if(target.group()==null||target.form()==null||data==null)return null;
        return stack?ConfigManager.getStackForm(target.group(),target.form()):ConfigManager.getForm(data.getCharacter().getRaceName(),target.group(),target.form());
    }
    public static FormConfig.FormData project(StatsData data,String group,FormConfig.FormData source,boolean stack) {
        if(source==null)return null;
        var target=data(data,group,source.getName(),stack);
        if(target==null||target==source)return target;
        if(copies.size()>1024)copies.clear();
        return copies.computeIfAbsent(new Copy(source,target),key->{
            var gson=new Gson();var copy=gson.fromJson(gson.toJson(target),FormConfig.FormData.class);
            copy.setName(source.getName());copy.setUnlockOnSkillLevel(source.getUnlockOnSkillLevel());return copy;
        });
    }
}
