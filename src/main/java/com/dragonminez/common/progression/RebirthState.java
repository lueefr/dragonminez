package com.dragonminez.common.progression;

import com.dragonminez.common.config.RebirthConfig;
import net.minecraft.nbt.CompoundTag;

public final class RebirthState {
    private int deathLevel, stacks;
    private double earned, budget;
    private long elapsed, extraTicks;
    private boolean pending;
    private String originRace="", previousRace="";
    public String originRace() { return originRace; }
    public String previousRace() { return previousRace; }
    public void race(String previous,String next,boolean created) {
        if(originRace.isEmpty())originRace=created?previous:next;
        if((created||pending)&&!previous.equals(next))previousRace=previous;
    }

    public void resetOrigins(String race) { originRace=race;previousRace=""; }

    public int deathLevel() { return deathLevel; }
    public double earned() { return earned; }
    public double budget() { return budget; }
    public int stacks() { return stacks; }
    public boolean pending() { return pending; }
    public void record(int level) { deathLevel=Math.max(1,level); }
    public void prepare() { pending=true; }

    public void begin(RebirthConfig config,double target) {
        var bonus=config.bonus;var policy=bonus.stacking;
        boolean existing=stacks>0&&(!bonus.cap.enabled||earned<budget)&&(bonus.durationTicks<0||elapsed<duration(bonus.durationTicks));
        int limit=policy.limit==0?Integer.MAX_VALUE:policy.limit;
        stacks=existing?(int)Math.min(limit,(long)stacks+1):1;
        if(!existing||policy.renewal.equals("restart")) { earned=0;budget=Math.max(0,target); }
        else if(policy.renewal.equals("extend"))budget=Math.min(Double.MAX_VALUE,budget+Math.max(0,target));
        if(!existing||policy.durationRenewal.equals("restart")) { elapsed=0;extraTicks=0; }
        else if(policy.durationRenewal.equals("extend")&&bonus.durationTicks>0)
            extraTicks=extraTicks>Long.MAX_VALUE-bonus.durationTicks?Long.MAX_VALUE:extraTicks+bonus.durationTicks;
        pending=false;
    }

    private long duration(long ticks) { return extraTicks>Long.MAX_VALUE-ticks?Long.MAX_VALUE:ticks+extraTicks; }

    public void tick() { if(stacks>0&&elapsed<Long.MAX_VALUE)elapsed++; }

    public double award(RebirthConfig config,double amount) {
        if(!Double.isFinite(amount)||amount<=0)return amount;
        var bonus=config.bonus;
        if(!config.enabled||pending||stacks==0)return amount;
        boolean active=(!bonus.cap.enabled||earned<budget)&&(bonus.durationTicks<0||elapsed<duration(bonus.durationTicks));
        double result=amount;
        if(active) {
            int count=bonus.stacking.limit==0?stacks:Math.min(stacks,bonus.stacking.limit);
            double multiplier=switch(bonus.stacking.mode) {
                case "add" -> Math.max(0,1+(bonus.multiplier-1)*count);
                case "maximum" -> bonus.multiplier;
                default -> Math.pow(bonus.multiplier,count);
            };
            multiplier=Math.min(Double.MAX_VALUE,multiplier);
            if(multiplier>0) {
                double boosted=bonus.cap.enabled?Math.min(amount,Math.max(0,budget-earned)/multiplier):amount;
                result=Math.min(Double.MAX_VALUE,boosted*multiplier+(amount-boosted));
            } else result=0;
        }
        double received=result;
        earned=Math.min(Double.MAX_VALUE,earned+received);
        return received;
    }

    public CompoundTag save() {
        var tag=new CompoundTag();tag.putInt("DeathLevel",deathLevel);tag.putInt("Stacks",stacks);
        tag.putString("OriginRace",originRace);tag.putString("PreviousRace",previousRace);
        tag.putDouble("Earned",earned);tag.putDouble("Budget",budget);tag.putLong("Elapsed",elapsed);tag.putLong("ExtraTicks",extraTicks);tag.putBoolean("Pending",pending);return tag;
    }
    public void load(CompoundTag tag) {
        originRace=tag.getString("OriginRace");previousRace=tag.getString("PreviousRace");
        deathLevel=Math.max(0,tag.getInt("DeathLevel"));stacks=Math.max(0,tag.getInt("Stacks"));
        earned=safe(tag.getDouble("Earned"));budget=safe(tag.getDouble("Budget"));elapsed=Math.max(0,tag.getLong("Elapsed"));extraTicks=Math.max(0,tag.getLong("ExtraTicks"));pending=tag.getBoolean("Pending");
    }
    private static double safe(double value) { return Double.isFinite(value)?Math.max(0,value):0; }
}
