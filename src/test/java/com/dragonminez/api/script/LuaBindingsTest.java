package com.dragonminez.api.script;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.luaj.vm2.compiler.LuaC;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class LuaBindingsTest {
    @TempDir Path directory;
    @Test void generatedSyntax() throws Exception {
        String source = LuaBindings.declarations();
        assertNotNull(LuaC.instance.compile(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), "dragonminez.lua"));
        for (String event : LuaBindings.EVENTS) assertTrue(source.contains("'" + event + "'"));
        for (String value : LuaBindings.SOURCES) assertTrue(source.contains("'" + value + "'"));
        assertTrue(source.contains("DMZContent")); assertTrue(source.contains("DMZReward"));
    }
    @Test void generatedOwnership() throws Exception {
        Path file = LuaBindings.write(directory);
        assertEquals(LuaBindings.declarations(), Files.readString(file));
        LuaBindings.write(directory);
        Files.writeString(file, "-- owner content");
        assertThrows(java.io.IOException.class, () -> LuaBindings.write(directory));
        assertEquals("-- owner content", Files.readString(file));
    }
    @Test void symlinkProtection() throws Exception {
        Path target = directory.resolve("owner.lua"); Files.writeString(target, "-- owner content");
        Files.createSymbolicLink(directory.resolve("dragonminez.lua"), target);
        assertThrows(java.io.IOException.class, () -> LuaBindings.write(directory));
        assertEquals("-- owner content", Files.readString(target));
    }
}
