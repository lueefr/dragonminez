package com.dragonminez.gametest;

import com.dragonminez.common.combat.aim.KiAim;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.api.rules.Conditions;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("dragonminez") @PrefixGameTestTemplate(false)
public final class CombatAimGameTests {
    @GameTest(template="port_vertical_empty")
    public static void cameraAimConvergesFromMuzzleAndLockOverridesRay(GameTestHelper helper) {
        var level=helper.getLevel();var player=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"AimTest"));
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2,10,2))));
        var eye=player.getEyePosition();var origin=eye.add(2,0,-3);var direction=new Vec3(0,0,1);
        var marker=BlockPos.containing(origin.add(0,0,12));var before=level.getBlockState(marker);
        var target=EntityType.ARMOR_STAND.create(level);
        try {
            level.setBlock(marker,Blocks.DIAMOND_BLOCK.defaultBlockState(),2);
            KiAim.accept(player,origin,direction,-1);
            Vec3 hit=KiAim.target(player);helper.assertTrue(BlockPos.containing(hit.add(0,0,.001)).equals(marker),"Server ignored offset camera ray");
            Vec3 muzzle=eye.add(-.35,-.4,0);Vec3 aim=KiAim.direction(player,muzzle);
            helper.assertTrue(aim.dot(hit.subtract(muzzle).normalize())>.999999,"Off-hand muzzle does not converge on crosshair");
            helper.assertTrue(!KiAim.validRay(origin,new Vec3(Double.NaN,0,1),eye)&&!KiAim.validRay(eye.add(65,0,0),direction,eye)&&!KiAim.validRay(origin,Vec3.ZERO,eye),"Invalid or remote camera input accepted");
            var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);data.getSkills().setSkillLevel("kisense",2);
            target.setPos(player.position().add(-3,0,2));level.addFreshEntity(target);
            KiAim.accept(player,origin,direction,target.getId());
            helper.assertTrue(KiAim.target(player).distanceToSqr(target.getBoundingBox().getCenter())<1e-8,"Lock-on did not override camera ray");
            KiAim.accept(player,origin,direction,-1);
            helper.assertTrue(KiAim.target(player).distanceToSqr(hit)<1e-8,"Releasing lock retained stale target");
            var disk=com.dragonminez.common.init.MainEntities.KI_DISC.get().create(level);disk.setOwner(player);disk.setPos(muzzle);disk.setKiSpeed(1);
            disk.fireHability(20);
            helper.assertTrue(disk.getDeltaMovement().normalize().dot(hit.subtract(disk.position()).normalize())>.9999,"Disk ignored camera aim");disk.discard();
            var wave=com.dragonminez.common.init.MainEntities.KI_WAVE.get().create(level);wave.setOwner(player);wave.setupKiHamePlayer(player,1,1,1);wave.fireHability(20);
            helper.assertTrue(Vec3.directionFromRotation(wave.getFixedPitch(),wave.getFixedYaw()).dot(hit.subtract(wave.position()).normalize())>.9999,"Beam ignored camera aim");wave.discard();
            var laser=new com.dragonminez.common.init.entities.ki.KiLaserEntity(level,player);laser.setupKiLaserPlayer(player,1,1,0xffffff,0xff0000,0);laser.fireHability(20);
            helper.assertTrue(Vec3.directionFromRotation(laser.getFixedPitch(),laser.getFixedYaw()).dot(hit.subtract(laser.position()).normalize())>.9999,"Laser retained body rotation instead of camera aim");laser.discard();
            var bytes=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try{var packet=new com.dragonminez.common.network.C2S.KiAimC2S(origin,direction,-1);packet.encode(bytes);var decoded=new com.dragonminez.common.network.C2S.KiAimC2S(bytes);helper.assertTrue(decoded.origin().equals(origin)&&decoded.direction().equals(direction)&&decoded.lockedTarget()==-1,"Aim packet round trip failed");}finally{bytes.release();}
            helper.succeed();
        }finally{level.setBlock(marker,before,2);target.discard();player.discard();}
    }
    @GameTest(template="port_vertical_empty")
    public static void contextualConditionsComposeAndValidateAtLoad(GameTestHelper helper) {
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"Conditions"));
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2,10,2))));
        try {
            var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);player.setHealth(player.getMaxHealth()/2);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND));
            var conditions=JsonParser.parseString("{\"all\":[{\"healthPercent\":{\"min\":49,\"max\":51}},{\"dimension\":\"minecraft:overworld\"},{\"heldItem\":\"minecraft:diamond\"},{\"not\":{\"burning\":true}}]}").getAsJsonObject();
            Conditions.validate(conditions);helper.assertTrue(Conditions.test(new Conditions.Context(data,null),conditions),"New contextual conditions failed composition");
            player.setHealth(player.getMaxHealth());helper.assertTrue(!Conditions.test(new Conditions.Context(data,null),conditions),"Health predicate did not update");
            var moving=JsonParser.parseString("{\"moving\":true}").getAsJsonObject();
            player.setOnGround(true);player.setDeltaMovement(0,-.0784,0);
            helper.assertTrue(!Conditions.test(new Conditions.Context(data,null),moving),"Ground gravity incorrectly counts as movement");
            player.setDeltaMovement(.1,0,0);
            helper.assertTrue(Conditions.test(new Conditions.Context(data,null),moving),"Horizontal movement not detected");
            player.setOnGround(false);player.setDeltaMovement(0,.1,0);
            helper.assertTrue(Conditions.test(new Conditions.Context(data,null),moving),"Airborne vertical movement not detected");
            for(String invalid:new String[]{"{\"energyPercent\":{\"min\":101}}","{\"altitude\":{\"min\":10,\"max\":0}}","{\"timeOfDay\":{}}","{\"effect\":{\"id\":\"minecraft:speed\",\"minAmplifier\":0.5}}"}) {
                boolean rejected=false;try{Conditions.validate(JsonParser.parseString(invalid).getAsJsonObject());}catch(RuntimeException expected){rejected=true;}
                helper.assertTrue(rejected,"Invalid condition accepted: "+invalid);
            }
            helper.succeed();
        }finally{player.discard();}
    }
}
