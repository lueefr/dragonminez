package com.dragonminez.gametest;

import com.dragonminez.common.flight.FlightPose;
import com.dragonminez.common.network.C2S.CreateTechniqueC2S;
import com.dragonminez.common.stats.techniques.KiAttackData.KiType;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class AnimationRulesTests {
    @GameTest(template = "port_empty")
    public static void poseRules(GameTestHelper helper) {
        helper.assertTrue(!FlightPose.fast(0.499, 1) && FlightPose.fast(0.5, 1), "Fast pose must begin at 50% forward speed");
        helper.assertTrue(!FlightPose.fast(-3, 1) && !FlightPose.fast(0, 1), "Backward and lateral motion must keep combat pose");
        helper.assertTrue(FlightPose.fast(0.01, 0.02) && !FlightPose.fast(0.01, 1), "Threshold must scale with the current speed cap");
        helper.assertTrue(!FlightPose.fast(Double.NaN, 1) && !FlightPose.fast(1, 0), "Invalid limits must not activate fast pose");
        helper.assertTrue(CreateTechniqueC2S.allowedPose(KiType.WAVE, "ki.kameha"), "Existing matching energy pose must be accepted");
        helper.assertTrue(!CreateTechniqueC2S.allowedPose(KiType.WAVE, "ki.soul_punisher")
                && !CreateTechniqueC2S.allowedPose(KiType.WAVE, "custom.missing"), "Unknown or mismatched poses must be rejected");
        var flash = com.dragonminez.common.stats.techniques.KiPose.offset("ki.finalflash", 20);
        var galick = com.dragonminez.common.stats.techniques.KiPose.offset("ki.galick", 20);
        var hame = com.dragonminez.common.stats.techniques.KiPose.offset("ki.kameha", 20);
        helper.assertTrue(flash.z == 2.5 && galick.z == -0.3 && hame.x == 0.4,
                "Wave poses must use their native hand attachments instead of the generic origin");
        helper.assertTrue(!com.dragonminez.common.stats.techniques.KiPose.offset("ki.kameha", 2).equals(hame),
                "Kamehameha attachment must follow the existing hand transition");
        helper.assertTrue(com.dragonminez.common.stats.techniques.KiPose.offset("ki.dodonpa", 20).y == -0.1
                && com.dragonminez.common.stats.techniques.KiPose.offset("ki.makkako", 20).x == -0.2
                && com.dragonminez.common.stats.techniques.KiPose.offset("ki.assault_rain", 20).y == 1.3,
                "Laser, beam and barrage casts must keep the selected pose attachment");
        var source = new com.dragonminez.common.init.entities.ki.KiBlastEntity(com.dragonminez.common.init.MainEntities.KI_BLAST.get(), helper.getLevel());
        var remote = new com.dragonminez.common.init.entities.ki.KiBlastEntity(com.dragonminez.common.init.MainEntities.KI_BLAST.get(), helper.getLevel());
        source.castPose("ki.finalflash");
        var updates = source.getEntityData().packDirty();
        helper.assertTrue(updates != null, "Cast pose must produce native entity metadata");
        remote.getEntityData().assignValues(updates);
        helper.assertTrue("ki.finalflash".equals(remote.castPose()), "Remote projectile must receive pose without owner technique data");
        var packet = new CreateTechniqueC2S("Pose test", "WAVE", "DAMAGE", 1, 1, 1, 0, 20, 20, 1, 2, 3, "NONE", "", 0, 0, "ki.kameha");
        var first = new FriendlyByteBuf(Unpooled.buffer());
        var second = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.toBytes(first);
            byte[] before = new byte[first.readableBytes()];
            first.getBytes(0, before);
            new CreateTechniqueC2S(first).toBytes(second);
            byte[] after = new byte[second.readableBytes()];
            second.getBytes(0, after);
            helper.assertTrue(java.util.Arrays.equals(before, after), "Selected pose must survive packet serialization");
        } finally { first.release(); second.release(); }
        helper.succeed();
    }
}
