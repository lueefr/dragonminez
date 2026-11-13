package com.dragonminez.gametest;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.TechniqueConfig;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainGameRules;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class KiBaselineGameTests {
    @GameTest(template = "port_vertical_empty")
    public static void sphereBaseline(GameTestHelper helper) throws Exception {
        var owner = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "SphereBaseline"));
        var source = MainEntities.KI_BLAST.get().create(helper.getLevel());
        var remote = MainEntities.KI_BLAST.get().create(helper.getLevel());
        var restored = MainEntities.KI_BLAST.get().create(helper.getLevel());
        var position = KiBlastEntity.class.getDeclaredMethod("updatePositionRelativeToOwner", net.minecraft.world.entity.LivingEntity.class);
        position.setAccessible(true);
        try {
            source.setupKiGenkiPlayer(owner, 1, .6F, 2);
            source.setSize(20);
            position.invoke(source, owner);
            remote.setOwner(owner);
            remote.getEntityData().assignValues(source.getEntityData().packDirty());
            position.invoke(remote, owner);
            helper.assertTrue(source.position().distanceTo(remote.position()) < .00001,
                    "A remote overcharged sphere reconstructed a different initial edge");
            restored.load(source.saveWithoutId(new CompoundTag()));
            restored.setOwner(owner);
            position.invoke(restored, owner);
            helper.assertTrue(source.position().distanceTo(restored.position()) < .00001,
                    "Reload lost the initial sphere edge or bounding-box lift");
            source.setSize(40); remote.setSize(40); restored.setSize(40);
            position.invoke(source, owner); position.invoke(remote, owner); position.invoke(restored, owner);
            helper.assertTrue(source.position().distanceTo(remote.position()) < .00001
                            && source.position().distanceTo(restored.position()) < .00001,
                    "Further growth diverged after synchronization/reload");
        } finally { source.discard(); remote.discard(); restored.discard(); owner.discard(); }
        helper.succeed();
    }

    @GameTest(template = "port_vertical_empty")
    public static void chargeReload(GameTestHelper helper) throws Exception {
        var owner = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ChargeReload"));
        var source = MainEntities.KI_WAVE.get().create(helper.getLevel());
        var restored = MainEntities.KI_WAVE.get().create(helper.getLevel());
        var config = ConfigManager.getTechniqueConfig();
        var cap = TechniqueConfig.class.getDeclaredField("maxOverchargePercent"); cap.setAccessible(true);
        var scale = TechniqueConfig.class.getDeclaredField("scaleOvercharge"); scale.setAccessible(true);
        Object oldCap = cap.get(config), oldScale = scale.get(config);
        boolean oldRule = helper.getLevel().getGameRules().getBoolean(MainGameRules.OVERCHARGE_GROWTH);
        var stats = StatsProvider.get(StatsCapability.INSTANCE, owner).orElseThrow(IllegalStateException::new);
        try {
            cap.set(config, 1000); scale.set(config, true);
            helper.getLevel().getGameRules().getRule(MainGameRules.OVERCHARGE_GROWTH).set(true, helper.getLevel().getServer());
            source.setOwner(owner); source.setTechniqueId("reload_test");
            source.setSize(2); source.setCastSize(.5F);
            stats.getTechniques().startTechniqueCharge("reload_test");
            stats.getTechniques().setTechniqueChargePercent(200);
            source.growCharge();
            helper.assertTrue(source.getSize() == 4 && source.getCastSize() == 1, "Initial 200% charge did not scale");
            restored.load(source.saveWithoutId(new CompoundTag())); restored.setOwner(owner);
            restored.growCharge();
            helper.assertTrue(restored.getSize() == 4 && restored.getCastSize() == 1,
                    "Reload applied overcharge a second time");
            stats.getTechniques().setTechniqueChargePercent(1000);
            source.growCharge(); restored.growCharge();
            helper.assertTrue(restored.getSize() == 20 && restored.getCastSize() == 5
                            && restored.getSize() == source.getSize(), "1000% growth lost its original base");
            scale.set(config, false);
            restored.growCharge();
            helper.assertTrue(restored.getSize() == 2 && restored.getCastSize() == .5F,
                    "Server scaleOvercharge=false ignored the authored base");
            scale.set(config, true);
            source.setTechniqueId("kamehameha");
            source.setSize(2); source.setCastSize(.5F);
            stats.getTechniques().startTechniqueCharge("kamehameha");
            stats.getTechniques().setTechniqueChargePercent(1000);
            source.growCharge();
            helper.assertTrue(source.getSize() == 2 && source.getCastSize() == .5F,
                    "Kamehameha incorrectly inherited unbounded visual growth");
            stats.getTechniques().startTechniqueCharge("reload_test");
            stats.getTechniques().setTechniqueChargePercent(1000);
            helper.getLevel().getGameRules().getRule(MainGameRules.OVERCHARGE_GROWTH).set(false, helper.getLevel().getServer());
            restored.growCharge();
            helper.assertTrue(restored.getSize() == 2 && restored.getCastSize() == .5F,
                    "Growth gamerule did not restore the authored base");
        } finally {
            cap.set(config, oldCap); scale.set(config, oldScale);
            helper.getLevel().getGameRules().getRule(MainGameRules.OVERCHARGE_GROWTH).set(oldRule, helper.getLevel().getServer());
            stats.getTechniques().clearTechniqueCharge(); source.discard(); restored.discard(); owner.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "port_vertical_empty")
    public static void heatDomeBoundary(GameTestHelper helper) {
        var owner = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "HeatBoundary"));
        var beam = MainEntities.KI_WAVE.get().create(helper.getLevel());
        try {
            beam.setOwner(owner); beam.setTechniqueId("heat_dome");
            beam.setupHeatDomePlayer(owner, 1, .6F, 20);
            beam.fireHability(200);
            helper.assertTrue(beam.planeNormal() != null && beam.planeNormal().y > .99999,
                    "Heat Dome bypassed the upward emission boundary");
            helper.assertTrue(owner.position().subtract(beam.planeOrigin()).dot(beam.planeNormal()) < 0,
                    "Heat Dome can carve the ground beneath its caster");
            helper.assertTrue(beam.getY() - beam.getSize() * 3.2F >= beam.planeOrigin().y - .00001,
                    "The enlarged Heat Dome muzzle overlaps the caster below its emission edge");
        } finally { beam.discard(); owner.discard(); }
        helper.succeed();
    }

    @GameTest(template = "port_vertical_empty")
    public static void terrainBoundary(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new net.minecraft.core.BlockPos(2, 3, 2));
        var behind = origin.offset(0, -1, -1);
        var front = origin.offset(0, -1, 2);
        var rule = level.getGameRules().getRule(MainGameRules.ALLOW_KI_GRIEFING_MOBS);
        boolean old = rule.get();
        try {
            rule.set(true, level.getServer());
            level.setBlock(behind, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(front, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            com.dragonminez.common.combat.util.KiTerrainDestruction.carve(level, origin, 0, 4, 3,
                    null, Vec3.atCenterOf(origin), new Vec3(0, 0, 1));
            helper.assertTrue(level.getBlockState(behind).is(net.minecraft.world.level.block.Blocks.STONE),
                    "The emitted attack carved the ground behind its emission boundary");
            helper.assertTrue(level.getBlockState(front).isAir(),
                    "The emission boundary incorrectly disabled valid terrain destruction ahead");
        } finally {
            rule.set(old, level.getServer());
            level.setBlock(behind, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(front, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
        helper.succeed();
    }

    @GameTest(template = "port_vertical_empty")
    public static void firedSpherePlane(GameTestHelper helper) throws Exception {
        var owner = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "SpherePlane"));
        var sphere = MainEntities.KI_BLAST.get().create(helper.getLevel());
        try {
            owner.setYRot(0); owner.setXRot(0);
            sphere.setupKiGenkiPlayer(owner, 1, .6F, 20);
            sphere.fireHability(200);
            Vec3 direction = sphere.getDeltaMovement().normalize();
            helper.assertTrue(sphere.planeNormal() != null && sphere.planeNormal().dot(direction) > .99999,
                    "Fired overhead sphere retained its vertical charging plane");
            Vec3 center = sphere.position().add(0, sphere.getSize() * .5, 0);
            helper.assertTrue(center.subtract(sphere.planeOrigin()).dot(direction) >= sphere.getSize() * .5 - .00001,
                    "The fired sphere's rear edge overlaps the caster emission boundary");
            helper.assertTrue(owner.getBoundingBox().getCenter().subtract(sphere.planeOrigin()).dot(direction) < 0,
                    "The caster lies in the terrain destruction half-space");
            var saved = sphere.saveWithoutId(new CompoundTag());
            var restored = MainEntities.KI_BLAST.get().create(helper.getLevel());
            restored.load(saved);
            helper.assertTrue(restored.planeOrigin().distanceTo(sphere.planeOrigin()) < .00001
                            && restored.planeNormal().dot(direction) > .99999,
                    "Firing emission plane did not survive save/load");
            restored.discard();
        } finally { sphere.discard(); owner.discard(); }
        helper.succeed();
    }
    @GameTest(template = "port_vertical_empty")
    public static void predefinedChargeAndFire(GameTestHelper helper) {
        var owner = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "AllKiAnchors"));
        var stats = StatsProvider.get(StatsCapability.INSTANCE, owner).orElseThrow(IllegalStateException::new);
        var target = net.minecraft.world.entity.EntityType.ZOMBIE.create(helper.getLevel());
        try {
            owner.setPos(helper.absoluteVec(new Vec3(2, 30, 2)));
            stats.getCharacter().setRace("saiyan");
            stats.getCharacter().forceSetActiveForm("", com.dragonminez.common.util.lists.SaiyanForms.OOZARU);
            stats.getSkills().registerDefaultSkill("kisense", 10); stats.getSkills().setSkillLevel("kisense", 5);
            target.setPos(owner.position().add(0, 0, 12)); helper.getLevel().addFreshEntity(target);
            int checked = 0;
            for (var definition : com.dragonminez.common.stats.techniques.PredefinedTechniques.REGISTRY.values()) {
                if (!(definition instanceof com.dragonminez.common.stats.techniques.KiAttackData)) continue;
                var technique = (com.dragonminez.common.stats.techniques.KiAttackData)
                        com.dragonminez.common.stats.techniques.PredefinedTechniques.copyOf(definition.getId());
                stats.getTechniques().setHomingTargetId(target.getId());
                stats.getTechniques().startTechniqueCharge(technique.getId());
                stats.getTechniques().setTechniqueChargePercent(1000);
                helper.assertTrue(com.dragonminez.common.stats.techniques.TechniqueDispatcher.executeKiAttack(
                        owner, helper.getLevel(), technique, stats, 0), "Native charge failed: " + technique.getId());
                var powers = com.dragonminez.common.combat.logic.player.KiChargeTracker.owned(owner);
                helper.assertTrue(!powers.isEmpty(), "Native charge created no entity: " + technique.getId());
                for (var power : powers) {
                    power.setBlockDestructionEnabled(false);
                    power.growCharge();
                    helper.assertTrue(Double.isFinite(power.getX()) && Double.isFinite(power.getY())
                            && Double.isFinite(power.getZ()) && Float.isFinite(power.getSize()),
                            "Non-finite native charging attachment: " + technique.getId());
                }
                helper.assertTrue(com.dragonminez.common.stats.techniques.TechniqueDispatcher.executeKiAttack(
                        owner, helper.getLevel(), technique, stats, 10), "Native release failed: " + technique.getId());
                for (var power : powers) {
                    helper.assertTrue(Double.isFinite(power.getX()) && Double.isFinite(power.getY())
                            && Double.isFinite(power.getZ()) && power.getMaxLife() > 0,
                            "Invalid native fired attachment/lifetime: " + technique.getId());
                    power.discard();
                }
                stats.getTechniques().clearTechniqueCharge();
                checked++;
            }
            helper.assertTrue(checked >= 20, "Native anchor matrix omitted most predefined Ki techniques");
            com.mojang.logging.LogUtils.getLogger().info("Predefined Ki charge/fire matrix verified {} techniques", checked);
        } finally {
            for (var power : com.dragonminez.common.combat.logic.player.KiChargeTracker.owned(owner)) power.discard();
            stats.getTechniques().clearTechniqueCharge(); target.discard(); owner.discard();
        }
        helper.succeed();
    }

}
