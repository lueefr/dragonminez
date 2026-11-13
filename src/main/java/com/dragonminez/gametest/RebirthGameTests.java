package com.dragonminez.gametest;

import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class RebirthGameTests {
    @GameTest(template="port_empty")
    public static void cumulativeAwards(GameTestHelper helper)throws Exception {
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"RebirthBudget"));
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
        var field=ConfigManager.class.getDeclaredField("rebirth");field.setAccessible(true);var original=field.get(null);
        var config=new RebirthConfig();field.set(null,config);
        try {
            data.getStatus().setHasCreatedCharacter(true);
            data.getResources().setTrainingPointsExact(0);
            data.getRebirth().record(100);data.getRebirth().begin(config,200000);
            data.getResources().addTrainingPoints(50000,false,true);
            helper.assertTrue(data.getResources().getTrainingPointsExact()==100000,"Initial reincarnation award was not boosted");
            data.getResources().removeTrainingPoints(100000);
            data.getResources().addTrainingPoints(50000,false,true);
            helper.assertTrue(data.getRebirth().earned()==200000,"Spending TP reduced lifetime reincarnation progress");
            data.getResources().addTrainingPoints(10,false,true);
            helper.assertTrue(data.getResources().getTrainingPointsExact()==100010,"Bonus continued after cumulative cap");
            var restored=new StatsData(player);restored.load(data.save());var clone=new StatsData(player);clone.copyFrom(restored);
            helper.assertTrue(clone.getRebirth().earned()==200010&&clone.getRebirth().deathLevel()==100,"Reincarnation ledger did not survive save/load and cloning");
            helper.assertTrue(data.levelCost(100)>data.levelCost(50),"TP budget did not follow the configured level progression");
            helper.succeed();
        } finally {field.set(null,original);player.discard();}
    }
    @GameTest(template="port_empty")
    public static void recreatedBudget(GameTestHelper helper)throws Exception {
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"RebirthCreation"));
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
        var field=ConfigManager.class.getDeclaredField("rebirth");field.setAccessible(true);var original=field.get(null);
        var config=new RebirthConfig();field.set(null,config);
        try {
            data.getStatus().setHasCreatedCharacter(true);data.getCharacter().setRace("human");
            data.getRebirth().record(100);data.getRebirth().prepare();
            data.resetPlayerProgress(player,0,false,false);
            data.getStatus().setHasCreatedCharacter(false);
            var restored=new StatsData(player);restored.load(data.save());
            helper.assertTrue(restored.getRebirth().pending(),"Pending reincarnation was lost on reconnect");
            data.copyFrom(restored);data.getStatus().setHasCreatedCharacter(true);
            var reward=com.google.gson.JsonParser.parseString("{\"id\":\"saiyan\",\"keepStats\":false}");
            com.dragonminez.common.progression.RaceChange.apply(player,data,reward);
            double expected=data.levelCost(100);
            com.dragonminez.common.progression.RebirthService.finish(data);
            helper.assertTrue(!data.getRebirth().pending()&&data.getRebirth().budget()==expected,"Bonus budget was not calculated for the new race and class");
            helper.assertTrue(data.getRebirth().originRace().equals("human")&&data.getCharacter().getRaceName().equals("saiyan"),"Race-change reward lost origins or target race");
            config.bonus.cap.enabled=false;data.getRebirth().prepare();
            com.dragonminez.common.progression.RebirthService.finish(data);
            helper.assertTrue(data.getRebirth().stacks()==1,"Uncapped bonus stacked despite the configured limit");
            config.retention.raceHistory=false;data.getRebirth().prepare();
            com.dragonminez.common.progression.RebirthService.finish(data);
            helper.assertTrue(data.getRebirth().originRace().equals("saiyan")&&data.getRebirth().previousRace().isEmpty(),"Configured race-history reset was ignored");
            helper.succeed();
        } finally {field.set(null,original);player.discard();}
    }

    @GameTest(template="port_empty")
    public static void rebirthInventoryIsConsumedOnlyAfterAcceptance(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "RebirthInventory"));
        var data = StatsProvider.get(StatsCapability.INSTANCE, player).orElseThrow(IllegalStateException::new);
        try {
            player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND));
            player.getInventory().armor.set(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_BOOTS));
            player.getInventory().offhand.set(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHIELD));
            player.inventoryMenu.setCarried(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD));
            helper.assertTrue(!com.dragonminez.common.progression.RebirthService.begin(player, data), "Invalid reincarnation accepted");
            helper.assertTrue(!player.getInventory().getItem(0).isEmpty() && !player.inventoryMenu.getCarried().isEmpty(), "Rejected reincarnation consumed inventory");
            var curios = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).orElseThrow();
            for (var slots : curios.getCurios().values()) {
                if (slots.getStacks().getSlots() > 0) { slots.getStacks().setStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE)); }
                if (slots.getCosmeticStacks().getSlots() > 0) slots.getCosmeticStacks().setStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE));
            }
            com.dragonminez.common.progression.RebirthService.clearInventory(player);
            helper.assertTrue(player.getInventory().isEmpty() && player.inventoryMenu.getCarried().isEmpty(), "Rebirth retained inventory, armor, offhand or cursor");
            for (var slots : curios.getCurios().values()) {
                for (int slot = 0; slot < slots.getStacks().getSlots(); slot++) helper.assertTrue(slots.getStacks().getStackInSlot(slot).isEmpty(), "Equipped Curio retained");
                for (int slot = 0; slot < slots.getCosmeticStacks().getSlots(); slot++) helper.assertTrue(slots.getCosmeticStacks().getStackInSlot(slot).isEmpty(), "Cosmetic Curio retained");
            }
            helper.succeed();
        } finally { player.discard(); }
    }

}
