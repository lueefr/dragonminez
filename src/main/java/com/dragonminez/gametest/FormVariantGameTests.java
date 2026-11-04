package com.dragonminez.gametest;

import com.dragonminez.api.forms.*;
import com.dragonminez.api.content.ContentCompletionApi;
import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.util.TransformationsHelper;
import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class FormVariantGameTests {
    @GameTest(template="port_empty")
    public static void sharedSlots(GameTestHelper helper)throws Exception {
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"FormVariants"));
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
        String race="variant_test";
        var group=new Gson().fromJson("""
                {groupName:'path',formType:'superforms',forms:{
                  one:{name:'one',strMultiplier:2,maxStatsMultiplier:1,unlockOnSkillLevel:1,allowFreeTransformOnMastery:0},
                  variant:{name:'variant',strMultiplier:3,inheritsMultipliersFrom:'path.one',maxStatsMultiplier:1,unlockOnSkillLevel:1,allowFreeTransformOnMastery:0,hairColor:'#FF88CC',use:{content:'example:permit'}},
                  two:{name:'two',strMultiplier:4,maxStatsMultiplier:1,unlockOnSkillLevel:1,allowFreeTransformOnMastery:0}}}
                """,FormConfig.class);
        var field=ConfigManager.class.getDeclaredField("RACE_FORMS");field.setAccessible(true);
        @SuppressWarnings("unchecked")var forms=(Map<String,Map<String,FormConfig>>)field.get(null);
        var attributes=ConfigManager.class.getDeclaredField("attributes");attributes.setAccessible(true);var previous=attributes.get(null);
        forms.put(race,new LinkedHashMap<>(Map.of("path",group)));
        attributes.set(null,AttributeConfig.parse("""
                {"attributes":{"example:variant":{"when":{"content":"example:variant"},"forms":{"replace":{"path.one":"path.variant"},"block":["path.two"],"grant":["path.one"]}}}}
                """));
        try {
            var character=data.getCharacter();character.setRace(race);data.getStatus().setHasCreatedCharacter(true);
            data.getSkills().registerDefaultSkill("superforms",10);data.getSkills().setSkillLevel("superforms",1);
            character.setSelectedFormGroup("path");character.setSelectedForm("one");
            character.getFormMasteries().setMastery("path","one",70,100);
            helper.assertTrue(FormVariants.resolve(data,"path","one").form().equals("one"),"Inactive variant changed the form");
            ContentCompletionApi.complete(player,"example:variant","test");
            helper.assertTrue(!FormAccess.missing(data,"path","one",false).isEmpty(),"Replacement requirements were not checked");
            character.setActiveForm("path","one");helper.assertTrue(!character.hasActiveForm(),"Setter bypassed the replacement use condition");
            character.forceSetActiveForm("path","one");
            helper.assertTrue(character.slotForm().equals("one")&&character.getActiveForm().equals("variant"),"Replacement lost its logical slot or effective identity");
            helper.assertTrue(character.getActiveFormData().getHairColor().equals("#FF88CC"),"Replacement appearance was not resolved");
            helper.assertTrue(data.getFormMultiplier("STR")==6,"Replacement composition did not inherit the original form");
            helper.assertTrue(character.getFormMasteries().getMastery("path","variant")==70,"Replacement did not share original mastery");
            character.getFormMasteries().addMastery("path","variant",5,100);
            helper.assertTrue(character.getFormMasteries().getMastery("path","one")==75,"Replacement mastery writes diverged");
            helper.assertTrue(!TransformationsHelper.getSelectableFormNames(data,race,"path").contains("variant"),"Replacement appeared as a duplicate entry");
            helper.assertTrue(TransformationsHelper.getNextAvailableForm(data).getName().equals("two"),"Replacement changed progression order");
            character.setActiveForm("path","two");helper.assertTrue(character.getActiveForm().equals("variant"),"Attribute block was bypassed");
            ContentCompletionApi.complete(player,"example:permit","test");character.setActiveForm("path","variant");
            helper.assertTrue(character.slotForm().equals("one"),"Direct target activation did not retain the original slot");
            var restored=new StatsData(player);restored.load(data.save());var clone=new StatsData(player);clone.copyFrom(restored);
            helper.assertTrue(clone.getCharacter().getActiveForm().equals("variant")&&clone.getCharacter().getFormMasteries().getMastery("path","variant")==75,"Variant or mastery did not survive reconnect/cloning");
            data.getSkills().setSkillLevel("superforms",0);character.clearActiveForm();
            helper.assertTrue(TransformationsHelper.getSelectableFormNames(data,race,"path").contains("one"),"Attribute grant did not unlock its slot");
            helper.assertTrue(TransformationsHelper.getGroupWithFirstAvailableForm(data).equals("path"),"Granted form had no default group");
            var other=new Gson().fromJson("{groupName:'other',formType:'superforms',forms:{alternate:{name:'alternate',strMultiplier:3,inheritsMultipliersFrom:'path.one',maxStatsMultiplier:1,unlockOnSkillLevel:1,energyDrain:0.7}}}",FormConfig.class);
            forms.get(race).put("other",other);
            attributes.set(null,AttributeConfig.parse("{attributes:{'example:cross':{when:{content:'example:variant'},forms:{replace:{'path.one':'other.alternate'},grant:['path.one']}}}}"));
            var crossed=new StatsData(player);crossed.copyFrom(data);crossed.getCharacter().setSelectedFormGroup("path");crossed.getCharacter().setSelectedForm("one");
            helper.assertTrue(TransformationsHelper.getNextAvailableForm(crossed).getEnergyDrain()==0.7,"Cross-group variant did not use target costs");
            crossed.getCharacter().forceSetActiveForm("other","alternate");
            helper.assertTrue(crossed.getCharacter().slotGroup().equals("path")&&crossed.getCharacter().getActiveFormGroup().equals("other"),"Cross-group variant lost its slot or effective owner");
            helper.assertTrue(crossed.getFormMultiplier("STR")==6&&crossed.getCharacter().getFormMasteries().getMastery("other","alternate")==75,"Cross-group stats/mastery diverged");
            helper.succeed();
        } finally {attributes.set(null,previous);forms.remove(race);player.discard();}
    }
    @GameTest(template="port_empty")
    public static void stackSlots(GameTestHelper helper)throws Exception {
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"StackVariants"));
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
        var field=ConfigManager.class.getDeclaredField("STACK_FORMS");field.setAccessible(true);
        @SuppressWarnings("unchecked")var stacks=(Map<String,FormConfig>)field.get(null);
        var attributes=ConfigManager.class.getDeclaredField("attributes");attributes.setAccessible(true);var previous=attributes.get(null);
        var group=new Gson().fromJson("{groupName:'variant_stack',formType:'kaioken',forms:{one:{name:'one',strMultiplier:2,maxStatsMultiplier:1},variant:{name:'variant',strMultiplier:3,maxStatsMultiplier:1,inheritsMultipliersFrom:'variant_stack.one'},two:{name:'two',maxStatsMultiplier:1}}}",FormConfig.class);
        stacks.put("variant_stack",group);
        attributes.set(null,AttributeConfig.parse("{attributes:{'example:stack':{forms:{replace:{'variant_stack.one':'variant_stack.variant'},grant:['variant_stack.one','variant_stack.two']}}}}"));
        try {
            var character=data.getCharacter();data.getStatus().setHasCreatedCharacter(true);
            character.setActiveStackFormGroup("variant_stack");character.setActiveStackForm("variant");
            helper.assertTrue(character.stackForm().equals("one")&&character.getActiveStackForm().equals("variant"),"Script stack setter lost its canonical slot");
            helper.assertTrue(data.getStackFormMultiplier("STR")==6,"Stack replacement composition was incorrect");
            helper.assertTrue(TransformationsHelper.getNextAvailableStackForm(data).getName().equals("two"),"Stack replacement duplicated a progression step");
            helper.assertTrue(!TransformationsHelper.getSelectableStackFormNames(data,"variant_stack").contains("variant"),"Stack radial exposed the target twice");
            helper.succeed();
        } finally {attributes.set(null,previous);stacks.remove("variant_stack");player.discard();}
    }

}
