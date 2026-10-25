package com.dragonminez.gametest;

import com.dragonminez.api.content.*;
import com.dragonminez.api.forms.*;
import com.dragonminez.api.rules.*;
import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.network.C2S.UpdateSkillC2S;
import com.dragonminez.common.util.FormRequisites;
import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.NeoForge;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class DataRulesGameTests {
    private static final Gson GSON=new Gson();
    private static java.lang.reflect.Field field(String name){try{var f=ConfigManager.class.getDeclaredField(name);f.setAccessible(true);return f;}catch(Exception ex){throw new IllegalStateException(ex);}}
    private static Object get(String name){try{return field(name).get(null);}catch(Exception ex){throw new IllegalStateException(ex);}}
    private static void set(String name,Object value){try{field(name).set(null,value);}catch(Exception ex){throw new IllegalStateException(ex);}}
    @SuppressWarnings("unchecked") private static <T> Map<String,T> map(String name){return (Map<String,T>)get(name);}
    private static FormConfig group(){return GSON.fromJson("""
        {"groupName":"path","formType":"superforms","forms":{
          "stage":{"name":"stage","unlockOnSkillLevel":2,"maxMastery":100,"editHair":true,
            "visual":{"parts":{"left_forearm":[1.5,1,1.5]},"aura":false,"rules":[
              {"if":{"first":true},"set":{"outline":true}},
              {"if":{"flying":true},"set":{"aura":true}}]}}
        }}
        """,FormConfig.class);}
    private static String installRace(){String race="rules_"+UUID.randomUUID().toString().replace("-","");map("RACE_FORMS").put(race,new HashMap<>(Map.of("path",group())));var character=new RaceCharacterConfig();character.setFormSkillTpCosts("superforms",new Integer[]{-1,-1,-1});map("RACE_CHARACTER").put(race,character);return race;}
    private static void removeRace(String race){map("RACE_FORMS").remove(race);map("RACE_CHARACTER").remove(race);}

    @GameTest(template="port_empty")
    public static void surgeBurstKeepsConfiguredFormOutline(GameTestHelper helper) {
        String race=installRace();
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"BurstColor"));
        player.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new net.minecraft.core.BlockPos(1,1,1))));
        var area=player.getBoundingBox().inflate(30);
        try {
            ConfigManager.getAllFormsForRace(race).put("path",GSON.fromJson("""
                {"groupName":"path","formType":"superforms","forms":{"stage":{"name":"stage",
                  "auraColor":"#FFFFFF","outlineShader":{"enabled":true,"primaryColor":"#00AB34"}}}}
                """,FormConfig.class));
            var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
            data.getCharacter().setRace(race);data.getCharacter().setActiveForm("path","stage");
            com.dragonminez.server.events.players.KiSurgeService.forceActivateSurge(player,data);
            var bursts=helper.getLevel().getEntitiesOfClass(com.dragonminez.common.init.entities.ki.KiExplosionVisualEntity.class,area);
            helper.assertTrue(data.getStatus().isSurgeActive() && bursts.stream().anyMatch(b->b.getColorOutline()==0x00AB34),"Native surge burst replaced the configured outline with white");
            helper.succeed();
        } finally {
            helper.getLevel().getEntitiesOfClass(com.dragonminez.common.init.entities.ki.KiExplosionVisualEntity.class,area).forEach(net.minecraft.world.entity.Entity::discard);
            removeRace(race);player.discard();
        }
    }

    private static void collectTranslationKeys(net.minecraft.network.chat.Component component,Set<String> keys){
        if(component.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translated){
            keys.add(translated.getKey());
            for(Object argument:translated.getArgs())if(argument instanceof net.minecraft.network.chat.Component child)collectTranslationKeys(child,keys);
        }
        for(var child:component.getSiblings())collectTranslationKeys(child,keys);
    }

    @GameTest(template="port_empty")
    public static void purchaseRequirementNamesRemainTranslatable(GameTestHelper helper) {
        String race=installRace();
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"RuleNames"));
        try {
            var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
            data.getCharacter().setRace(race);
            var conditions=JsonParser.parseString("{\"any\":[{\"mastery\":{\"form\":\"path.stage\",\"min\":50}},{\"content\":\"example:trial\"}]}").getAsJsonObject();
            var missing=Conditions.missing(new Conditions.Context(data,null),conditions);
            var keys=new HashSet<String>();collectTranslationKeys(missing.getFirst(),keys);
            helper.assertTrue(keys.contains("rules.dragonminez.mastery"),"Mastery description was flattened");
            helper.assertTrue(keys.contains("race.dragonminez."+race+".form.path.stage"),"Form name lost race/group context");
            helper.assertTrue(keys.contains("example:trial"),"Content name bypassed client translation");
            helper.succeed();
        } finally { removeRace(race);player.discard(); }
    }

    @GameTest(template="port_empty")
    public static void completionRewardsOncePersistAndRollbackAsAChain(GameTestHelper helper)throws Exception{
        String race=installRace();Object previous=get("rulesConfig");var events=new AtomicInteger();
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"RuleRewards"));
        Consumer<Rewards.Applied> listener=e->{if(e.getPlayer()==player)events.incrementAndGet();};NeoForge.EVENT_BUS.addListener(listener);
        try{
            set("rulesConfig",RuleConfig.parse("""
                {"content":{"example:trial":[{"form":"path.stage"},{"mastery":{"form":"path.stage","add":25}},{"content":"example:linked"}],
                "example:linked":[{"tp":12}],"example:bad":[{"tp":7},{"form":"path.missing"}]}}
                """));
            var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);data.getCharacter().setRace(race);data.getResources().setTrainingPointsExact(0);
            ContentCompletionApi.complete(player,"example:trial","ritual");ContentCompletionApi.complete(player,"example:trial","ritual");
            helper.assertTrue(data.getSkills().getSkillLevel("superforms")==2,"Completion failed to permanently grant the form skill level");
            helper.assertTrue(Conditions.mastery(data,"path.stage")==25 && data.getResources().getTrainingPointsExact()==12,"First-only rewards repeated or lost numeric precision");
            helper.assertTrue(data.getContentProgress().hasCompleted("example:linked")&&events.get()==4,"Chained completion or Java reward events missing");
            int before=events.get();boolean failed=false;try{ContentCompletionApi.complete(player,"example:bad","event");}catch(IllegalArgumentException expected){failed=true;}
            helper.assertTrue(failed&&!data.getContentProgress().hasCompleted("example:bad")&&data.getResources().getTrainingPointsExact()==12&&events.get()==before,"Failed reward chain leaked state or committed events");
            var restored=new StatsData(player);restored.load(data.save());helper.assertTrue(restored.getSkills().getSkillLevel("superforms")==2&&restored.getContentProgress().count("example:trial")==2,"Reward progress failed to persist");
        }finally{set("rulesConfig",previous);removeRace(race);NeoForge.EVENT_BUS.unregister(listener);}
        helper.succeed();
    }

    @GameTest(template="port_empty")
    public static void purchasePacketRejectsRequirementsBeforeAnyDebit(GameTestHelper helper){
        Object previous=get("rulesConfig");
        try{
            set("rulesConfig",RuleConfig.parse("""
                {"buy":{"jump":{"1":{"content":"example:purchase_trial","mastery":{"form":"path.stage","min":40}}}}}
                """));
            var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"RulePurchase"));var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);data.getCharacter().setRace("human");data.getResources().setTrainingPointsExact(10000);data.getSkills().registerDefaultSkill("jump",10);
            var packet=new UpdateSkillC2S(UpdateSkillC2S.SkillAction.UPGRADE,"jump",0);var context=new com.dragonminez.compat.network.NetworkEvent.Context(player);
            var lock=FormRequisites.checkSkillLevel(data,"jump",1,false);helper.assertTrue(lock.isLocked()&&FormRequisites.describeInline(lock,false).getString().contains("example:purchase_trial"),"Purchase refusal does not identify missing content");
            packet.handle(()->context);helper.assertTrue(data.getSkills().getSkillLevel("jump")==0&&data.getResources().getTrainingPointsExact()==10000,"Rejected purchase changed skill or TP");
            ContentCompletionApi.complete(player,"example:purchase_trial","test");data.getCharacter().getFormMasteries().addMastery("path","stage",40,100);
            packet.handle(()->context);int cost=ConfigManager.getSkillsConfig().getSkillCosts("jump").getCosts().getFirst();
            helper.assertTrue(data.getSkills().getSkillLevel("jump")==1&&data.getResources().getTrainingPointsExact()==10000-cost,"Satisfied purchase gate did not use server price");
        }finally{set("rulesConfig",previous);}
        helper.succeed();
    }

    @GameTest(template="port_empty")
    public static void firstUseHistoryVisualContextsAndHairPersist(GameTestHelper helper)throws Exception{
        String race=installRace();
        try{
            var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"RuleHistory"));var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);var c=data.getCharacter();c.setRace(race);
            c.setActiveForm("path","stage");c.clearActiveForm();FormUseTracker.update(player,data);helper.assertTrue(!data.getFormHistory().used(race,"path.stage"),"Aborted transformation consumed first use");
            c.setActiveForm("path","stage");FormUseTracker.update(player,data);
            helper.assertTrue(data.getFormHistory().first(race,"path.stage")&&FormVisuals.flag(data,"outline",false),"First activation visual was not selected");
            var hair=new com.dragonminez.common.hair.CustomHair();hair.setName("Personal style");c.setFormHair("path.stage",hair);
            var restored=new StatsData(player);restored.load(data.save());helper.assertTrue(restored.getFormHistory().first(race,"path.stage")&&restored.getCharacter().getFormHair("path.stage").getName().equals("Personal style"),"History or player form hair lost on reload");
            c.clearActiveForm();FormUseTracker.update(player,data);c.setActiveForm("path","stage");FormUseTracker.update(player,data);
            helper.assertTrue(data.getFormHistory().count(race,"path.stage")==2&&!FormVisuals.flag(data,"outline",false),"First-use visuals repeated after reactivation");
            data.getSkills().registerDefaultSkill("fly",10);data.getSkills().setSkillActive("fly",true);player.tickCount++;
            helper.assertTrue(FormVisuals.flag(data,"aura",false),"Flying context did not enable aura");
            helper.assertTrue(FormHair.editable(race).containsKey("path.stage"),"Editable form missing from the data-driven hair editor list");
            var copy=new StatsData(player);copy.copyFrom(data);helper.assertTrue(copy.getFormHistory().used(race,"path.stage")&&copy.getCharacter().getFormHair("path.stage")!=null,"Clone lost persistent form data");
        }finally{removeRace(race);}
        helper.succeed();
    }

    @GameTest(template="port_empty")
    public static void formHairPacketHonorsEditFlag(GameTestHelper helper) {
        String race=installRace();
        try {
            var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"RuleHair"));
            var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);data.getCharacter().setRace(race);
            var hair=new com.dragonminez.common.hair.CustomHair();hair.setName("Player choice");
            var context=new com.dragonminez.compat.network.NetworkEvent.Context(player);
            var packet=new com.dragonminez.common.network.C2S.UpdateCustomHairC2S(race+":path.stage",hair);
            com.dragonminez.common.network.C2S.UpdateCustomHairC2S.handle(packet,()->context);
            helper.assertTrue(data.getCharacter().getFormHair("path.stage")!=null,"Editable form hair packet was rejected");
            var json=GSON.toJsonTree(group()).getAsJsonObject();var form=json.getAsJsonObject("forms").getAsJsonObject("stage");form.addProperty("editHair",false);form.addProperty("hairType","none");
            DataRulesGameTests.<Map<String,FormConfig>>map("RACE_FORMS").get(race).put("path",GSON.fromJson(json,FormConfig.class));
            helper.assertTrue(FormHair.editable(race).isEmpty(),"Fixed hair form was still offered in the editor");
            hair.setName("Rejected choice");com.dragonminez.common.network.PacketRateLimiter.clear(player.getUUID());
            com.dragonminez.common.network.C2S.UpdateCustomHairC2S.handle(new com.dragonminez.common.network.C2S.UpdateCustomHairC2S(race+":path.stage",hair),()->context);
            helper.assertTrue(data.getCharacter().getFormHair("path.stage").getName().equals("Player choice"),"Client overrode a JSON-owned hairstyle");
        } finally {removeRace(race);}
        helper.succeed();
    }

    @GameTest(template="port_empty")
    public static void locateCustomNpcUsesPlacementsWithoutGeneratingChunks(GameTestHelper helper)throws Exception {
        var registry=com.dragonminez.api.npc.CustomNpcRegistry.INSTANCE;
        var root=java.nio.file.Files.createTempDirectory("dmz-locate-npc-");
        com.dragonminez.common.init.entities.questnpc.CustomNpcEntity npc=null;
        try {
            java.nio.file.Files.createDirectories(root.resolve("custom_npcs"));java.nio.file.Files.createDirectories(root.resolve("custom_npc_placements"));
            java.nio.file.Files.writeString(root.resolve("custom_npcs/guide.json"),"{\"id\":\"example:locate_guide\",\"baseTexture\":\"master_base\",\"name\":\"Guide\",\"hairType\":\"none\"}");
            String dimension=helper.getLevel().dimension().location().toString();
            java.nio.file.Files.writeString(root.resolve("custom_npc_placements/guide.json"),"{\"npc\":\"example:locate_guide\",\"dimension\":\""+dimension+"\",\"x\":1000000,\"y\":200,\"z\":1000000}");registry.reload(root);
            var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"RuleLocate"));var source=helper.getLevel().getServer().createCommandSourceStack().withLevel(helper.getLevel()).withPosition(player.position());
            var dispatcher=new com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack>();com.dragonminez.server.commands.LocateCommand.register(dispatcher);
            helper.assertTrue(!helper.getLevel().hasChunk(62500,62500),"Locate fixture was already loaded");
            helper.assertTrue(dispatcher.execute("dmzlocate npc example:locate_guide",source)==1,"Configured custom NPC placement was not locatable");
            helper.assertTrue(!helper.getLevel().hasChunk(62500,62500),"Locate generated a distant NPC chunk");
            var pos=helper.absolutePos(net.minecraft.core.BlockPos.ZERO);npc=com.dragonminez.api.npc.CustomNpcApi.spawn(helper.getLevel(),"example:locate_guide",pos.getX(),pos.getY(),pos.getZ(),0);
            helper.assertTrue(dispatcher.execute("dmzlocate npc example:locate_guide",source)==1,"Loaded custom NPC was not locatable");
            var suggestions=dispatcher.getCompletionSuggestions(dispatcher.parse("dmzlocate npc example:",source)).join().getList();
            helper.assertTrue(suggestions.stream().anyMatch(v->v.getText().equals("example:locate_guide")),"Custom NPC ID autocomplete missing");
        } finally {
            if(npc!=null)npc.discard();registry.reload(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("dragonminez"));
        }
        helper.succeed();
    }

    @GameTest(template="port_empty")
    public static void invalidRulesFailClearlyAndAbsentFileIsEmpty(GameTestHelper helper)throws Exception{
        var file=java.nio.file.Path.of(System.getProperty("java.io.tmpdir"),"dmz-absent-"+UUID.randomUUID()+".json");
        helper.assertTrue(RuleConfig.load(file).rewards("example:none").isEmpty(),"Absent optional config changed default behavior");
        for(String invalid:List.of("{\"content\":{\"example:a\":[{\"content\":\"example:b\"}],\"example:b\":[{\"content\":\"example:a\"}]}}","{\"buy\":{\"jump\":{\"1\":{\"unknown:condition\":true}}}}")){
            boolean rejected=false;try{RuleConfig.parse(invalid);}catch(IllegalArgumentException expected){rejected=true;}helper.assertTrue(rejected,"Invalid rule was accepted: "+invalid);
        }
        boolean rejected=false;try{FormVisuals.validate(JsonParser.parseString("{\"parts\":{\"missing_part\":[1,1,1]}}").getAsJsonObject());}catch(IllegalArgumentException expected){rejected=true;}helper.assertTrue(rejected,"Unknown body part accepted");
        helper.succeed();
    }
}
