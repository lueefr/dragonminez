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

    @GameTest(template="port_vertical_empty")
    public static void movementLimitsPersistClampAndRoundTrip(GameTestHelper helper) {
        var resources = new com.dragonminez.common.stats.character.Resources();
        resources.load(new net.minecraft.nbt.CompoundTag());
        helper.assertTrue(resources.getSprintLimit() == 100 && resources.getJumpLimit() == 100, "Legacy movement defaults changed");
        resources.setSprintLimit(43); resources.setJumpLimit(Integer.MIN_VALUE);
        var copy = new com.dragonminez.common.stats.character.Resources(); copy.load(resources.save());
        helper.assertTrue(copy.getSprintLimit() == 40 && copy.getJumpLimit() == 5, "Limits were not clamped/persisted");
        copy.setJumpLimit(Integer.MAX_VALUE);
        helper.assertTrue(copy.getJumpLimit() == 100, "Jump limit exceeded maximum");
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            new com.dragonminez.common.network.C2S.SetMovementLimitC2S(true, 45).encode(buffer);
            var packet = new com.dragonminez.common.network.C2S.SetMovementLimitC2S(buffer);
            helper.assertTrue(packet.jump() && packet.value() == 45, "Movement packet lost selection");
        } finally { buffer.release(); }
        double scale = com.dragonminez.common.flight.MovementLimits.jumpScale(25);
        helper.assertTrue(Math.abs(scale * scale - 0.25) < 1e-8, "Jump height percentage uses linear velocity");
        helper.assertTrue(com.dragonminez.common.flight.MovementLimits.jumpHeight(10, 1) > com.dragonminez.common.flight.MovementLimits.jumpHeight(1, 1), "Jump apex ignores skill level");
        helper.succeed();
    }

}
