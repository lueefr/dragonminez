package com.dragonminez.gametest;

import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.nio.file.*;
import java.util.*;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class ScriptQueryGameTests {
    @GameTest(template="port_empty")
    public static void querySnapshots(GameTestHelper helper) throws Exception {
        Path root=Files.createTempDirectory("dmz-lua-queries-"),file=root.resolve("progression/runtime.json"),script=root.resolve("scripts/example/queries.lua");
        Files.createDirectories(file.getParent());Files.createDirectories(script.getParent());
        Files.writeString(file,"""
            {meters:{'example:count':{scope:'character',max:1000},'example:actions':{scope:'character'}},
             queries:{'example:ready':{meter:{id:'example:actions',min:1}},'example:gift':{content:'example:gift'}},
             rules:[{event:'example:query',actions:[{meter:'example:actions',operation:'add',value:1}]}],
             scripts:[{id:'example:queries',events:['example:query']}]}
            """);
        Files.writeString(script,"return {onEvent=function(ctx) return {{meter='example:count',operation='add',value=(ctx.flags['example:ready'] and 10 or 1)+(ctx.flags['example:gift'] and 100 or 0)}} end}");
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"ScriptQueries"));
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
        var field=ConfigManager.class.getDeclaredField("runtime");field.setAccessible(true);var before=field.get(null);
        try {
            field.set(null,RuntimeConfig.load(file));
            data.getRuntime().dispatch(data,"example:query",Map.of());
            helper.assertTrue(data.getRuntime().value("example:count")==1,"Lua query observed JSON actions instead of the pre-event snapshot");
            com.dragonminez.api.content.ContentCompletionApi.complete(player,"example:gift","script-test");
            data.getRuntime().dispatch(data,"example:query",Map.of());
            helper.assertTrue(data.getRuntime().value("example:count")==111,"Named queries did not observe committed meter/content changes: "+data.getRuntime().value("example:count"));
            for(String invalid:List.of("{queries:{invalid:{}}}","{queries:{'example:bad':{unknown:true}}}","{queries:{'example:bad':{meter:{id:'example:missing',min:1}}}}")) {
                boolean rejected=false;try{RuntimeConfig.parse(invalid);}catch(RuntimeException ex){rejected=true;}
                helper.assertTrue(rejected,"Invalid script query accepted: "+invalid);
            }
            helper.succeed();
        } finally {field.set(null,before);player.discard();}
    }
}
