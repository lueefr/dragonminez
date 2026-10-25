package com.dragonminez.api.forms;

import org.junit.jupiter.api.Test;
import java.util.*;
import static com.dragonminez.api.forms.FormCompositionResolver.*;
import static org.junit.jupiter.api.Assertions.*;

class FormCompositionResolverTest {
    private static Multipliers m(double n) { return new Multipliers(n,n,n,n,n,n,n,n); }
    private static Definition d(String id, String base, String inherited, double n) {
        return new Definition(id,base,inherited,m(n));
    }
    @Test void dynamicRootPropagatesThroughChainAndRecomputesForNewUnlocks() {
        var r = new FormCompositionResolver();
        r.reload(List.of(d("group.first",null,null,2),d("group.second",null,null,4),
                d("custom.root","best",null,1.2),d("custom.next",null,"custom.root",1.5)));
        UUID player=UUID.randomUUID();
        r.updatePlayer(player,new PlayerState(1,null,null,Set.of("group.first","custom.root","custom.next"),Map.of()));
        var cached=r.resolve(player,"custom.next");
        assertEquals(3.6,cached.str(),1e-9);
        assertSame(cached,r.resolve(player,"custom.next"));
        r.updatePlayer(player,new PlayerState(2,null,null,Set.of("group.first","group.second","custom.root","custom.next"),Map.of()));
        assertEquals(7.2,r.resolve(player,"custom.next").str(),1e-9);
        r.reload(List.of(d("group.first",null,null,3),d("custom.root","best",null,1.2)));
        r.updatePlayer(player,new PlayerState(3,null,null,Set.of("group.first","custom.root"),Map.of()));
        assertEquals(3.6,r.resolve(player,"custom.root").str(),1e-9);
    }
    @Test void currentReferenceUsesPersistedAnchorWhenActiveFormIsItsOwnDescendant() {
        var r=new FormCompositionResolver();
        r.reload(List.of(d("group.base",null,null,3),d("custom.root","current",null,1.2),d("custom.next",null,"custom.root",2)));
        UUID p=UUID.randomUUID();
        r.updatePlayer(p,new PlayerState(1,"custom.next","group.base",Set.of(),Map.of()));
        assertEquals(7.2,r.resolve(p,"custom.next").str(),1e-9);
        r.updatePlayer(p,new PlayerState(2,".",".",Set.of(),Map.of()));
        assertEquals(2.4,r.resolve(p,"custom.next").str(),1e-9);
    }
    @Test void staticCycleFailsWithPathAndDoesNotReplacePreviousSnapshot() {
        var r=new FormCompositionResolver();
        r.reload(List.of(d("safe.one",null,null,2)));
        var ex=assertThrows(IllegalArgumentException.class,()->r.reload(List.of(d("a.one",null,"b.two",2),d("b.two",null,"a.one",2))));
        assertTrue(ex.getMessage().contains("a.one -> b.two -> a.one"));
        UUID p=UUID.randomUUID();r.updatePlayer(p,new PlayerState(1,null,null,Set.of(),Map.of()));
        assertEquals(2,r.resolve(p,"safe.one").str());
    }
    @Test void playersHaveIndependentProgressAndMastery() {
        var r=new FormCompositionResolver();
        r.reload(List.of(d("base.one",null,null,2),d("custom.one","best",null,1.2)));
        UUID a=UUID.randomUUID(),b=UUID.randomUUID();
        r.updatePlayer(a,new PlayerState(1,null,null,Set.of("base.one","custom.one"),Map.of("base.one",m(3))));
        r.updatePlayer(b,new PlayerState(1,null,null,Set.of("custom.one"),Map.of()));
        assertEquals(3.6,r.resolve(a,"custom.one").str(),1e-9);
        assertEquals(1.2,r.resolve(b,"custom.one").str(),1e-9);
    }
    @Test void invalidReferenceAndNonFiniteValuesFailClearly() {
        var r=new FormCompositionResolver();
        assertThrows(IllegalArgumentException.class,()->r.reload(List.of(d("custom.one",null,"missing.one",2))));
        assertThrows(IllegalArgumentException.class,()->m(Double.NaN));
        assertThrows(IllegalArgumentException.class,()->m(Double.POSITIVE_INFINITY));
    }
    @Test void additiveChainUsesResolvedDynamicRootAndZeroDeltas() {
        var resolver=new FormCompositionResolver();var player=UUID.randomUUID();
        resolver.reload(List.of(d("base.first",null,null,16),d("base.second",null,null,32),
            new Definition("custom.first","best",null,m(5),"add"),
            new Definition("custom.second",null,"custom.first",m(5),"add"),
            new Definition("custom.same",null,"custom.second",m(0),"add")));
        resolver.updatePlayer(player,new PlayerState(1,null,null,Set.of("base.first","custom.first","custom.second"),Map.of()));
        assertEquals(21,resolver.resolve(player,"custom.first").str());
        assertEquals(26,resolver.resolve(player,"custom.same").str());
        resolver.updatePlayer(player,new PlayerState(2,null,null,Set.of("base.first","base.second","custom.first","custom.second"),Map.of()));
        assertEquals(42,resolver.resolve(player,"custom.same").str());
        assertThrows(IllegalArgumentException.class,()->new Definition("bad.one",null,null,m(0),"multiply"));
    }
}
