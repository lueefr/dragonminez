package com.dragonminez.api.script;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

/** Shared native vocabulary and generated editor declarations for the controlled Lua API. */
public final class LuaBindings {
    public static final List<String> EVENTS = List.of("dmz:tick", "dmz:form_changed", "dmz:combat_enter", "dmz:combat_exit", "dmz:damage_received", "dmz:damage_dealt", "dmz:evaded", "dmz:blocked", "dmz:countered", "dmz:attack_started", "dmz:attack_released", "dmz:attack_cancelled", "dmz:attack_recovery", "dmz:projectile_destroyed", "dmz:projectile_deflected", "dmz:attack_finished");
    public static final List<String> SOURCES = List.of("state.formTicks", "state.stackTicks", "self.healthPercent", "attack.charging", "attack.active", "attack.recovery", "attack.ticks", "attack.charge", "event.damage", "event.originalDamage", "event.blockedDamage");
    private static final String HEADER = "-- Generated DragonMineZ Lua editor declarations. Do not edit.\n";
    private LuaBindings() {}
    public static String declarations() {
        var text = new StringBuilder(HEADER).append("---@meta\n\n---@alias DMZEvent string\n");
        for (String event : EVENTS) text.append("---| '").append(event).append("'\n");
        text.append("\n---@alias DMZSource string\n");
        for (String source : SOURCES) text.append("---| '").append(source).append("'\n");
        return text.append("""

            ---Pre-event snapshot. Keys meter:<namespace:path> contain declared meters.
            ---Damage sources exist only on damage/block/counter events.
            ---@class DMZContext
            ---@field event DMZEvent
            ---@field values table<DMZSource, number>
            ---@field flags table<string, boolean> Named JSON queries evaluated before event actions.

            ---@class DMZMeter
            ---@field meter string
            ---@field operation 'add'|'set'
            ---@field value number

            ---Uses existing JSON reward schemas; never a direct Java object.
            ---@class DMZReward
            ---@field reward table<string, any>

            ---@class DMZContent
            ---@field type 'form_group'|'stack_group'|'attribute'|'spacepod_destination'
            ---@field id string
            ---@field race? string
            ---@field mode? 'add'|'replace'
            ---@field value table<string, any>

            ---@class DMZModule
            ---@field content? DMZContent[]
            ---@field onEvent? fun(ctx: DMZContext): (DMZMeter|DMZReward)[]?

            ---@type DMZModule
            local module = {}
            return module
            """).toString();
    }
    public static Path write(Path directory) throws IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve("dragonminez.lua");
        if (Files.isSymbolicLink(file)) throw new IOException("Editor declaration path cannot be a symlink");
        if (Files.exists(file) && !Files.readString(file, StandardCharsets.UTF_8).startsWith(HEADER)) throw new IOException("Refusing to overwrite a non-generated Lua file");
        Files.writeString(file, declarations(), StandardCharsets.UTF_8);
        return file;
    }
    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("Expected declaration output directory");
        write(Path.of(args[0]));
    }
}
