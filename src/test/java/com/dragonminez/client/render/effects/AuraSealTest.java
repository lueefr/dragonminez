package com.dragonminez.client.render.effects;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AuraSealTest {
    @Test void startsSealedAndKeepsIndependentTiming(){
        var inner=new AuraSeal(true); var outer=new AuraSeal(false);
        assertEquals(0,inner.factor());
        assertEquals(.5,inner.update(false,.1,.2),1e-9);
        assertEquals(1,outer.factor());
        assertEquals(.5,outer.update(true,.5,1),1e-9);
        assertEquals(1,inner.update(false,.1,.2));
        assertEquals(.5,outer.update(true,0,1),1e-9);
    }
    @Test void contractsIntoNothingAndReversesWithoutSnapping(){
        var seal=new AuraSeal();assertEquals(1,seal.factor());
        assertEquals(.5,seal.update(true,.2),1e-9);
        assertTrue(seal.update(false,.05)>.5);
        assertEquals(0,seal.update(true,1));
        assertEquals(0,seal.update(true,1));
        assertEquals(.5,seal.update(false,.2),1e-9);
        assertEquals(1,seal.update(false,1));
    }
}
