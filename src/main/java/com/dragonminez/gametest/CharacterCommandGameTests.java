package com.dragonminez.gametest;

import com.dragonminez.common.stats.*;
import com.dragonminez.server.commands.CharacterCommand;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class CharacterCommandGameTests {
    @GameTest(template="port_empty")
    public static void resetClearsCharacterAndEquipment(GameTestHelper helper) {
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"ResetCommand"));
        try {
            var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
            data.getStatus().setHasCreatedCharacter(true);
            data.getStats().setStrength(600);
            data.getResources().setTrainingPointsExact(1234);
            player.getInventory().setItem(0,new ItemStack(Items.DIAMOND));
            CharacterCommand.reset(player,data);
            helper.assertTrue(!data.getStatus().isHasCreatedCharacter(),"Reset did not reopen character creation");
            helper.assertTrue(data.getStatus().isAlive(),"Reset left the character dead");
            helper.assertTrue(data.getStats().getStrength()==new StatsData(player).getStats().getStrength(),"Reset retained progression");
            helper.assertTrue(data.getResources().getTrainingPointsExact()==0 && player.getInventory().isEmpty(),"Reset retained points or inventory");
            helper.succeed();
        } finally {player.discard();}
    }
    @GameTest(template="port_empty")
    public static void invalidRacePreservesCharacter(GameTestHelper helper) {
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"RaceCommand"));
        try {
            var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
            data.getCharacter().setRace("human");data.getStats().setStrength(600);
            var saved=data.save();
            helper.assertTrue(!CharacterCommand.race(player,data,"missing_race_93"),"Unregistered race accepted");
            helper.assertTrue(saved.equals(data.save()),"Invalid race changed player state");
            helper.succeed();
        } finally {player.discard();}
    }

    @GameTest(template="port_empty")
    public static void customRacePreservesProgression(GameTestHelper helper) throws Exception {
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"CustomRaceCmd"));
        var racesField=com.dragonminez.common.config.ConfigManager.class.getDeclaredField("LOADED_RACES");racesField.setAccessible(true);
        @SuppressWarnings("unchecked") var races=(java.util.List<String>)racesField.get(null);
        var statsField=com.dragonminez.common.config.ConfigManager.class.getDeclaredField("RACE_STATS");statsField.setAccessible(true);
        @SuppressWarnings("unchecked") var configs=(java.util.Map<String,com.dragonminez.common.config.RaceStatsConfig>)statsField.get(null);
        String race="command_test_race";
        var custom=new com.dragonminez.common.config.RaceStatsConfig();custom.getOrCreateClassStats("warrior");
        races.add(race);configs.put(race,custom);
        try {
            var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
            data.getCharacter().setRace("human");data.getStats().setStrength(600);
            data.getResources().setTrainingPointsExact(1234);
            helper.assertTrue(CharacterCommand.race(player,data,race),"Server-defined race rejected");
            helper.assertTrue(data.getCharacter().getRaceName().equals(race),"Server-defined race not applied");
            helper.assertTrue(data.getStats().getStrength()==600 && data.getResources().getTrainingPointsExact()==1234,"Race change erased progression");
            var restored=new StatsData(player);restored.load(data.save());
            helper.assertTrue(restored.getCharacter().getRaceName().equals(race),"Custom race did not survive persistence");
            helper.succeed();
        } finally {races.remove(race);configs.remove(race);player.discard();}
    }
    @GameTest(template="port_empty")
    public static void releaseCeilingUsesServerConfig(GameTestHelper helper) throws Exception {
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"ReleaseConfig"));
        var gameplay=com.dragonminez.common.config.ConfigManager.getServerConfig().getGameplay();
        var field=gameplay.getClass().getDeclaredField("maxPowerRelease");field.setAccessible(true);var previous=field.get(gameplay);
        try {
            var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
            data.getCharacter().setRace("human");
            field.set(gameplay,115);int original=data.getMaxPowerRelease();
            field.set(gameplay,1000);
            helper.assertTrue(data.getMaxPowerRelease()==(int)Math.floor(original*1000.0/115),"Server release ceiling ignored progression");
            field.set(gameplay,null);helper.assertTrue(gameplay.getMaxPowerRelease()==115,"Missing config did not preserve default");
            field.set(gameplay,-10);helper.assertTrue(gameplay.getMaxPowerRelease()==5,"Invalid ceiling was not bounded");
            helper.succeed();
        } finally {field.set(gameplay,previous);player.discard();}
    }
    @GameTest(template="port_empty")
    public static void commandsRequireOperator(GameTestHelper helper) {
        var dispatcher=new com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack>();
        CharacterCommand.register(dispatcher);
        var source=helper.getLevel().getServer().createCommandSourceStack();
        for(String name:new String[]{"dmzreset","dmzcharacter","dmzrace"}) {
            var command=dispatcher.getRoot().getChild(name);
            helper.assertTrue(command!=null && !command.canUse(source.withPermission(0)) && command.canUse(source.withPermission(2)),"Character command permission mismatch: "+name);
        }
        helper.succeed();
    }
}
