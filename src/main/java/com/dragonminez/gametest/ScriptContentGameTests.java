package com.dragonminez.gametest;

import com.dragonminez.common.config.*;
import com.google.gson.*;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
import java.nio.file.*;
import java.util.*;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class ScriptContentGameTests {
    @GameTest(template = "port_empty")
    public static void contentOverlays(GameTestHelper helper) throws Exception {
        Path root = Files.createTempDirectory("dmz-script-content-");
        Path script = root.resolve("scripts/example/content.lua"), runtime = root.resolve("progression/runtime.json");
        Files.createDirectories(script.getParent()); Files.createDirectories(runtime.getParent());
        Files.writeString(runtime, "{scripts:[{id:'example:content'}]}");
        Files.writeString(script, """
            return {content={
              {type='form_group', id='example/forms', race='saiyan', value={
                identity={groupName='example/forms',formType='falseform'},
                forms={focus={name='focus',stats={multipliers={strength=2}},appearance={hair={color='#FF1234'}}}}
              }},
              {type='attribute',id='example:gift',value={base={STR=1.5},when={content='example:gift'},forms={grant={'example/forms.focus'}}}}
            }}
            """);
        String original = Files.readString(script);
        var loaded = RuntimeConfig.load(runtime);
        helper.assertTrue(loaded.content().size() == 2 && !loaded.json().contains("example:gift"), "Lua content was lost or source definitions leaked into runtime sync metadata");
        Map<String,Map<String,FormConfig>> races = new LinkedHashMap<>(); races.put("saiyan", new LinkedHashMap<>());
        var attributes = new AttributeConfig(); var overlay = new ScriptContent();
        var prepared = overlay.prepare(races, Map.of(), attributes, loaded.content());
        helper.assertTrue(races.get("saiyan").isEmpty(), "Preparing an overlay mutated the live registry");
        var group = prepared.races().get("saiyan").get("example/forms");
        helper.assertTrue(group != null && group.getForm("focus") != null && group.getForm("focus").getStrMultiplier() == 2, "Lua form group did not use the form schema");
        var attribute = new Gson().toJsonTree(prepared.attributes()).getAsJsonObject().getAsJsonObject("attributes").getAsJsonObject("example:gift");
        helper.assertTrue(attribute.getAsJsonObject("base").get("STR").getAsDouble() == 1.5, "Lua attribute did not use the attribute schema");
        var restored = prepared.overlay().prepare(prepared.races(), prepared.stacks(), prepared.attributes(), List.of());
        helper.assertTrue(restored.races().get("saiyan").isEmpty() && !new Gson().toJson(restored.attributes()).contains("example:gift"), "Removing scripts retained stale content");
        var owner = new FormConfig(); owner.setGroupName("example/forms");
        var changed = new LinkedHashMap<>(prepared.races().get("saiyan")); changed.put("example/forms", owner);
        var fresh = prepared.overlay().prepare(Map.of("saiyan", changed), Map.of(), new AttributeConfig(), List.of());
        helper.assertTrue(fresh.races().get("saiyan").get("example/forms") == owner, "Removing overlays overwrote a newer owner configuration");
        boolean conflict = false;
        try { new ScriptContent().prepare(prepared.races(), Map.of(), attributes, loaded.content()); } catch (RuntimeException ex) { conflict = true; }
        helper.assertTrue(conflict && prepared.races().get("saiyan").get("example/forms") == group, "Content collision partially changed the live registry");
        var replacement = loaded.content(); replacement.getFirst().addProperty("mode", "replace");
        var replaced = new ScriptContent().prepare(prepared.races(), prepared.stacks(), attributes, replacement);
        helper.assertTrue(replaced.races().get("saiyan").get("example/forms") != group, "Explicit replacement retained the old definition");
        var reverted = replaced.overlay().prepare(replaced.races(), replaced.stacks(), replaced.attributes(), List.of());
        helper.assertTrue(reverted.races().get("saiyan").get("example/forms") == group, "Removing replacement did not restore the original definition");
        var stack = loaded.content().getFirst(); stack.addProperty("type", "stack_group"); stack.remove("race");
        var stacked = new ScriptContent().prepare(races, Map.of(), attributes, List.of(stack));
        helper.assertTrue(stacked.stacks().containsKey("example/forms") && stacked.races().get("saiyan").isEmpty(), "Stack content entered the wrong registry");
        var invalid = loaded.content(); invalid.getFirst().getAsJsonObject("value").getAsJsonObject("forms").getAsJsonObject("focus").add("stats", JsonParser.parseString("{composition:{inherits:'missing.form'}}"));
        boolean rejected = false;
        try { overlay.prepare(races, Map.of(), attributes, invalid); } catch (RuntimeException ex) { rejected = true; }
        helper.assertTrue(rejected && races.get("saiyan").isEmpty(), "Invalid composition graph partially registered Lua content");
        try (var files = Files.list(root.resolve("progression"))) {
            helper.assertTrue(Files.readString(script).equals(original) && files.count() == 1, "Lua registration rewrote owner files");
        }
        installContent(helper, loaded);
        helper.succeed();
    }
    private static void installContent(GameTestHelper helper, RuntimeConfig loaded) throws Exception {
        var races = ConfigManager.class.getDeclaredField("RACE_FORMS"); races.setAccessible(true);
        var stacks = ConfigManager.class.getDeclaredField("STACK_FORMS"); stacks.setAccessible(true);
        var attributes = ConfigManager.class.getDeclaredField("attributes"); attributes.setAccessible(true);
        var overlay = ConfigManager.class.getDeclaredField("overlay"); overlay.setAccessible(true);
        var cache = ConfigManager.class.getDeclaredField("cachedSyncPayloads"); cache.setAccessible(true);
        var current = (Map<String,Map<String,FormConfig>>) races.get(null);
        var shared = (Map<String,FormConfig>) stacks.get(null);
        var before = new LinkedHashMap<>(current); var previous = new LinkedHashMap<>(shared);
        var oldAttributes = attributes.get(null); var oldOverlay = overlay.get(null);
        var install = ConfigManager.class.getDeclaredMethod("installScripts", RuntimeConfig.class); install.setAccessible(true);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "ScriptContent"));
        try {
            install.invoke(null, loaded);
            var data = com.dragonminez.common.stats.StatsProvider.get(com.dragonminez.common.stats.StatsCapability.INSTANCE, player).orElseThrow(IllegalStateException::new);
            data.getCharacter().setRace("saiyan"); data.getStatus().setHasCreatedCharacter(true);
            com.dragonminez.api.content.ContentCompletionApi.complete(player, "example:gift", "script-test");
            helper.assertTrue(ConfigManager.getAttributes().multiplier(data, "STR") == 1.5, "Installed Lua attribute did not affect the character base");
            data.getCharacter().setActiveForm("example/forms", "focus");
            helper.assertTrue(data.getCharacter().getActiveForm().equals("focus"), "Normal form activation did not find installed Lua content");
            var packet = ConfigManager.getSyncPayloads().stream().filter(value -> value.path().equals("races/saiyan/forms/example/forms")).findFirst().orElseThrow();
            var gson = new GsonBuilder().registerTypeAdapterFactory(new com.dragonminez.common.config.schema.ContentAdapters()).create();
            var synced = gson.fromJson(com.dragonminez.common.network.CompressionUtil.decompress(packet.data()), FormConfig.class);
            helper.assertTrue(synced.getForm("focus").getStrMultiplier() == 2, "Lua form did not survive the normal config synchronization payload");
            var invalid = loaded.content(); invalid.getFirst().addProperty("race", "missing");
            var installed = current.get("saiyan").get("example/forms");
            boolean rejected = false;
            try { new ScriptContent().prepare(current, shared, ConfigManager.getAttributes(), invalid); } catch (RuntimeException ex) { rejected = true; }
            helper.assertTrue(rejected && current.get("saiyan").get("example/forms") == installed, "Failed reload removed the previous working Lua form");
            install.invoke(null, RuntimeConfig.empty());
            helper.assertTrue(!current.get("saiyan").containsKey("example/forms"), "ConfigManager retained removed script content");
        } finally {
            current.clear(); current.putAll(before); shared.clear(); shared.putAll(previous);
            attributes.set(null, oldAttributes); overlay.set(null, oldOverlay); cache.set(null, null); player.discard();
        }
    }

}
