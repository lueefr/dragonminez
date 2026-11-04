package com.dragonminez.common.progression;

import com.dragonminez.common.config.RebirthConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RebirthStateTest {
    @Test void cumulativeBudget() {
        var config=new RebirthConfig();var state=new RebirthState();
        state.begin(config,200000);
        assertEquals(100000,state.award(config,50000));
        assertEquals(100000,state.award(config,50000));
        assertEquals(200000,state.earned());
        assertEquals(10,state.award(config,10));
    }
    @Test void crossingBudget() {
        var config=new RebirthConfig();var state=new RebirthState();state.begin(config,50);
        assertEquals(125,state.award(config,100));
        assertEquals(125,state.earned());
        assertEquals(100,state.award(config,100));
    }
    @Test void infiniteRenewal() {
        var config=new RebirthConfig();config.bonus.cap.enabled=false;
        var state=new RebirthState();state.begin(config,0);state.begin(config,0);
        assertEquals(1,state.stacks());assertEquals(200,state.award(config,100));
        config.bonus.stacking.limit=3;state.begin(config,0);
        assertEquals(2,state.stacks());assertEquals(400,state.award(config,100));
        config.bonus.stacking.mode="add";
        assertEquals(300,state.award(config,100));
    }
    @Test void configuredExpiry() {
        var config=new RebirthConfig();config.bonus.durationTicks=1;
        var state=new RebirthState();state.begin(config,200000);state.tick();
        assertEquals(100,state.award(config,100));
        config.bonus.durationTicks=-1;config.bonus.stacking.renewal="keep";
        state.begin(config,500000);assertEquals(200000,state.budget());
        config.bonus.stacking.renewal="extend";state.begin(config,500000);
        assertEquals(700000,state.budget());
    }
    @Test void fractionalAwards() {
        var config=new RebirthConfig();config.bonus.multiplier=1.5;
        var state=new RebirthState();state.begin(config,10);
        assertEquals(1.5,state.award(config,1));
        assertEquals(1.5,state.earned());
    }
    @Test void durationRenewal() {
        var config=new RebirthConfig();config.bonus.cap.enabled=false;config.bonus.durationTicks=2;
        config.bonus.stacking.durationRenewal="extend";
        var state=new RebirthState();state.begin(config,0);state.tick();state.begin(config,0);
        state.tick();state.tick();assertEquals(200,state.award(config,100));
        state.tick();assertEquals(100,state.award(config,100));
    }

}
