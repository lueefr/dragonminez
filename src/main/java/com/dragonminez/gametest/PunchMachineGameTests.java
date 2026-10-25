package com.dragonminez.gametest;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.PunchMachineEntity;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class PunchMachineGameTests {
    @GameTest(template = "port_empty")
    public static void survivesDamageAndRestoresInvisiblePermanentProtection(GameTestHelper helper) {
        PunchMachineEntity machine = MainEntities.PUNCH_MACHINE.get().create(helper.getLevel());
        try {
            machine.removeAllEffects();
            machine.hurt(machine.damageSources().generic(), 1000000);
            machine.hurt(machine.damageSources().outOfBorder(), 1000000);
            machine.hurt(machine.damageSources().fellOutOfWorld(), 1000000);
            helper.assertTrue(machine.isAlive() && machine.getHealth() == machine.getMaxHealth(),
                    "Machine lost health to damage with effects removed");
            machine.tick();
            for (var effect : java.util.List.of(MobEffects.DAMAGE_RESISTANCE, MobEffects.REGENERATION)) {
                var instance = machine.getEffect(effect);
                helper.assertTrue(instance != null && instance.getAmplifier() == 254
                        && instance.isInfiniteDuration() && !instance.isVisible() && !instance.showIcon(),
                        "Missing hidden permanent level-255 protection");
            }
            CompoundTag saved = new CompoundTag();
            machine.saveWithoutId(saved);
            PunchMachineEntity loaded = MainEntities.PUNCH_MACHINE.get().create(helper.getLevel());
            try {
                loaded.load(saved);
                loaded.hurt(loaded.damageSources().genericKill(), 1000000);
                helper.assertTrue(loaded.isAlive() && loaded.getHealth() == loaded.getMaxHealth(),
                        "Reloaded machine died to resistance-bypassing damage");
                helper.assertTrue(loaded.getEffect(MobEffects.REGENERATION).isInfiniteDuration(),
                        "Permanent protection did not persist");
            } finally { loaded.discard(); }
            helper.succeed();
        } finally { machine.discard(); }
    }

    @GameTest(template = "port_empty")
    public static void meleeStillReportsDamageAndCrouchingStillPicksUp(GameTestHelper helper) throws Exception {
        PunchMachineEntity machine = MainEntities.PUNCH_MACHINE.get().create(helper.getLevel());
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "MachineTester"));
        try {
            var data = com.dragonminez.common.stats.StatsProvider.get(
                    com.dragonminez.common.stats.StatsCapability.INSTANCE, player).orElseThrow(IllegalStateException::new);
            data.getCharacter().setRace("human");
            data.getStatus().setHasCreatedCharacter(true);
            data.getStats().setStrength(100);
            data.getResources().setPowerRelease(100);
            double trainingBefore = data.getResources().getTrainingPoints();
            machine.hurt(machine.damageSources().playerAttack(player), 1234);
            var field = PunchMachineEntity.class.getDeclaredField("accumulatedDamage");
            field.setAccessible(true);
            helper.assertTrue(field.getDouble(machine) > 0, "Melee damage no longer reaches training readout");
            helper.assertTrue(machine.isAlive() && machine.getHealth() == machine.getMaxHealth(),
                    "Machine lost health to melee");
            helper.assertTrue(data.getResources().getTrainingPoints() > trainingBefore,
                    "Machine no longer grants training points");
            player.setShiftKeyDown(true);
            player.setPose(net.minecraft.world.entity.Pose.CROUCHING);
            machine.hurt(machine.damageSources().playerAttack(player), 1);
            helper.assertTrue(machine.isRemoved(), "Crouching pickup stopped working");
            helper.succeed();
        } finally { machine.discard(); player.discard(); }
    }
}
