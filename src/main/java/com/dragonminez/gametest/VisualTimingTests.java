package com.dragonminez.gametest;

import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.VisualTiming;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class VisualTimingTests {
    @GameTest(template = "port_empty")
    public static void renderIntervals(GameTestHelper helper) {
        for (double physical : new double[]{0, 0.003, 0.009, 0.011, 0.25, 2}) {
            boolean fine = false, coarse = false;
            for (int tick = 0; tick < 60; tick++) {
                fine = VisualTiming.moving(VisualTiming.speed(physical, 1), fine);
                if (tick % 10 == 0) coarse = VisualTiming.moving(VisualTiming.speed(physical * 10, 10), coarse);
                helper.assertTrue(fine == coarse, "Same physical speed selected different poses at 20 and 2 render samples per second");
            }
        }
        helper.assertTrue(!VisualTiming.moving(0.009, false) && VisualTiming.moving(0.009, true),
                "Noise near the movement boundary must not alternate the charge pose");
        helper.assertTrue(!VisualTiming.moving(0.006, true) && VisualTiming.moving(0.011, false),
                "Hysteresis must still allow genuine movement and stopping");
        helper.assertTrue(VisualTiming.speed(Double.NaN, 10) == 0, "Invalid samples must not animate movement");
        helper.succeed();
    }

    @GameTest(template = "port_empty")
    public static void emitterState(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "AuraTiming"));
        var data = new StatsData(player);
        data.getStatus().setAuraActive(true);
        helper.assertTrue(VisualTiming.aura(data), "Active aura must remain eligible without a render timestamp");
        data.getStatus().setAuraActive(false);
        helper.assertTrue(!VisualTiming.aura(data), "Cached layers must not emit after aura state becomes inactive");
        data.getStatus().setPermanentAura(true);
        helper.assertTrue(VisualTiming.aura(data), "Permanent aura must use the same eligibility rule");
        data.getStatus().setPermanentAura(false);
        helper.assertTrue(!VisualTiming.aura(data) && !VisualTiming.aura(null), "Missing or disabled state must not reuse stale emission");
        helper.succeed();
    }
}
