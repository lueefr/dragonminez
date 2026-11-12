package com.dragonminez.gametest;

import com.dragonminez.common.stats.character.BodyShape;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.TechniqueConfig;
import com.dragonminez.common.network.C2S.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class BodyShapeGameTests {
    @GameTest(template="port_empty")
    public static void appearanceRoundtrip(GameTestHelper helper) throws Exception {
        Character character = new Character();
        character.getBodyShape().set(BodyShape.Part.HEIGHT,1.25f);
        character.getBodyShape().set(BodyShape.Part.LEGS,.75f);
        character.getBodyShape().frame(1);
        character.setBoobScale(0);
        var saved=character.save();
        helper.assertTrue(saved.getCompound("BodyShape").getFloat("HEIGHT")==1.25f,"Height missing from save");
        helper.assertTrue(BodyShape.load(saved.getCompound("BodyShape")).frame()==1,"Frame missing from save");
        var copy=character.getBodyShape().copy(); copy.set(BodyShape.Part.HEIGHT,Float.NaN);
        helper.assertTrue(copy.get(BodyShape.Part.HEIGHT)==1 && character.getBodyShape().get(BodyShape.Part.HEIGHT)==1.25f,"Shape copy aliases or accepts NaN");
        for(Class<?> type:new Class<?>[]{StatsSyncC2S.class,CreateCharacterC2S.class,UpdateCharacterC2S.class}) {
            Object packet=type.getConstructor(Character.class).newInstance(character);
            FriendlyByteBuf buffer=new FriendlyByteBuf(Unpooled.buffer());
            try {
                type.getMethod("encode",type,FriendlyByteBuf.class).invoke(null,packet,buffer);
                Object decoded=type.getMethod("decode",FriendlyByteBuf.class).invoke(null,buffer);
                var field=type.getDeclaredField("shape"); field.setAccessible(true);
                var shape=(BodyShape)field.get(decoded);
                helper.assertTrue(shape.get(BodyShape.Part.HEIGHT)==1.25f && shape.frame()==1,"Appearance packet lost shape: "+type.getSimpleName());
                helper.assertTrue(buffer.readableBytes()==0,"Appearance packet framing mismatch: "+type.getSimpleName());
            } finally {buffer.release();}
        }
        helper.assertTrue(com.dragonminez.common.stats.character.AppearanceValidator.boobScale(0,1)==0,"Server rejected flat bust");
        helper.succeed();
    }
    @GameTest(template="port_empty")
    public static void overchargeCeiling(GameTestHelper helper) throws Exception {
        var config=ConfigManager.getTechniqueConfig();
        var field=TechniqueConfig.class.getDeclaredField("maxOverchargePercent");field.setAccessible(true);
        Object previous=field.get(config);
        try {
            field.set(config,1000);
            var techniques=new com.dragonminez.common.stats.techniques.Techniques();
            techniques.setTechniqueChargePercent(950);
            helper.assertTrue(KiAttackData.maxCharge()==1000 && techniques.getTechniqueChargePercent()==950,"Old charge cap remains active");
            var restored=new com.dragonminez.common.stats.techniques.Techniques();
            restored.load(techniques.save());
            helper.assertTrue(restored.getTechniqueChargePercent()==950,"Charge persistence truncated overcharge");
            techniques.setTechniqueChargePercent(Float.NaN);
            helper.assertTrue(techniques.getTechniqueChargePercent()==0,"NaN charge accepted");
            helper.assertTrue(KiAttackData.costMultiplier(1000)>KiAttackData.costMultiplier(175),"Higher ceiling made overcharge cheaper");
        } finally {field.set(config,previous);}
        helper.succeed();
    }
    @GameTest(template="port_vertical_empty")
    public static void growthTracksCharge(GameTestHelper helper) throws Exception {
        var owner=net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"GrowthCaster"));
        owner.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new net.minecraft.core.BlockPos(1,1,1))));
        var stats=com.dragonminez.common.stats.StatsProvider.get(com.dragonminez.common.stats.StatsCapability.INSTANCE,owner).orElseThrow(IllegalStateException::new);
        var config=ConfigManager.getTechniqueConfig();
        var field=TechniqueConfig.class.getDeclaredField("maxOverchargePercent");field.setAccessible(true);
        Object previous=field.get(config);
        var wave=com.dragonminez.common.init.MainEntities.KI_WAVE.get().create(helper.getLevel());
        try {
            field.set(config,1000);
            wave.setOwner(owner); wave.setupMouthBlastPlayer(owner,1,.6f,0xFFFFFF,0xFF00FF,1);
            wave.setTechniqueId("mouth_blast");
            stats.getTechniques().startTechniqueCharge("mouth_blast");
            stats.getTechniques().setTechniqueChargePercent(100); wave.growCharge();
            float size=wave.getSize(), cast=wave.getCastSize();
            stats.getTechniques().setTechniqueChargePercent(1000); wave.growCharge();
            helper.assertTrue(Math.abs(wave.getSize()-size*10)<.001 && Math.abs(wave.getCastSize()-cast*10)<.001,"Wave growth saturated before configured ceiling");
            wave.growCharge();
            helper.assertTrue(Math.abs(wave.getSize()-size*10)<.001,"Wave size compounded every tick");
            wave.setFiring(true);stats.getTechniques().clearTechniqueCharge();wave.growCharge();
            helper.assertTrue(Math.abs(wave.getSize()-size*10)<.001,"Release lost charged size");
        } finally {field.set(config,previous);wave.discard();owner.discard();}
        helper.succeed();
    }

    @GameTest(template="port_vertical_empty")
    public static void donationsSurviveChargeGrowth(GameTestHelper helper) {
        var owner=net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"DonorCaster"));
        var stats=com.dragonminez.common.stats.StatsProvider.get(com.dragonminez.common.stats.StatsCapability.INSTANCE,owner).orElseThrow(IllegalStateException::new);
        var ball=com.dragonminez.common.init.MainEntities.KI_BLAST.get().create(helper.getLevel());
        try {
            ball.setupKiGenkiPlayer(owner,1,.6f,8);ball.setTechniqueId("genkidama");
            stats.getTechniques().startTechniqueCharge("genkidama");stats.getTechniques().setTechniqueChargePercent(100);
            ball.growCharge();float base=ball.getSize();
            helper.assertTrue(ball.absorbGenkiDonation(.25f),"Donation fixture was rejected");
            float donation=ball.getGenkiDonatedSize();ball.growCharge();
            helper.assertTrue(Math.abs(ball.getSize()-base-donation)<.001,"Tick erased donated Ki growth");
        } finally {ball.discard();owner.discard();}
        helper.succeed();
    }

}
