package com.dragonminez.gametest;

import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.combat.aim.*;
import com.dragonminez.api.content.ContentCompletionApi;
import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class CharacterPropertyGameTests {
    @GameTest(template="port_empty")
    public static void resolvedBase(GameTestHelper helper)throws Exception {
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"ResolvedBase"));
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
        var field=ConfigManager.class.getDeclaredField("attributes");field.setAccessible(true);var original=field.get(null);
        var registry=ConfigManager.class.getDeclaredField("RACE_FORMS");registry.setAccessible(true);
        @SuppressWarnings("unchecked")var forms=(Map<String,Map<String,FormConfig>>)registry.get(null);
        var group=new Gson().fromJson("{groupName:'path',formType:'superforms',forms:{root:{name:'root',strMultiplier:16,maxStatsMultiplier:1},source:{name:'source',strMultiplier:5,multiplierMode:'add',inheritsMultipliersFrom:'path.root',maxStatsMultiplier:1},final:{name:'final',strMultiplier:2,maxStatsMultiplier:1}}}",FormConfig.class);
        forms.put("property_test",new LinkedHashMap<>(Map.of("path",group)));
        field.set(null,AttributeConfig.parse("{attributes:{'example:absorbed':{when:{content:'example:absorbed'},base:{STR:3},composition:{inherits:[{form:'path.source',stats:['STR']}]}}}}"));
        try {
            data.getCharacter().setRace("property_test");data.getStatus().setHasCreatedCharacter(true);
            helper.assertTrue(data.getTotalMultiplier("STR")==1,"Inactive inherited potential changed base");
            ContentCompletionApi.complete(player,"example:absorbed","test");
            helper.assertTrue(data.getTotalMultiplier("STR")==63,"Base did not inherit resolved additive chain (16+5)*3");
            data.getCharacter().setActiveForm("path","final");
            helper.assertTrue(data.getTotalMultiplier("STR")==126,"Transformation did not multiply the inherited base");
            data.getCharacter().getFormMasteries().setMastery("path","root",100,100);
            group.getForm("root").setMaxStatsMultiplier(2d);
            helper.assertTrue(data.getTotalMultiplier("STR")==222,"Inherited base ignored source mastery or reused stale resolved factors");
            var restored=new StatsData(player);restored.load(data.save());
            helper.assertTrue(restored.getTotalMultiplier("STR")==222,"Inherited potential did not survive reconnect");
            helper.succeed();
        } finally {field.set(null,original);forms.remove("property_test");player.discard();}
    }
    @GameTest(template="port_empty")
    public static void detectionRules(GameTestHelper helper)throws Exception {
        var observer=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"PropertyViewer"));
        var target=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"PropertyTarget"));
        var viewer=StatsProvider.get(StatsCapability.INSTANCE,observer).orElseThrow(IllegalStateException::new);
        var data=StatsProvider.get(StatsCapability.INSTANCE,target).orElseThrow(IllegalStateException::new);
        var field=ConfigManager.class.getDeclaredField("attributes");field.setAccessible(true);var original=field.get(null);
        field.set(null,AttributeConfig.parse("""
            {"attributes":{
              "example:signal":{"when":{"content":"example:signal"},"properties":{"example:signature":"example:special"}},
              "example:sight":{"when":{"content":"example:sight"},"properties":{"example:perception":["example:special"],"example:rank":4}},
              "example:derived":{"when":{"property":{"id":"example:rank","min":3}},"properties":{"example:qualified":true}}
            },"detection":[{"channels":["ki_sense","lock_on","instant_transmission","scouter"],"mode":"replace",
              "target":{"property":{"id":"example:signature","equals":"example:special"}},
              "observer":{"property":{"id":"example:perception","contains":"example:special"}}}]}
            """));
        try {
            viewer.getStatus().setHasCreatedCharacter(true);data.getStatus().setHasCreatedCharacter(true);
            data.getResources().setPowerRelease(50);
            helper.assertTrue(KiDetection.allows(viewer,data,"instant_transmission"),"Property rules changed an ordinary target");
            ContentCompletionApi.complete(target,"example:signal","test");
            for(String channel:List.of("ki_sense","lock_on","instant_transmission","scouter"))
                helper.assertTrue(!KiDetection.allows(viewer,data,channel),"Undetectable signature leaked through "+channel);
            ContentCompletionApi.complete(observer,"example:sight","test");
            for(String channel:List.of("ki_sense","lock_on","instant_transmission","scouter"))
                helper.assertTrue(KiDetection.allows(viewer,data,channel),"Configured perception was ignored by "+channel);
            helper.assertTrue(ConfigManager.getAttributes().property(viewer,"example:qualified").getAsBoolean(),"Property-derived condition failed");
            observer.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1,1,1)));target.setPos(observer.position().add(1,0,0));helper.getLevel().addFreshEntity(target);
            viewer.getSkills().registerDefaultSkill("kisense",10);viewer.getSkills().setSkillLevel("kisense",1);
            helper.assertTrue(KiAim.validLock(observer,target.getId())==target,"Server rejected permitted property-based lock-on");
            viewer.getContentProgress().load(new net.minecraft.nbt.CompoundTag());
            helper.assertTrue(KiAim.validLock(observer,target.getId())==null,"Server accepted lock-on without the required perception");
            helper.succeed();
        } finally {field.set(null,original);observer.discard();target.discard();}
    }
}
