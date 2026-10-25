package com.dragonminez.gametest;

import com.dragonminez.api.forms.*;
import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.util.FormRequisites;
import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class FormAccessGameTests {
    @GameTest(template="port_empty")
    public static void useConditionsAreIndependentAndGuardNormalAndStackSetters(GameTestHelper helper)throws Exception{
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"FormUseTest"));
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
        var race="use_test";
        var group=new Gson().fromJson("""
            {groupName:'path',formType:'superforms',forms:{
                one:{name:'one'},two:{name:'two'},target:{name:'target',unlockOnSkillLevel:1,
                    use:{all:[{mastery:{form:'path.one',min:100}},{mastery:{form:'path.two',min:100}},
                        {any:[{content:'example:trial'},{content:'example:alternate'}]},
                        {not:{content:'example:blocked'}}]}}}}
            """,FormConfig.class);
        var field=ConfigManager.class.getDeclaredField("RACE_FORMS");field.setAccessible(true);
        @SuppressWarnings("unchecked") var forms=(Map<String,Map<String,FormConfig>>)field.get(null);
        var stackField=ConfigManager.class.getDeclaredField("STACK_FORMS");stackField.setAccessible(true);
        @SuppressWarnings("unchecked") var stacks=(Map<String,FormConfig>)stackField.get(null);
        forms.put(race,new LinkedHashMap<>(Map.of("path",group)));stacks.put("use_test",group);
        try{
            data.getCharacter().setRace(race);
            var wireGson = new GsonBuilder().registerTypeAdapterFactory(
                    new com.dragonminez.common.config.schema.ContentAdapters()).create();
            var wireGroup = wireGson.fromJson(wireGson.toJson(group), FormConfig.class);
            helper.assertTrue(wireGroup.getForm("target").getUse().equals(group.getForm("target").getUse()),
                    "Nested form serialization lost use conditions");
            helper.assertTrue(wireGroup.getForm("one").getVisual() == null,
                    "Unset visual configuration changed during synchronization");
            var qualified=com.dragonminez.common.util.FusionForms.qualify(race,"path")+".target";
            var translated=(net.minecraft.network.chat.contents.TranslatableContents)FormAccess.name(data,qualified).getContents();
            helper.assertTrue(translated.getKey().equals("race.dragonminez."+race+".form.path.target"),"Fusion-qualified form name lost its source race");
            var reward = new com.dragonminez.common.quest.rewards.TransformationReward("path", "target", 0, false);
            var rewardText = (net.minecraft.network.chat.contents.TranslatableContents) reward.getDescription(1.0, data).getContents();
            var rewardName = (net.minecraft.network.chat.Component) rewardText.getArgs()[0];
            helper.assertTrue(((net.minecraft.network.chat.contents.TranslatableContents) rewardName.getContents()).getKey()
                    .equals("race.dragonminez." + race + ".form.path.target"),
                    "Transformation reward lost its player's race translation namespace");
            var fusionContext = new com.dragonminez.api.rules.Conditions.Context(data, qualified);
            var linkedMastery = JsonParser.parseString("{mastery:{form:'path.one',min:100}}").getAsJsonObject();
            data.getCharacter().getFormMasteries().setMastery("path", "one", 100, 100);
            helper.assertTrue(!com.dragonminez.api.rules.Conditions.test(fusionContext, linkedMastery),
                    "Fusion use requirement incorrectly read the leader's unqualified mastery");
            data.getCharacter().getFormMasteries().setMastery(race + "@path", "one", 100, 100);
            helper.assertTrue(com.dragonminez.api.rules.Conditions.test(fusionContext, linkedMastery),
                    "Fusion use requirement ignored qualified mastery");
            data.getCharacter().getFormMasteries().setMastery("path", "one", 0, 100);
            var used = JsonParser.parseString("{used:'path.one'}").getAsJsonObject();
            data.getFormHistory().update("test", race, "path.one");
            helper.assertTrue(!com.dragonminez.api.rules.Conditions.test(fusionContext, used),
                    "Fusion use requirement confused the leader's form history with the partner's");
            data.getFormHistory().update("test", race, race + "@path.one");
            helper.assertTrue(com.dragonminez.api.rules.Conditions.test(fusionContext, used),
                    "Fusion use requirement ignored qualified form history");
            helper.assertTrue(!FormRequisites.checkSkillLevel(data,"superforms",1,false).isLocked(),"Use conditions blocked purchase");
            helper.assertTrue(!FormAccess.missing(data,"path","target",false).isEmpty(),"Missing mastery/content was not described");
            data.getCharacter().setActiveForm("path","target");
            helper.assertTrue(!data.getCharacter().hasActiveForm(),"Ordinary/script setter bypassed use conditions");
            data.getCharacter().setActiveStackForm("use_test","target");
            helper.assertTrue(!data.getCharacter().hasActiveStackForm(),"Stack setter bypassed use conditions");
            data.getCharacter().setActiveFormGroup("path");data.getCharacter().setActiveForm("target");
            data.getCharacter().setActiveStackFormGroup("use_test");data.getCharacter().setActiveStackForm("target");
            helper.assertTrue(!data.getCharacter().hasActiveForm()&&!data.getCharacter().hasActiveStackForm(),"Single-field script setters bypassed use conditions");
            data.getCharacter().forceSetActiveForm("path","target");
            helper.assertTrue(data.getCharacter().hasActiveForm(),"Explicit administrative force stopped working");
            data.getCharacter().clearActiveForm();
            data.getCharacter().getFormMasteries().setMastery("path","one",100,100);
            data.getCharacter().getFormMasteries().setMastery("path","two",100,100);
            com.dragonminez.api.content.ContentCompletionApi.complete(player,"example:trial","test");
            helper.assertTrue(FormAccess.missing(data,"path","target",false).isEmpty(),"Satisfied nested use conditions still locked");
            data.getCharacter().setActiveForm("path","target");
            helper.assertTrue(data.getCharacter().hasActiveForm(),"Valid transformation was denied");
            com.dragonminez.api.content.ContentCompletionApi.complete(player,"example:blocked","test");
            helper.assertTrue(!FormAccess.missing(data,"path","target",false).isEmpty(),"Negated use condition was ignored");
            var additive=group.getForm("one");additive.setMultiplierMode("add");additive.setMaxStatsMultiplier(1d);additive.setStrMultiplier(5d);additive.setDefMultiplier(0d);
            helper.assertTrue(FormCompositionRuntime.isComposed(additive),"Parentless additive mode skipped the resolver");
            var composition=new FormCompositionRuntime();
            helper.assertTrue(composition.multiplier(data,"path",additive,"STR")==6
                    && composition.multiplier(data,"path",additive,"DEF")==1,"Identity-base additive values resolved incorrectly");
            helper.succeed();
        }finally{forms.remove(race);stacks.remove("use_test");player.discard();}
    }
}
