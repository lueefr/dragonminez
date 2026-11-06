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
public final class FormActionGameTests {
    @GameTest(template="port_empty")
    public static void lifecycleActions(GameTestHelper helper) throws Exception {
        var config=RuntimeConfig.parse("""
            {"behaviors":{"forms":{"path.a":{
              "onEnter":[{"content":"example:entered"}],"onExit":[{"content":"example:exited"}]}}}}
            """);
        var field=ConfigManager.class.getDeclaredField("runtime");field.setAccessible(true);var original=field.get(null);
        var registry=ConfigManager.class.getDeclaredField("RACE_FORMS");registry.setAccessible(true);
        @SuppressWarnings("unchecked") var forms=(Map<String,Map<String,FormConfig>>)registry.get(null);
        var group=new com.google.gson.Gson().fromJson("{groupName:'path',formType:'superforms',forms:{a:{name:'a'},b:{name:'b'}}}",FormConfig.class);
        var previous=forms.put("transition_test",new LinkedHashMap<>(Map.of("path",group)));
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"FormActions"));
        var clone=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"FormActionClone"));
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
        var copied=StatsProvider.get(StatsCapability.INSTANCE,clone).orElseThrow(IllegalStateException::new);
        try{
            field.set(null,config);data.getCharacter().setRace("transition_test");data.getStatus().setHasCreatedCharacter(true);
            data.getCharacter().forceSetActiveForm("path","a");
            helper.assertTrue(data.getContentProgress().count("example:entered")==1,"First activation missed entry action");
            data.getCharacter().forceSetActiveForm("path","a");
            helper.assertTrue(data.getContentProgress().count("example:entered")==1,"Repeated setter duplicated entry action");
            data.getCharacter().forceSetActiveForm("path","b");
            helper.assertTrue(data.getContentProgress().count("example:exited")==1,"Form replacement missed exit action");
            data.getCharacter().forceSetActiveForm("path","a");
            field.set(null,RuntimeConfig.parse(config.json()));data.getRuntime().tick(data);
            helper.assertTrue(data.getContentProgress().count("example:entered")==2,"Config reload repeated entry actions");
            var saved=data.save();data.load(saved);data.getRuntime().tick(data);
            helper.assertTrue(data.getContentProgress().count("example:entered")==2,"Reconnect repeated entry actions");
            copied.load(saved);copied.getRuntime().tick(copied);
            helper.assertTrue(copied.getContentProgress().count("example:entered")==2,"Clone repeated entry actions");
            copied.getCharacter().clearActiveForm();
            helper.assertTrue(copied.getContentProgress().count("example:exited")==2,"Restored form missed its next exit");
            helper.succeed();
        }finally{
            field.set(null,original);if(previous==null)forms.remove("transition_test");else forms.put("transition_test",previous);
            player.discard();clone.discard();
        }
    }
}
