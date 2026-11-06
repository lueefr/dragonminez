package com.dragonminez.api.flight;

import com.dragonminez.api.flight.FlightMotion.*;

/** Presentation-only interpolation; authoritative poses are never modified. */
public final class FlightBlend {
    private State from, target;
    private long stamp;
    private double seconds;
    public FlightBlend(State state){from=target=state;}
    public State sample(long now){
        double amount=seconds<=0?1:Math.clamp((now-stamp)/(seconds*1e9),0,1);
        return new State(blend(from.position(),target.position(),amount),blend(from.velocity(),target.velocity(),amount),
                from.rotation().blend(target.rotation(),amount),blend(from.angular(),target.angular(),amount));
    }
    public void update(State state,long now,double duration){
        if(!Double.isFinite(duration)||duration<0||duration>.25)throw new IllegalArgumentException("Flight presentation duration must be 0..0.25 seconds");
        from=sample(now);target=state;stamp=now;seconds=duration;
    }
    private static Vector blend(Vector a,Vector b,double amount){return a.scale(1-amount).add(b.scale(amount));}
}
