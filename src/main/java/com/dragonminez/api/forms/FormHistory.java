package com.dragonminez.api.forms;

import net.minecraft.nbt.CompoundTag;
import java.util.*;

/** Lifetime activation counts are scoped by race and group.form; active first-use sessions survive save/load. */
public final class FormHistory {
    private final Map<String,Integer> uses=new HashMap<>();
    private final Set<String> first=new HashSet<>();
    private final Map<String,String> active=new HashMap<>();
    private long revision;
    private String key(String race,String form) { return race + ":" + form; }
    public int count(String race,String form) { return uses.getOrDefault(key(race,form),0); }
    public boolean used(String race,String form) { return count(race,form)>0; }
    public boolean first(String race,String form) { return first.contains(key(race,form)); }
    public long revision() { return revision; }
    public boolean update(String channel,String race,String form) {
        String next=form==null||form.isEmpty()?"":key(race,form),old=active.getOrDefault(channel,"");
        if(old.equals(next))return false;
        first.remove(old);active.put(channel,next);revision++;
        if(next.isEmpty())return false;
        int count=uses.compute(next,(k,n)->n==null?1:n==Integer.MAX_VALUE?n:n+1);
        if(count==1)first.add(next);return true;
    }
    public void resume(String channel,String race,String form) {
        if(form==null||form.isEmpty())return;String key=key(race,form);active.put(channel,key);uses.putIfAbsent(key,1);revision++;
    }
    public CompoundTag save() {
        CompoundTag tag=new CompoundTag(),counts=new CompoundTag(),active=new CompoundTag();
        uses.forEach(counts::putInt); first.forEach(key -> active.putBoolean(key,true));
        tag.put("Uses",counts);tag.put("First",active);
        var channels=new CompoundTag();this.active.forEach(channels::putString);tag.put("Active",channels);return tag;
    }
    public void load(CompoundTag tag) {
        uses.clear();first.clear();active.clear();var counts=tag.getCompound("Uses");
        counts.getAllKeys().forEach(key -> { int n=counts.getInt(key);if(n>0) uses.put(key,n); });
        tag.getCompound("Active").getAllKeys().forEach(channel -> active.put(channel,tag.getCompound("Active").getString(channel)));
        tag.getCompound("First").getAllKeys().forEach(key -> {if(uses.getOrDefault(key,0)==1) first.add(key);});revision++;
    }
    public void copyFrom(FormHistory other) { load(other.save()); }
}
