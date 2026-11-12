package com.dragonminez.gametest;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class KiChargeAnchorGameTests {
    @GameTest(template = "port_vertical_empty")
    public static void overheadChargeStaysAboveMovingCasterAcrossAimAndGrowth(GameTestHelper helper) {
        var owner = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ChargeAnchor"));
        owner.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1))));
        var ball = MainEntities.KI_BLAST.get().create(helper.getLevel());
        try {
            ball.setupKiGenkiPlayer(owner, 1, .6f, 8);
            ball.tick();
            double height = ball.getY() - AbstractKiProjectile.ownerCastCenterY(owner);
            double base = height - ball.getSize() * .5;
            for (float yaw : new float[]{0, 90, 180, -90}) {
                for (float pitch : new float[]{-90, -45, 0, 45, 90}) {
                    owner.setYRot(yaw);
                    owner.setXRot(pitch);
                    ball.tick();
                    helper.assertTrue(Math.abs(ball.getX() - owner.getX()) < 1e-5
                            && Math.abs(ball.getZ() - owner.getZ()) < 1e-5, "Overhead charge followed aim horizontally");
                    helper.assertTrue(Math.abs(ball.getY() - AbstractKiProjectile.ownerCastCenterY(owner) - height) < 1e-5,
                            "Overhead charge changed height with pitch");
                }
            }
            ball.setSize(24);
            owner.setPos(owner.position().add(1, 1, -1));
            owner.getAttribute(Attributes.SCALE).setBaseValue(2);
            owner.refreshDimensions();
            ball.tick();
            helper.assertTrue(Math.abs(ball.getX() - owner.getX()) < 1e-5 && Math.abs(ball.getZ() - owner.getZ()) < 1e-5,
                    "Charge failed to follow caster movement or drifted while growing");
            helper.assertTrue(Math.abs(ball.getY() - AbstractKiProjectile.ownerCastCenterY(owner)
                    - (base * AbstractKiProjectile.ownerScaleOf(owner) + ball.getSize() * .5)) < 1e-5, "Caster scaling was lost");
            helper.succeed();
        } finally {
            ball.discard();
            owner.discard();
        }
    }

    @GameTest(template = "port_vertical_empty")
    public static void handChargeKeepsRearEdgeAndReleasedBeamStillAims(GameTestHelper helper) {
        var owner = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "HandAnchor"));
        owner.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1))));
        var wave = MainEntities.KI_WAVE.get().create(helper.getLevel());
        try {
            wave.setOwner(owner);
            wave.setupKiHamePlayer(owner, 1, .6f, 4);
            wave.setCastSize(1);
            helper.getLevel().addFreshEntity(wave);
            wave.tickCount = 20;
            wave.tick();
            Vec3 original = wave.position();
            for (float pitch : new float[]{-90, -45, 45, 90}) {
                owner.setXRot(pitch);
                wave.tick();
                helper.assertTrue(wave.position().distanceTo(original) < 1e-5, "Held charge moved with view pitch");
            }
            wave.setCastSize(5);
            wave.tick();
            helper.assertTrue(Math.abs(wave.position().distanceTo(original)-6.6) < 1e-4, "Growing charge failed to preserve its rear edge");
            original = wave.position();
            owner.setYRot(90);
            owner.yHeadRot=90;
            wave.tick();
            helper.assertTrue(wave.position().distanceTo(original) < 1e-5, "Held charge moved with the head while the body stayed still");
            owner.yBodyRot=90;
            wave.tick();
            helper.assertTrue(wave.position().distanceTo(original) > .1, "Held charge failed to turn with the caster");
            helper.assertTrue(Math.abs(wave.getY() - original.y) < 1e-5, "Turning changed the hand's height");
            owner.setXRot(-35);
            wave.fireHability(200);
            Vec3 center = new Vec3(owner.getX(), AbstractKiProjectile.ownerCastCenterY(owner), owner.getZ());
            Vec3 look = owner.getLookAngle();
            Vec3 right = look.cross(new Vec3(0,1,0)).normalize();
            Vec3 up = right.cross(look).normalize();
            Vec3 expected = center.add(right.scale(.4 * AbstractKiProjectile.ownerWidthScale(owner)))
                    .add(up.scale(.1 * AbstractKiProjectile.ownerScaleOf(owner)))
                    .add(look.scale(wave.getSize()*1.65));
            helper.assertTrue(wave.isFiring() && wave.position().distanceTo(expected) < 1e-5,
                    "Released beam lost its original aim/clearance calculation");
            helper.succeed();
        } finally {
            wave.discard();
            owner.discard();
        }
    }
}
