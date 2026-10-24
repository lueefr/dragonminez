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
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
        finally {
            if (npc != null) npc.discard();
            try { com.dragonminez.api.npc.CustomNpcRegistry.INSTANCE.reload(
                    net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("dragonminez")); }
            catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
        }
        helper.succeed();
    }
}
