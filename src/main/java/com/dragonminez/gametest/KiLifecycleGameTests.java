package com.dragonminez.gametest;

import com.dragonminez.common.combat.logic.player.KiChargeTracker;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import com.dragonminez.common.stats.techniques.TechniqueDispatcher;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class KiLifecycleGameTests {
    @GameTest(template = "port_vertical_empty")
    public static void distantChargeReleasesOnceAndNeverRespawns(GameTestHelper helper) {
        var owner = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "DistantCharge"));
        owner.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1))));
        var wave = MainEntities.KI_WAVE.get().create(helper.getLevel());
        var data = new StatsData(owner);
        var technique = (KiAttackData) PredefinedTechniques.copyOf("kamehameha");
        try {
            wave.setOwner(owner);
            wave.setupKiHamePlayer(owner, 1, .6f, 4);
            wave.setTechniqueId(technique.getId());
            wave.setPos(owner.position().add(0, 150, 0));
            helper.getLevel().addFreshEntity(wave);
            helper.assertTrue(KiChargeTracker.charging(owner).contains(wave), "Charge index lost an overcharged distant attack");
            helper.assertTrue(TechniqueDispatcher.executeKiAttack(owner, helper.getLevel(), technique, data, 1), "Distant charge could not release");
            helper.assertTrue(wave.isFiring(), "Distant wave was not fired");
            helper.assertTrue(KiChargeTracker.charging(owner).isEmpty(), "Fired wave remained a charging entity");
            helper.assertTrue(!TechniqueDispatcher.executeKiAttack(owner, helper.getLevel(), technique, data, 1), "Release without a charge recreated an attack");
            wave.discard();
            helper.assertTrue(KiChargeTracker.charging(owner).isEmpty(), "Removed wave remained indexed");
            helper.succeed();
        } finally {
            wave.discard();
            owner.discard();
        }
    }

    @GameTest(template = "port_vertical_empty")
    public static void humanCasterCannotRetainOozaruMouthAttack(GameTestHelper helper) {
        var owner = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "MouthLifecycle"));
        var wave = MainEntities.KI_WAVE.get().create(helper.getLevel());
        try {
            wave.setOwner(owner);
            wave.setTechniqueId("mouth_blast");
            helper.assertTrue(!KiChargeTracker.validate(wave), "Oozaru mouth attack survived a human caster");
            wave.setTechniqueId("kamehameha");
            helper.assertTrue(KiChargeTracker.validate(wave), "Ordinary attack was incorrectly form-restricted");
            helper.succeed();
        } finally {
            wave.discard();
            owner.discard();
        }
    }
    @GameTest(template = "port_vertical_empty")
    public static void revertingOozaruDiscardsExistingAttack(GameTestHelper helper) {
        var owner = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "OozaruRevert"));
        var stats = com.dragonminez.common.stats.StatsProvider.get(
                com.dragonminez.common.stats.StatsCapability.INSTANCE, owner).orElseThrow(IllegalStateException::new);
        var mouth = MainEntities.KI_WAVE.get().create(helper.getLevel());
        try {
            stats.getCharacter().setRace("saiyan");
            stats.getCharacter().forceSetActiveForm("", com.dragonminez.common.util.lists.SaiyanForms.OOZARU);
            helper.assertTrue(stats.getCharacter().isOozaruCached(), "Fixture failed to activate Oozaru");
            mouth.setOwner(owner); mouth.setTechniqueId("mouth_blast");
            helper.getLevel().addFreshEntity(mouth);
            helper.assertTrue(KiChargeTracker.validate(mouth), "A valid Oozaru mouth attack was rejected");
            stats.getCharacter().forceSetActiveForm("", "");
            mouth.tick();
            helper.assertTrue(mouth.isRemoved(), "A live mouth attack survived the first human-form tick");
            helper.assertTrue(!KiChargeTracker.owned(owner).contains(mouth), "Discarded mouth remained in the release index");
            var scream = MainEntities.KI_WAVE.get().create(helper.getLevel());
            scream.setOwner(owner); scream.setTechniqueId("rage_scream");
            scream.tick();
            helper.assertTrue(scream.isRemoved(), "Residual rage scream survived after Oozaru ended");
        } finally { mouth.discard(); owner.discard(); }
        helper.succeed();
    }

    @GameTest(template = "port_vertical_empty")
    @SuppressWarnings("unchecked")
    public static void revertingOozaruEndsTimedScream(GameTestHelper helper) throws Exception {
        var owner = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "TimedScream"));
        var stats = com.dragonminez.common.stats.StatsProvider.get(
                com.dragonminez.common.stats.StatsCapability.INSTANCE, owner).orElseThrow(IllegalStateException::new);
        var handler = com.dragonminez.server.events.players.combat.EvasionAttackHandler.class;
        var field = handler.getDeclaredField("ACTIVE"); field.setAccessible(true);
        var active = (java.util.Map<UUID, ?>) field.get(null);
        var playersField = net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");
        playersField.setAccessible(true);
        var players = (java.util.Map<UUID, net.minecraft.server.level.ServerPlayer>) playersField.get(helper.getLevel().getServer().getPlayerList());
        try {
            stats.getStatus().setHasCreatedCharacter(true);
            stats.getCharacter().setRace("saiyan");
            stats.getCharacter().forceSetActiveForm("", com.dragonminez.common.util.lists.SaiyanForms.OOZARU);
            stats.getSkills().registerDefaultSkill("kicontrol", 10); stats.getSkills().setSkillLevel("kicontrol", 1);
            stats.getResources().setPowerRelease(50);
            owner.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            helper.assertTrue(owner.isCreative(), "Timed scream fixture was not creative");
            helper.assertTrue(stats.getSkills().getSkillLevel("kicontrol") > 0
                    && stats.getResources().getPowerRelease() >= 5, "Timed scream fixture lacks Ki prerequisites");
            var scream = PredefinedTechniques.copyOf("rage_scream");
            stats.getTechniques().getUnlockedTechniques().put("rage_scream", scream);
            stats.getTechniques().getEquippedSlots()[0] = "rage_scream";
            com.dragonminez.server.events.players.combat.EvasionAttackHandler.cast(owner, "rage_scream");
            helper.assertTrue(active.containsKey(owner.getUUID()), "Actual timed Rage Scream failed to start");
            players.put(owner.getUUID(), owner);
            stats.getCharacter().forceSetActiveForm("", "");
            com.dragonminez.server.events.players.combat.EvasionAttackHandler.onServerTick(null);
            helper.assertTrue(!active.containsKey(owner.getUUID()), "Actual timed Rage Scream survived Oozaru reversion");
            helper.assertTrue(stats.getStatus().getEvasionLockTicks() == 0, "Rage Scream left its movement lock active");
        } finally {
            active.remove(owner.getUUID()); players.remove(owner.getUUID()); owner.discard();
        }
        helper.succeed();
    }

}
