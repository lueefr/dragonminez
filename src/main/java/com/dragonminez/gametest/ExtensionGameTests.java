package com.dragonminez.gametest;

import com.dragonminez.api.content.*;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.util.FormRequisites;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class ExtensionGameTests {
    @GameTest(template="port_empty")
    public static void lifetimeCompletionEventPersistenceAndJsonGate(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"ContentApiTest"));
        var data = StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
        FormConfig.FormData form = new com.google.gson.Gson().fromJson(
                "{\"name\":\"custom_stage\",\"completedContentRequisites\":[\"example:trial\"]}",FormConfig.FormData.class);
        helper.assertTrue(FormRequisites.check(data,form,false).isLocked(),"JSON content prerequisite was bypassed");
        var observed = new AtomicReference<ContentCompletedEvent>();
        Consumer<ContentCompletedEvent> listener = event -> { if (event.getPlayer()==player) observed.set(event); };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            ContentCompletionApi.complete(player,"example:trial","ritual");
            helper.assertTrue(observed.get()!=null && observed.get().getPlayer()==player && observed.get().isFirstCompletion(),
                    "Native completion event must carry the player and first-completion flag");
            helper.assertTrue(!FormRequisites.check(data,form,false).isLocked(),"Completed content did not unlock the JSON prerequisite");
            ContentCompletionApi.complete(player,"example:trial","ritual");
            helper.assertTrue(data.getContentProgress().count("example:trial")==2,"Repeat completion count is incorrect");
            var saved=data.save();
            var restored=new StatsData(player);restored.load(saved);
            helper.assertTrue(restored.getContentProgress().count("example:trial")==2,"Completion history did not survive save/load");
            var clone=new StatsData(player);clone.copyFrom(restored);
            helper.assertTrue(clone.getContentProgress().hasCompleted("example:trial"),"Completion history did not survive player cloning");
            data.getPlayerQuestData().completeQuest("example:custom_quest");
            helper.assertTrue(ContentCompletionApi.hasCompleted(player,ContentCompletionApi.questContentId("example:custom_quest")),
                    "Native quest completion did not set the lifetime flag");
            var legacy = data.save();
            legacy.remove("CompletedContent");
            var lastEvent = observed.get();
            var migrated = new StatsData(player);
            migrated.load(legacy);
            helper.assertTrue(migrated.getContentProgress().hasCompleted(ContentCompletionApi.questContentId("example:custom_quest")),
                    "Existing quest history was not migrated to lifetime content flags");
            helper.assertTrue(observed.get() == lastEvent,"Loading legacy progress replayed a completion event");
            data.getPlayerQuestData().resetQuest("example:custom_quest");
            helper.assertTrue(ContentCompletionApi.hasCompleted(player,ContentCompletionApi.questContentId("example:custom_quest")),
                    "Resetting a quest erased lifetime completion");
        } catch (ClassNotFoundException ex) { throw new IllegalStateException(ex); }
        finally { NeoForge.EVENT_BUS.unregister(listener); }
        helper.succeed();
    }
    @GameTest(template="port_empty")
    public static void customNpcDefinitionCosmeticsAndPersistence(GameTestHelper helper) {
        var definition = com.dragonminez.api.npc.CustomNpcRegistry.fromJson("""
                {"id":"example:guide","baseTexture":"master_male","hairType":"none",
                 "name":"npc.example.guide","alignment":50,
                 "armor":{"chest":"minecraft:iron_chestplate"},
                 "offers":{"shop":[{"item":"minecraft:apple","count":2,"price":5}]}}
                """);
        var npc = com.dragonminez.common.init.MainEntities.CUSTOM_NPC.get().create(helper.getLevel());
        helper.assertTrue(npc != null,"Custom NPC entity type was not registered");
        npc.setDefinition(definition);
        helper.assertTrue(npc.getName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents name
                && name.getKey().equals(definition.name()),"NPC configured name was replaced by the inherited quest-NPC key");
        helper.assertTrue(npc.cosmeticItem(net.minecraft.world.entity.EquipmentSlot.CHEST)
                .is(net.minecraft.world.item.Items.IRON_CHESTPLATE),"Cosmetic armor was not resolved");
        helper.assertTrue(npc.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).isEmpty(),
                "Cosmetic armor must not become real equipment");
        var saved = new net.minecraft.nbt.CompoundTag();
        npc.addAdditionalSaveData(saved);
        var restored = com.dragonminez.common.init.MainEntities.CUSTOM_NPC.get().create(helper.getLevel());
        restored.readAdditionalSaveData(saved);
        helper.assertTrue(restored.definition().equals(definition),"Custom NPC definition did not survive persistence");
        helper.assertTrue(restored.getNpcId().equals("example:guide"),"Custom NPC identity did not survive persistence");
        helper.succeed();
    }
    @GameTest(template="port_empty")
    public static void customNpcShopServerValidation(GameTestHelper helper) {
        com.dragonminez.common.init.entities.questnpc.CustomNpcEntity npc = null;
        try {
            var root = java.nio.file.Files.createTempDirectory("dmz-npc-shop-test-");
            java.nio.file.Files.createDirectories(root.resolve("custom_npcs"));
            java.nio.file.Files.writeString(root.resolve("custom_npcs/guide.json"), """
                    {"id":"example:shop_guide","baseTexture":"master_male","hairType":"none",
                     "name":"npc.example.shop_guide","alignment":50,
                     "offers":{"shop":[{"item":"minecraft:apple","count":2,"price":5}]}}
                    """);
            var registry = com.dragonminez.api.npc.CustomNpcRegistry.INSTANCE;
            registry.reload(root);
            var player = FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"NpcShopTest"));
            player.setPos(helper.absolutePos(new net.minecraft.core.BlockPos(1,1,1)).getCenter());
            player.getInventory().clearContent();
            var data = StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
            data.getResources().setTrainingPointsExact(20);
            npc = com.dragonminez.api.npc.CustomNpcApi.spawn(helper.getLevel(),"example:shop_guide",
                    player.getX()+1,player.getY(),player.getZ(),0);
            helper.assertTrue(com.dragonminez.api.npc.CustomNpcServices.purchase(player,npc,0),"Valid shop purchase failed");
            helper.assertTrue(data.getResources().getTrainingPointsExact()==15,"Server price was not deducted exactly");
            helper.assertTrue(player.getInventory().countItem(net.minecraft.world.item.Items.APPLE)==2,"Shop item count is incorrect");
            helper.assertTrue(!com.dragonminez.api.npc.CustomNpcServices.purchase(player,npc,99),"Forged offer index was accepted");
            data.getResources().setTrainingPointsExact(4);
            helper.assertTrue(!com.dragonminez.api.npc.CustomNpcServices.purchase(player,npc,0),"Insufficient balance was accepted");
            data.getResources().setTrainingPointsExact(20);
            for (int slot=0;slot<36;slot++) player.getInventory().setItem(slot,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE,64));
            helper.assertTrue(!com.dragonminez.api.npc.CustomNpcServices.purchase(player,npc,0),"Full inventory was accepted");
            helper.assertTrue(data.getResources().getTrainingPointsExact()==20,"Rejected purchase debited the player");
            player.getInventory().clearContent();
            npc.setPos(player.getX()+11,player.getY(),player.getZ());
            helper.assertTrue(!com.dragonminez.api.npc.CustomNpcServices.purchase(player,npc,0),"Out-of-range purchase was accepted");
            npc.setPos(player.getX()+1,player.getY(),player.getZ());
            String original = java.nio.file.Files.readString(root.resolve("custom_npcs/guide.json"));
            java.nio.file.Files.writeString(root.resolve("custom_npcs/guide.json"), original.replace("\"alignment\":50", "\"alignment\":100"));
            registry.reload(root);
            data.getResources().setAlignment(50);
            helper.assertTrue(!com.dragonminez.api.npc.CustomNpcServices.purchase(player,npc,0),"Good-aligned NPC accepted incompatible alignment");
            helper.assertTrue(data.getResources().getTrainingPointsExact()==20,"Alignment rejection charged the player");
            data.getResources().setAlignment(100);
            helper.assertTrue(com.dragonminez.api.npc.CustomNpcServices.purchase(player,npc,0),"Good-aligned NPC rejected compatible alignment");
            java.nio.file.Files.writeString(root.resolve("custom_npcs/guide.json"), original.replace("\"alignment\":50", "\"alignment\":0"));
            registry.reload(root);
            helper.assertTrue(!com.dragonminez.api.npc.CustomNpcServices.purchase(player,npc,0),"Evil-aligned NPC accepted incompatible alignment");
            data.getResources().setAlignment(0);
            helper.assertTrue(com.dragonminez.api.npc.CustomNpcServices.purchase(player,npc,0),"Evil-aligned NPC rejected compatible alignment");
            java.nio.file.Files.writeString(root.resolve("custom_npcs/guide.json"), original.replace("\"price\":5", "\"price\":16777217"));
            registry.reload(root);
            data.getResources().setTrainingPointsExact(16777218);
            helper.assertTrue(com.dragonminez.api.npc.CustomNpcServices.purchase(player,npc,0),"Large-price purchase failed");
            helper.assertTrue(data.getResources().getTrainingPointsExact()==1,"Large shop price lost double precision");
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
        finally {
            if (npc != null) npc.discard();
            try { com.dragonminez.api.npc.CustomNpcRegistry.INSTANCE.reload(
                    net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("dragonminez")); }
            catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
        }
        helper.succeed();
    }
    @GameTest(template="port_empty")
    public static void nativeFormCompositionProgressionReloadAndCurrentAnchor(GameTestHelper helper) {
        String race = "api_test_" + UUID.randomUUID().toString().replace("-", "");
        var root = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("dragonminez/races").resolve(race);
        var forms = root.resolve("forms");
        try {
            java.nio.file.Files.createDirectories(forms);
            String source = """
                    {"configVersion":"2.2.0","groupName":"foundation","formType":"superforms","forms":{
                     "basic":{"name":"basic","unlockOnSkillLevel":1,"strMultiplier":2.0},
                     "stronger":{"name":"stronger","unlockOnSkillLevel":2,"strMultiplier":4.0,
                                 "completedContentRequisites":["example:composition_trial"]}}}
                    """;
            String chain = """
                    {"configVersion":"2.2.0","groupName":"custom_path","formType":"superforms","forms":{
                     "stage_one":{"name":"stage_one","unlockOnSkillLevel":1,"baseForm":"best","strMultiplier":1.1},
                     "stage_two":{"name":"stage_two","unlockOnSkillLevel":1,"inheritsMultipliersFrom":"custom_path.stage_one","strMultiplier":1.5}}}
                    """;
            java.nio.file.Files.writeString(forms.resolve("different_filename.json"), source);
            java.nio.file.Files.writeString(forms.resolve("chain.json"), chain);
            com.dragonminez.common.config.ConfigManager.reloadSpecificConfig("races/"+race+"/forms/different_filename");
            com.dragonminez.common.config.ConfigManager.reloadSpecificConfig("races/"+race+"/forms/chain");
            var player = FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"FormRuntimeTest"));
            var data = StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
            data.getCharacter().setRace(race);
            data.getSkills().registerDefaultSkill("superforms",10);
            data.getSkills().setSkillLevel("superforms",1);
            data.getCharacter().setActiveForm("custom_path","stage_two");
            helper.assertTrue(Math.abs(data.getFormMultiplier("STR")-3.3)<1e-8,"Native chained best multiplier is incorrect");
            data.getSkills().setSkillLevel("superforms",2);
            helper.assertTrue(Math.abs(data.getFormMultiplier("STR")-3.3)<1e-8,"Best selected a content-locked form");
            ContentCompletionApi.complete(player,"example:composition_trial","test");
            helper.assertTrue(Math.abs(data.getFormMultiplier("STR")-6.6)<1e-8,"Completion did not invalidate the player's best-form cache");
            java.nio.file.Files.writeString(forms.resolve("different_filename.json"),source.replace("4.0","6.0"));
            com.dragonminez.common.config.ConfigManager.reloadSpecificConfig("races/"+race+"/forms/different_filename");
            helper.assertTrue(Math.abs(data.getFormMultiplier("STR")-9.9)<1e-8,"Config reload did not propagate through the composition chain");
            java.nio.file.Files.writeString(forms.resolve("chain.json"),chain.replace("\"best\"","\"current\""));
            com.dragonminez.common.config.ConfigManager.reloadSpecificConfig("races/"+race+"/forms/chain");
            data.getCharacter().setActiveForm("foundation","basic");
            data.getCharacter().setActiveForm("custom_path","stage_two");
            helper.assertTrue(Math.abs(data.getFormMultiplier("STR")-3.3)<1e-8,"Current-form chain did not retain its preceding source");
            var restored = new StatsData(player);
            restored.load(data.save());
            helper.assertTrue(Math.abs(restored.getFormMultiplier("STR")-3.3)<1e-8,"Current-form source was lost during persistence");
        } catch (java.io.IOException | ClassNotFoundException ex) { throw new IllegalStateException(ex); }
        finally {
            com.dragonminez.common.config.ConfigManager.getAllForms().remove(race);
            try {
                java.nio.file.Files.deleteIfExists(forms.resolve("different_filename.json"));
                java.nio.file.Files.deleteIfExists(forms.resolve("chain.json"));
                java.nio.file.Files.deleteIfExists(forms);
                java.nio.file.Files.deleteIfExists(root);
            } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
        }
        helper.succeed();
    }
}
