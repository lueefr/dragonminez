package com.dragonminez.gametest;

import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class RuntimeGameTests {
    @GameTest(template="port_empty")
    public static void serverMeters(GameTestHelper helper) throws Exception {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "RuntimeMeters"));
        var data = StatsProvider.get(StatsCapability.INSTANCE, player).orElseThrow(IllegalStateException::new);
        var field = ConfigManager.class.getDeclaredField("runtime"); field.setAccessible(true); var original = field.get(null);
        field.set(null, RuntimeConfig.parse("""
            {"combatTicks":2,"meters":{
              "example:total":{"scope":"character"},"example:spirit":{"scope":"combat"},"example:burst":{"scope":"form"}
            },"rules":[
              {"event":"dmz:damage_received","when":{"content":"example:enabled"},"actions":[
                {"meter":"example:total","operation":"add","value":{"source":"event.damage"}},
                {"meter":"example:spirit","operation":"add","value":{"source":"event.damage"}},
                {"meter":"example:burst","operation":"add","value":1}
              ]}
            ]}
            """));
        try {
            data.getStatus().setHasCreatedCharacter(true);
            var container = new DamageContainer(player.damageSources().generic(), 20); container.setNewDamage(4);
            NeoForge.EVENT_BUS.post(new LivingDamageEvent.Post(player, container));
            helper.assertTrue(data.getRuntime().value("example:total") == 0, "Shared completion condition was ignored");
            com.dragonminez.api.content.ContentCompletionApi.complete(player, "example:enabled", "test");
            NeoForge.EVENT_BUS.post(new LivingDamageEvent.Post(player, container));
            helper.assertTrue(data.getRuntime().value("example:total") == 4, "Meter used original damage instead of committed damage");
            helper.assertTrue(data.getRuntime().value("example:spirit") == 4, "Combat meter did not receive server damage");
            data.getRuntime().tick(data);
            helper.assertTrue(data.getRuntime().value("example:burst") == 1, "First tick discarded form meter earned before the tick");
            data.getCharacter().forceSetActiveForm("runtime", "next");
            helper.assertTrue(data.getRuntime().value("example:burst") == 0 && data.getRuntime().value("example:total") == 4, "Direct form setter did not immediately reset only form-scoped meters");
            NeoForge.EVENT_BUS.post(new LivingDamageEvent.Post(player, container));
            helper.assertTrue(data.getRuntime().value("example:burst") == 1, "Form meter did not accept new events after transition");
            var saved = data.save();
            helper.assertTrue(saved.getCompound("RuntimeMeters").contains("example:total") && !saved.getCompound("RuntimeMeters").contains("example:spirit"), "Temporary meters leaked into persistence");
            var clone = new StatsData(player); clone.copyFrom(data);
            helper.assertTrue(clone.getRuntime().value("example:total") == 8 && clone.getRuntime().value("example:burst") == 0, "Clone retained temporary meters or lost permanent meters");
            var restored = new StatsData(player); restored.load(saved);
            helper.assertTrue(restored.getRuntime().value("example:total") == 8 && restored.getRuntime().value("example:spirit") == 0, "Save/load violated meter scopes");
            var synced = new StatsData(player); synced.load(new StatsSyncS2C(player).groups());
            helper.assertTrue(synced.getRuntime().value("example:spirit") == 8, "Full sync omitted temporary meter view");
            synced.load(new net.minecraft.nbt.CompoundTag());
            helper.assertTrue(synced.getRuntime().value("example:spirit") == 8, "Unrelated partial sync discarded meter state");
            data.getRuntime().tick(data); data.getRuntime().tick(data);
            helper.assertTrue(data.getRuntime().value("example:spirit") == 0 && data.getRuntime().value("example:total") == 8, "Combat exit did not reset only combat-scoped meters");
            var replacement = RuntimeConfig.parse("{\"meters\":{\"example:total\":{\"scope\":\"character\",\"max\":3}}}");
            field.set(null, replacement);
            helper.assertTrue(data.getRuntime().value("example:total") == 3, "Reload did not preserve and clamp saved character meter");
            helper.succeed();
        } finally { field.set(null, original); player.discard(); }
    }
}
