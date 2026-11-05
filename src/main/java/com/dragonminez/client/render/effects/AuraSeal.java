package com.dragonminez.client.render.effects;

/** Reversible geometric absorption. Zero means no shell exists. */
public final class AuraSeal {
    private double openness;
    public AuraSeal(){this(false);}
    public AuraSeal(boolean sealed){openness=sealed?0:1;}
    public double update(boolean sealed,double seconds) {
        return update(sealed,seconds,0.4);
    }
    public double update(boolean sealed,double seconds,double duration){
        duration=Double.isFinite(duration)?Math.max(0.01,Math.min(10,duration)):0.4;
        double delta=Math.max(0,seconds)/duration;
        openness=sealed?Math.max(0,openness-delta):Math.min(1,openness+delta);
        return factor();
    }
    public double factor(){return openness*openness*(3-2*openness);}
}
