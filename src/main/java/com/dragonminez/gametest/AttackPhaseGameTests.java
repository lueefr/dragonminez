package com.dragonminez.gametest;

import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class AttackPhaseGameTests {
    @GameTest(template="port_empty")
    public static void inputPolicies(GameTestHelper helper) throws Exception {
        var config=RuntimeConfig.parse("""
            {"behaviors":{"attacks":{"dmz:melee":{"activeTicks":1,"recoveryTicks":3,
              "restrictions":{"recovery":["ki","strike","melee"]}}}}}
            """);
        var field=ConfigManager.class.getDeclaredField("runtime");field.setAccessible(true);var original=field.get(null);
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"AttackInput"));
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
        try{
            field.set(null,config); data.getStatus().setHasCreatedCharacter(true);
            var target=new net.minecraft.world.entity.decoration.ArmorStand(helper.getLevel(),player.getX(),player.getY(),player.getZ());
            var first=new net.neoforged.neoforge.event.entity.player.AttackEntityEvent(player,target);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(first);
            helper.assertTrue(data.getRuntime().numbers(data).get("attack.active")==1,"Melee event did not enter active phase");
            data.getRuntime().tick(data);
            helper.assertTrue(!data.getRuntime().permits("ki")&&!data.getRuntime().permits("strike"),"Recovery restrictions not applied");
            var second=new net.neoforged.neoforge.event.entity.player.AttackEntityEvent(player,target);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(second);
            helper.assertTrue(second.isCanceled(),"Server accepted restricted melee input");
            for(int i=0;i<3;i++)data.getRuntime().tick(data);
            helper.assertTrue(data.getRuntime().permits("melee")&&data.getRuntime().permits("ki"),"Recovery restrictions leaked into idle");
            for(String invalid:java.util.List.of("{idle:['ki']}","{recovery:['unknown']}")){
                boolean rejected=false;
                try{RuntimeConfig.parse("{behaviors:{attacks:{beam:{restrictions:"+invalid+"}}}}");}catch(IllegalArgumentException ex){rejected=true;}
                helper.assertTrue(rejected,"Invalid restriction accepted");
            }
            helper.succeed();
        }finally{field.set(null,original);player.discard();}
    }
    @GameTest(template = "port_empty")
    public static void attackPhases(GameTestHelper helper) throws Exception {
        var config = RuntimeConfig.parse("""
            {"meters":{"example:releases":{"scope":"character"}},
             "rules":[{"event":"dmz:attack_released","actions":[{"meter":"example:releases","operation":"add","value":{"source":"attack.charge"}}]}],
             "behaviors":{"attacks":{"example:beam":{"activeTicks":2,"recoveryTicks":3}},
             "modifiers":[{"factors":{"PWR":{"add":[1,{"source":"attack.charging"}]}}}]}}
            """);
        var field = ConfigManager.class.getDeclaredField("runtime"); field.setAccessible(true); var original = field.get(null);
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "AttackPhases"));
        var data = StatsProvider.get(StatsCapability.INSTANCE, player).orElseThrow(IllegalStateException::new);
        try {
            field.set(null, config);
            data.getTechniques().startTechniqueCharge("example:beam"); data.getRuntime().tick(data);
            helper.assertTrue(config.behaviors().factor(data, "PWR") == 2, "Charging phase not available to stat modifiers");
            data.getRuntime().releaseAttack(data, "example:beam", 75); data.getTechniques().clearTechniqueCharge();
            helper.assertTrue(data.getRuntime().value("example:releases") == 75, "Release event lost charge snapshot");
            helper.assertTrue(data.getRuntime().view().getDouble("$attack.active") == 1, "Active phase missing from synchronization");
            data.getRuntime().tick(data); data.getRuntime().tick(data);
            helper.assertTrue(data.getRuntime().numbers(data).get("attack.recovery") == 1, "Configured recovery phase not entered");
            for (int index = 0; index < 3; index++) data.getRuntime().tick(data);
            helper.assertTrue(data.getRuntime().numbers(data).get("attack.recovery") == 0, "Recovery phase never ended");
            helper.assertTrue(!data.getRuntime().save().contains("$attack.active"), "Attack phase leaked into persistent meters");
            helper.succeed();
        } finally { field.set(null, original); player.discard(); }
    }
}
