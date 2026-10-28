package com.dragonminez.gametest;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class UpstreamIntegrationGameTests {
    @GameTest(template="port_vertical_empty")
    public static void defenseCurveIsContinuousAndNeverExceedsConfiguredCap(GameTestHelper helper) {
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"DefenseMerge"));
        try {
            var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
            double cap=ConfigManager.getCombatConfig().getBaseDamageReductionCap();
            double previous=0;
            for(double defense:new double[]{0,.01,1,10,100,1000,10000,100000,1e9}) {
                double reduction=data.getBaseDamageReduction(defense);
                helper.assertTrue(Double.isFinite(reduction)&&reduction>=previous&&reduction<=cap,"Defense curve is non-finite, non-monotonic or above cap");
                previous=reduction;
            }
            helper.assertTrue(Math.abs(previous-cap)<1e-8,"High defense never reaches configured cap");
            helper.assertTrue(data.getBaseDamageReduction(-10)<0,"Negative defense no longer increases incoming damage");
            helper.succeed();
        } finally {player.discard();}
    }

    @GameTest(template="port_vertical_empty")
    public static void legacyFlightLimitAndResourceCapsRemainValid(GameTestHelper helper) {
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"ResourcesMerge"));
        try {
            var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
            var resources=data.getResources();
            var saved=resources.save();saved.putInt("FlightSpeed",0);resources.load(saved);
            helper.assertTrue(resources.getFlightSpeedLimit()==100,"Legacy unlimited flight was restored as zero speed");
            resources.setFlightSpeedLimit(500);helper.assertTrue(resources.getFlightSpeedLimit()==100,"Flight limit escaped upper bound");
            resources.setCurrentEnergy(Float.MAX_VALUE);resources.setCurrentStamina(Float.MAX_VALUE);resources.setCurrentPoise(Float.MAX_VALUE);
            helper.assertTrue(resources.getCurrentEnergy()<=data.getMaxEnergy()&&resources.getCurrentStamina()<=data.getMaxStamina()&&resources.getCurrentPoise()<=data.getMaxPoise(),"Rounded resources exceeded their maxima");
            helper.succeed();
        } finally {player.discard();}
    }
    @GameTest(template="port_vertical_empty")
    public static void everyBootstrapRadarHasAnUncorruptedGeneratedDefinition(GameTestHelper helper) {
        var ids=new java.util.HashSet<String>();
        var resources=helper.getLevel().getServer().getResourceManager().listResources("dragonballs",id->id.getPath().endsWith("/definitions/radar.json"));
        for(var resource:resources.values()) {
            try(var reader=resource.openAsReader()) {ids.add(com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().get("id").getAsString());}
            catch(java.io.IOException failure){throw new IllegalStateException(failure);}
        }
        for(var radar:com.dragonminez.common.dragonball.DragonBallDefinitions.getBootstrapRadars())
            helper.assertTrue(ids.contains(radar.getId()),"Missing generated radar definition: "+radar.getId());
        helper.succeed();
    }

}
