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
}
