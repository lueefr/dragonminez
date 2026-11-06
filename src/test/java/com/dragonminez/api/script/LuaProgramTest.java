package com.dragonminez.api.script;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class LuaProgramTest {
    @Test void typedCommands() {
        var script = new LuaProgram("example:spirit", "return {onEvent=function(ctx) return {{meter='example:spirit',operation='add',value=ctx.values['event.damage']*2}} end}");
        var actions = script.evaluate("dmz:damage_received", Map.of("event.damage", 4d));
        assertEquals(8, actions.getFirst().value()); assertTrue(actions.getFirst().additive());
    }
    @Test void typedRewards() {
        var script=new LuaProgram("example:reward","return {onEvent=function(ctx) return {{reward={resource={id='energy',add=-2}}},{meter='example:count',operation='add',value=1}} end}");
        var batch=script.evaluateBatch("example:test",Map.of());
        assertEquals(-2,batch.rewards().getFirst().getAsJsonObject("resource").get("add").getAsInt());
        assertEquals(1,batch.meters().size());
        var mixed=new LuaProgram("example:mixed","return {onEvent=function(ctx) return {{reward={item={id='minecraft:stone',count=1}},meter='example:count'}} end}");
        assertThrows(IllegalArgumentException.class,()->mixed.evaluateBatch("example:test",Map.of()));
        var cyclic=new LuaProgram("example:cycle","return {onEvent=function(ctx) local a={} a.self=a return {{reward=a}} end}");
        assertThrows(IllegalArgumentException.class,()->cyclic.evaluateBatch("example:test",Map.of()));
    }
    @Test void unavailableApis() {
        var script = new LuaProgram("example:test", "assert(io==nil and os==nil and debug==nil and package==nil and luajava==nil and load==nil and loadfile==nil and dofile==nil and require==nil); return {onEvent=function(ctx) end}");
        assertTrue(script.evaluate("dmz:tick", Map.of()).isEmpty());
    }
    @Test void runawayCallbacks() {
        var script = new LuaProgram("example:loop", "return {onEvent=function(ctx) while true do end end}");
        assertThrows(IllegalArgumentException.class, () -> script.evaluate("dmz:tick", Map.of()));
        var caught = new LuaProgram("example:catch", "return {onEvent=function(ctx) while true do pcall(function() while true do end end) end end}");
        assertThrows(IllegalArgumentException.class, () -> caught.evaluate("dmz:tick", Map.of()));
        var strings = new LuaProgram("example:strings", "return {onEvent=function(ctx) local s='xx'; for i=1,100 do s=s..s end end}");
        assertThrows(IllegalArgumentException.class, () -> strings.evaluate("dmz:tick", Map.of()));
    }
    @Test void freshEnvironments() {
        var script = new LuaProgram("example:state", "counter=0; return {onEvent=function(ctx) counter=counter+1; return {{meter='example:count',operation='set',value=counter}} end}");
        assertEquals(1, script.evaluate("dmz:tick", Map.of()).getFirst().value());
        assertEquals(1, script.evaluate("dmz:tick", Map.of()).getFirst().value());
        assertThrows(IllegalArgumentException.class, () -> new LuaProgram("example:broken", "return {"));
        assertThrows(IllegalArgumentException.class, () -> new LuaProgram("example:startup", "while true do end"));
    }
}
