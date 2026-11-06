package com.dragonminez.gametest;

import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import java.nio.file.*;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class GameplayActionGameTests {
    @GameTest(template="port_empty")
    public static void scriptGameplay(GameTestHelper helper) throws Exception {
        var config=RuntimeConfig.parse("""
            {"meters":{"example:count":{"scope":"character"}},
             "rules":[{"event":"example:test","actions":[{"meter":"example:count","operation":"add","value":1}]}],
             "gameplay":[{"event":"example:test","actions":[{"resource":{"id":"energy","add":-1}},{"effect":{"id":"minecraft:regeneration","ticks":40}}]}],
             "scripts":[{"id":"example:actions","events":["example:test"]}]}
            """);
        var directory=Files.createTempDirectory("dmz-gameplay-script");Files.createDirectories(directory.resolve("scripts/example"));Files.createDirectories(directory.resolve("progression"));
        var file=directory.resolve("progression/runtime.json");Files.writeString(file,config.json());
        var script=directory.resolve("scripts/example/actions.lua");
        Files.writeString(script,"return {onEvent=function(ctx) return {{reward={resource={id='energy',add=-2}}},{meter='example:count',operation='add',value=2}} end}");
        config=RuntimeConfig.load(file);
        var field=ConfigManager.class.getDeclaredField("runtime");field.setAccessible(true);var original=field.get(null);
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"GameplayActions"));
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
        try{
            field.set(null,config);data.getStatus().setHasCreatedCharacter(true);data.getResources().setCurrentEnergy(data.getMaxEnergy());
            float before=data.getResources().getCurrentEnergy();data.getRuntime().dispatch(data,"example:test",Map.of());
            helper.assertTrue(data.getRuntime().value("example:count")==3,"Mixed script/JSON meter commands diverged");
            helper.assertTrue(Math.abs(data.getResources().getCurrentEnergy()-Math.max(0,before-3))<.001,"Gameplay actions did not share resource operations");
            helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),"Gameplay effect not applied");
            helper.assertTrue(RuntimeConfig.parse(config.json()).active(),"Gameplay metadata lost during config synchronization");
            for(String invalid:List.of("{effect:{id:'minecraft:regeneration',ticks:0}}","{resource:{id:'unknown',add:1}}","{activate:{form:'bad'}}")){
                boolean rejected=false;
                try{RuntimeConfig.parse("{gameplay:[{event:'example:test',actions:["+invalid+"]}]}");}catch(RuntimeException ex){rejected=true;}
                helper.assertTrue(rejected,"Invalid gameplay action accepted");
            }
            Files.writeString(script,"return {onEvent=function(ctx) return {{reward={resource={id='unknown',add=-5}}},{meter='example:count',operation='add',value=20}} end}");
            var invalid=RuntimeConfig.load(file);field.set(null,invalid);
            float energy=data.getResources().getCurrentEnergy();data.getRuntime().dispatch(data,"example:test",Map.of());
            helper.assertTrue(data.getRuntime().value("example:count")==3 && data.getResources().getCurrentEnergy()==energy,"Invalid script partially committed meters or gameplay");
            helper.succeed();
        }finally{field.set(null,original);player.discard();Files.deleteIfExists(script);Files.deleteIfExists(script.getParent());Files.deleteIfExists(script.getParent().getParent());Files.deleteIfExists(file);Files.deleteIfExists(file.getParent());Files.deleteIfExists(directory);}
    }
}
