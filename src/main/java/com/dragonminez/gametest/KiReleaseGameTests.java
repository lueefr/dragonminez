package com.dragonminez.gametest;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import com.dragonminez.common.stats.techniques.TechniqueDispatcher;
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
public final class KiReleaseGameTests {
    @GameTest(template = "port_vertical_empty")
    public static void releasedBeam(GameTestHelper helper) {
        var owner = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ReleaseBeam"));
        var other = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "OtherBeam"));
        var stats = StatsProvider.get(StatsCapability.INSTANCE, owner).orElseThrow(IllegalStateException::new);
        var beam = MainEntities.KI_WAVE.get().create(helper.getLevel());
        var foreign = MainEntities.KI_WAVE.get().create(helper.getLevel());
        var copy = MainEntities.KI_WAVE.get().create(helper.getLevel());
        try {
            var technique = (KiAttackData) PredefinedTechniques.copyOf("kamehameha");
            stats.getTechniques().getUnlockedTechniques().put(technique.getId(), technique);
            stats.getTechniques().getEquippedSlots()[0] = technique.getId();
            beam.setOwner(owner); beam.setupKiHamePlayer(owner, 7, .6F, 1);
            beam.setTechniqueId(technique.getId()); beam.fireHability(200);
            helper.getLevel().addFreshEntity(beam);
            foreign.setOwner(other); foreign.setupKiHamePlayer(other, 7, .6F, 1);
            foreign.setTechniqueId(technique.getId()); foreign.fireHability(200);
            helper.getLevel().addFreshEntity(foreign);
            int expiry = beam.getMaxLife(); float damage = beam.getKiDamage();
            helper.assertTrue(!TechniqueDispatcher.releaseSlot(owner, stats, -1)
                    && !TechniqueDispatcher.releaseSlot(owner, stats, 1), "An invalid/unrelated slot released a power");
            helper.assertTrue(TechniqueDispatcher.releaseSlot(owner, stats, 0), "Equipped firing technique could not release");
            helper.assertTrue(beam.isReleased() && !foreign.isReleased(), "Release changed another caster's attack");
            helper.assertTrue(!TechniqueDispatcher.isMovementRestrictedKiAttack(owner, stats)
                    && !TechniqueDispatcher.isActionRestrictedKiAttack(owner, stats), "Released beam still locks its caster");
            helper.assertTrue(beam.getOwner() == owner && beam.getKiDamage() == damage && beam.getMaxLife() == expiry,
                    "Release changed attribution, damage or lifetime");
            Vec3 start = beam.position(), direction = beam.getDeltaMovement().normalize();
            float yaw = beam.getYRot(), pitch = beam.getXRot();
            owner.setPos(owner.position().add(30, 10, 30)); owner.setYRot(yaw + 90); owner.setXRot(50);
            beam.tick();
            helper.assertTrue(!beam.isRemoved() && beam.position().subtract(start).normalize().dot(direction) > .9999
                    && beam.getYRot() == yaw && beam.getXRot() == pitch, "Released beam followed the moving/turning caster");
            copy.load(beam.saveWithoutId(new CompoundTag())); copy.setOwner(owner);
            helper.assertTrue(copy.isReleased() && !copy.isContinuousFollow() && copy.getMaxLife() == expiry,
                    "Release state or expiry did not survive save/load");
            helper.assertTrue(!TechniqueDispatcher.releaseSlot(owner, stats, 0), "Already released power accepted repeated release");
        } finally { beam.discard(); foreign.discard(); copy.discard(); owner.discard(); other.discard(); }
        helper.succeed();
    }

    @GameTest(template = "port_vertical_empty")
    public static void heldPowerCannotRelease(GameTestHelper helper) {
        var sphere = MainEntities.KI_BLAST.get().create(helper.getLevel());
        try {
            helper.assertTrue(!sphere.releaseControl() && !sphere.isReleased(), "A charging attack detached before firing");
            sphere.setFiring(true); sphere.setDeltaMovement(new Vec3(0, 0, .5));
            helper.assertTrue(sphere.releaseControl() && sphere.isDetached() && !sphere.isControllable(),
                    "Fired sphere did not disable player control");
            helper.assertTrue(sphere.getDeltaMovement().equals(new Vec3(0, 0, .5)), "Sphere release changed its existing direction/speed");
        } finally { sphere.discard(); }
        helper.succeed();
    }
    @GameTest(template = "port_vertical_empty")
    public static void releasedVolley(GameTestHelper helper) {
        var owner = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ReleaseVolley"));
        var volley = MainEntities.KI_BLAST.get().create(helper.getLevel());
        try {
            volley.setOwner(owner); volley.setKiRenderType(9); volley.setKiSpeed(.6F);
            volley.setFiring(true); volley.setMaxLife(200);
            volley.setPos(helper.absoluteVec(new Vec3(1, 5, 1)));
            volley.setYRot(0); volley.setXRot(0);
            helper.getLevel().addFreshEntity(volley);
            helper.assertTrue(volley.releaseControl(), "Volley could not release");
            owner.setDeltaMovement(.2, .3, .4); owner.setYRot(90);
            Vec3 movement = owner.getDeltaMovement();
            volley.tick(); volley.tick();
            helper.assertTrue(owner.getDeltaMovement().equals(movement), "Released volley still freezes its caster");
            helper.assertTrue(!volley.isRemoved(), "Released volley was deleted instead of continuing its existing lifetime");
            helper.assertTrue(!TechniqueDispatcher.isMovementRestrictedKiAttack(owner,
                    StatsProvider.get(StatsCapability.INSTANCE, owner).orElseThrow(IllegalStateException::new)),
                    "Volley child projectiles restored the caster movement lock");
            for (var shot : com.dragonminez.common.combat.logic.player.KiChargeTracker.owned(owner))
                if (shot != volley) {
                    helper.assertTrue(shot.isReleased(), "A volley child inherited control after its parent released");
                    shot.discard();
                }
        } finally { volley.discard(); owner.discard(); }
        helper.succeed();
    }

    @GameTest(template = "port_vertical_empty")
    public static void distantReleasePreservesOtherCharge(GameTestHelper helper) {
        var owner = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "DistantRelease"));
        var stats = StatsProvider.get(StatsCapability.INSTANCE, owner).orElseThrow(IllegalStateException::new);
        var sphere = MainEntities.KI_BLAST.get().create(helper.getLevel());
        try {
            var technique = (KiAttackData) PredefinedTechniques.copyOf("kamehameha");
            stats.getTechniques().getUnlockedTechniques().put(technique.getId(), technique);
            stats.getTechniques().getEquippedSlots()[0] = technique.getId();
            sphere.setOwner(owner); sphere.setTechniqueId(technique.getId()); sphere.setFiring(true);
            sphere.setControllable(true); sphere.setParked(true); sphere.setKiSpeed(.6F);
            sphere.setYRot(45); sphere.setXRot(-20); sphere.setDeltaMovement(Vec3.ZERO);
            sphere.setPos(owner.position().add(0, 400, 0));
            helper.getLevel().addFreshEntity(sphere);
            stats.getTechniques().startTechniqueCharge("galick_gun");
            helper.assertTrue(TechniqueDispatcher.releaseSlot(owner, stats, 0), "Distant indexed power could not release");
            helper.assertTrue("galick_gun".equals(stats.getTechniques().getChargingTechniqueId())
                    && stats.getTechniques().isTechniqueCharging(), "Releasing an old projectile cleared another charge");
            helper.assertTrue(sphere.getDeltaMovement().length() > .59
                    && sphere.getDeltaMovement().normalize().dot(Vec3.directionFromRotation(-20, 45)) > .9999,
                    "A parked Sokidan stayed stationary or lost its frozen direction after release");
        } finally { sphere.discard(); owner.discard(); }
        helper.succeed();
    }

}
