package com.dragonminez.gametest;

import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.nio.file.*;
import java.util.*;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class LuaGameTests {
    @GameTest(template="port_empty")
    public static void scriptDiagnostics(GameTestHelper helper) throws Exception {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "LuaDiagnostics"));
        try {
            var dispatcher = new com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack>();
            com.dragonminez.server.commands.ReloadCommand.register(dispatcher);
            var source = helper.getLevel().getServer().createCommandSourceStack().withSuppressedOutput();
            long generation = ConfigManager.getFormConfigGeneration(); var runtime = ConfigManager.getRuntime();
            helper.assertTrue(dispatcher.execute("dmzscript check", source) == 1, "Admin Lua diagnostics command failed");
            helper.assertTrue(generation == ConfigManager.getFormConfigGeneration() && runtime == ConfigManager.getRuntime(), "Diagnostic command reloaded the server configuration");
            boolean rejected = false;
            try { dispatcher.execute("dmzscript check", player.createCommandSourceStack().withPermission(0).withSuppressedOutput()); } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) { rejected = true; }
            helper.assertTrue(rejected, "Unprivileged player could inspect server scripts");
            helper.succeed();
        } finally { player.discard(); }
    }
    @GameTest(template="port_empty")
    public static void serverScripts(GameTestHelper helper) throws Exception {
        Path directory = Files.createTempDirectory("dmz-lua-test-");
        Path file = directory.resolve("progression/runtime.json"), script = directory.resolve("scripts/example/damage.lua");
        Files.createDirectories(file.getParent()); Files.createDirectories(script.getParent());
        Files.writeString(file, """
            {"meters":{"example:total":{"scope":"character"}},"rules":[{"event":"dmz:damage_received","actions":[{"meter":"example:total","operation":"add","value":1}]}],"scripts":[{"id":"example:damage","events":["dmz:damage_received"]}]}
            """);
        Files.writeString(script, "return {onEvent=function(ctx) return {{meter='example:total',operation='add',value=ctx.values['event.damage']*2}} end}");
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "LuaRuntime"));
        var data = StatsProvider.get(StatsCapability.INSTANCE, player).orElseThrow(IllegalStateException::new);
        var field = ConfigManager.class.getDeclaredField("runtime"); field.setAccessible(true); var original = field.get(null);
        try {
            field.set(null, RuntimeConfig.load(file)); data.getRuntime().dispatch(data, "dmz:damage_received", Map.of("event.damage",4d));
            helper.assertTrue(data.getRuntime().value("example:total") == 9, "Server script did not receive event and emit typed meter command");
            Files.writeString(script, "return {onEvent=function(ctx) return {{meter='example:unknown',operation='set',value=999}} end}");
            field.set(null, RuntimeConfig.load(file)); data.getRuntime().dispatch(data, "dmz:damage_received", Map.of("event.damage",4d));
            helper.assertTrue(data.getRuntime().value("example:total") == 9, "Script failure partially committed JSON event actions");
            Files.writeString(script, "return {"); boolean rejected = false;
            try { RuntimeConfig.load(file); } catch (IllegalArgumentException ex) { rejected = true; }
            helper.assertTrue(rejected && Files.readString(script).equals("return {"), "Invalid script was accepted or overwritten");
            helper.succeed();
        } finally {
            field.set(null, original); player.discard();
            Files.deleteIfExists(script); Files.deleteIfExists(script.getParent()); Files.deleteIfExists(directory.resolve("scripts"));
            Files.deleteIfExists(file); Files.deleteIfExists(file.getParent()); Files.deleteIfExists(directory);
        }
    }
}
