package com.dragonminez.gametest;

import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.*;
import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class PowerSnapshotGameTests {
    @GameTest(template = "port_empty")
    public static void capturedPower(GameTestHelper helper) throws Exception {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PowerSnapshot"));
        var data = StatsProvider.get(StatsCapability.INSTANCE, player).orElseThrow(IllegalStateException::new);
        var field = ConfigManager.class.getDeclaredField("attributes"); field.setAccessible(true); var original = field.get(null);
        var registry = ConfigManager.class.getDeclaredField("RACE_FORMS"); registry.setAccessible(true);
        @SuppressWarnings("unchecked") var forms = (Map<String, Map<String, FormConfig>>) registry.get(null);
        var group = new Gson().fromJson("{groupName:'path',formType:'superforms',forms:{root:{name:'root',strMultiplier:16,maxStatsMultiplier:2,maxMastery:100},source:{name:'source',strMultiplier:5,multiplierMode:'add',inheritsMultipliersFrom:'path.root',maxStatsMultiplier:1}}}", FormConfig.class);
        forms.put("snapshot_test", new LinkedHashMap<>(Map.of("path", group)));
        String json = """
            {"attributes":{"example:absorbed":{"when":{"content":"example:absorbed"},"base":{"STR":3},
             "composition":{"inherits":[{"id":"example:power","form":"path.source","stats":["STR"],"mode":"snapshot"}]}}}}
            """;
        try {
            boolean rejected = false;
            try { AttributeConfig.parse(json.replace("\"id\":\"example:power\",", "")); } catch (IllegalArgumentException ex) { rejected = true; }
            helper.assertTrue(rejected, "Snapshot source without stable ID was accepted");
            var config = AttributeConfig.parse(json); field.set(null, config);
            data.getCharacter().setRace("snapshot_test"); data.getStatus().setHasCreatedCharacter(true);
            config.capture(data);
            helper.assertTrue(!data.getSnapshots().has("example:absorbed/example:power"), "Inactive attribute captured power");
            var completed = new net.minecraft.nbt.CompoundTag(); completed.putInt("example:absorbed", 1); data.getContentProgress().load(completed);
            config.multiplier(data, "STR");
            helper.assertTrue(!data.getSnapshots().has("example:absorbed/example:power"), "Stat query mutated snapshot state");
            config.capture(data);
            helper.assertTrue(config.multiplier(data, "STR") == 63, "Snapshot did not resolve additive form chain before capture");
            data.getCharacter().getFormMasteries().setMastery("path", "root", 100, 100);
            config.capture(data);
            helper.assertTrue(config.multiplier(data, "STR") == 63, "Once snapshot changed with later mastery");
            var clone = new StatsData(player); clone.copyFrom(data);
            helper.assertTrue(config.multiplier(clone, "STR") == 63, "Clone lost power snapshot");
            var restored = new StatsData(player); restored.load(data.save()); config.capture(restored);
            helper.assertTrue(config.multiplier(restored, "STR") == 63, "Reconnect recaptured power");
            var packet = new com.dragonminez.common.network.S2C.StatsSyncS2C(player);
            helper.assertTrue(packet.groups().contains("PowerSnapshots"), "Snapshot missing from full synchronization");
            var live = AttributeConfig.parse(json.replace("snapshot", "live")); field.set(null, live);
            helper.assertTrue(live.multiplier(data, "STR") == 111, "Live inheritance ignored new source mastery");
            config = AttributeConfig.parse(json.replace("\"mode\":\"snapshot\"", "\"mode\":\"snapshot\",\"capture\":\"activation\"")); field.set(null, config);
            config.capture(data); helper.assertTrue(config.multiplier(data, "STR") == 63, "Reload spuriously recaptured active source");
            data.getContentProgress().load(new net.minecraft.nbt.CompoundTag()); config.capture(data);
            com.dragonminez.api.content.ContentCompletionApi.complete(player, "example:absorbed", "test");
            helper.assertTrue(config.multiplier(data, "STR") == 111, "Activation policy did not recapture source");
            helper.succeed();
        } finally { field.set(null, original); forms.remove("snapshot_test"); player.discard(); }
    }
}
