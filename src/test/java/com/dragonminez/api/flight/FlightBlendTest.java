package com.dragonminez.api.flight;

import com.dragonminez.api.flight.FlightMotion.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightBlendTest {
    @Test void presentationIsContinuousAcrossJitter(){
        var initial=FlightMotion.initial(Vector.ZERO,Vector.ZERO,0,0);
        var blend=new FlightBlend(initial);
        var target=new State(new Vector(10,0,0),new Vector(1,0,0),FlightMotion.axis(0,0,1,Math.PI/2),Vector.ZERO);
        blend.update(target,1_000_000_000L,.1);
        assertEquals(0,blend.sample(1_000_000_000L).position().x(),1e-9);
        assertEquals(5,blend.sample(1_050_000_000L).position().x(),1e-9);
        var half=blend.sample(1_050_000_000L);
        blend.update(new State(new Vector(20,0,0),target.velocity(),target.rotation(),Vector.ZERO),1_050_000_000L,.1);
        assertEquals(half.position(),blend.sample(1_050_000_000L).position());
        assertEquals(20,blend.sample(2_000_000_000L).position().x(),1e-9);
        assertEquals(0,initial.position().x());
    }
    @Test void delayedAcknowledgementsReplayTheSameTrajectory(){
        var tuning=new Tuning(.04,.03,.3,.001,0,.8,1.5,.1,.3,.2);
        var states=new java.util.ArrayList<State>();var controls=new java.util.ArrayList<Controls>();
        states.add(FlightMotion.initial(Vector.ZERO,Vector.ZERO,0,0));
        for(int tick=1;tick<=100;tick++){
            var input=new Controls(Math.sin(tick*.1),Math.cos(tick*.2),.2,1,0,tick%3==0,false);controls.add(input);
            states.add(FlightMotion.step(states.getLast(),input,tuning));
        }
        for(int now=10;now<=100;now++){
            int acknowledged=now-1-now%8;var replay=states.get(acknowledged);
            for(int index=acknowledged;index<now;index++)replay=FlightMotion.step(replay,controls.get(index),tuning);
            assertEquals(states.get(now).position(),replay.position());
            var a=states.get(now).rotation();var b=replay.rotation();
            assertEquals(1,Math.abs(a.x()*b.x()+a.y()*b.y()+a.z()*b.z()+a.w()*b.w()),1e-12);
        }
    }
    @Test void disabledBlendAndQuaternionSignAreSafe(){
        var state=FlightMotion.initial(Vector.ZERO,Vector.ZERO,0,0);
        var blend=new FlightBlend(state);
        var q=state.rotation();var opposite=new Rotation(-q.x(),-q.y(),-q.z(),-q.w());
        blend.update(new State(new Vector(1,2,3),Vector.ZERO,opposite,Vector.ZERO),10,0);
        assertEquals(new Vector(1,2,3),blend.sample(10).position());
        assertThrows(IllegalArgumentException.class,()->blend.update(state,10,.5));
    }
}
