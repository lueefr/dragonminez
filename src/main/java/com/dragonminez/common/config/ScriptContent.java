package com.dragonminez.common.config;

import com.dragonminez.api.rules.Conditions;
import com.dragonminez.common.config.schema.ContentAdapters;
import com.google.gson.*;
import java.util.*;

/** Script overlays are validated in memory and never overwrite owner files. */
public final class ScriptContent {
    private static final Gson GSON = new GsonBuilder().registerTypeAdapterFactory(new ContentAdapters()).create();
    private record Entry(String race, String id, FormConfig original, FormConfig installed) {}
    private final List<Entry> entries = new ArrayList<>();
    private AttributeConfig baseline, installed;
    public record Prepared(Map<String,Map<String,FormConfig>> races, Map<String,FormConfig> stacks, AttributeConfig attributes, ScriptContent overlay) {}
    public Prepared prepare(Map<String,Map<String,FormConfig>> current, Map<String,FormConfig> stacks, AttributeConfig attributes, List<JsonObject> definitions) {
        var races = new LinkedHashMap<String,Map<String,FormConfig>>();
        current.forEach((race, groups) -> races.put(race, new LinkedHashMap<>(groups)));
        var shared = new LinkedHashMap<>(stacks);
        for (var entry : entries) {
            var groups = entry.race() == null ? shared : races.get(entry.race());
            if (groups != null && groups.get(entry.id()) == entry.installed()) {
                if (entry.original() == null) groups.remove(entry.id()); else groups.put(entry.id(), entry.original());
            }
        }
        AttributeConfig base = attributes == installed ? baseline : attributes;
        var source = GSON.toJsonTree(base).getAsJsonObject();
        var values = source.getAsJsonObject("attributes");
        var next = new ScriptContent(); next.baseline = base;
        Set<String> ids = new HashSet<>();
        for (var definition : definitions) {
            if (!Set.of("type", "id", "race", "mode", "value").containsAll(definition.keySet())) throw new IllegalArgumentException("Unknown Lua content field");
            String type = definition.get("type").getAsString(), id = definition.get("id").getAsString();
            String mode = definition.has("mode") ? definition.get("mode").getAsString() : "add";
            if (!Set.of("add", "replace").contains(mode)) throw new IllegalArgumentException("Lua content mode must be add or replace");
            String race = definition.has("race") ? definition.get("race").getAsString() : null;
            if (!ids.add(type + "/" + race + "/" + id)) throw new IllegalArgumentException("Duplicate Lua content definition: " + id);
            var value = definition.getAsJsonObject("value").deepCopy();
            if (type.equals("attribute")) {
                com.dragonminez.api.content.ContentProgress.requireId(id);
                if (race != null) throw new IllegalArgumentException("Attributes do not declare race; use conditions");
                collision(id, mode, values.has(id)); values.add(id, value);
            } else if (Set.of("form_group", "stack_group").contains(type)) {
                Conditions.id(id);
                if (!id.matches("[a-z0-9_/-]+")) throw new IllegalArgumentException("Form group IDs use lowercase letters, numbers, underscore, slash or hyphen");
                if (type.equals("form_group") && (race == null || !races.containsKey(race))) throw new IllegalArgumentException("Unknown Lua form race: " + race);
                if (type.equals("stack_group") && race != null) throw new IllegalArgumentException("Stack groups cannot declare race");
                var groups = race == null ? shared : races.get(race);
                collision(id, mode, groups.containsKey(id));
                var group = GSON.fromJson(value, FormConfig.class);
                if (!group.getGroupName().equals(id) || group.getForms().isEmpty()) throw new IllegalArgumentException("Lua form group needs matching identity.groupName and nonempty forms: " + id);
                group.getForms().forEach((key, form) -> {
                    if (form == null || !key.equals(form.getName())) throw new IllegalArgumentException("Lua form key must match name: " + key);
                    Conditions.form(id + "." + key);
                });
                next.entries.add(new Entry(race, id, groups.get(id), group)); groups.put(id, group);
            } else throw new IllegalArgumentException("Unsupported Lua content type: " + type);
        }
        AttributeConfig resolved = AttributeConfig.parse(GSON.toJson(source));
        races.forEach((race, groups) -> ConfigManager.validateForms(groups, shared));
        if (races.isEmpty()) ConfigManager.validateForms(Map.of(), shared);
        next.installed = resolved;
        return new Prepared(races, shared, resolved, next);
    }
    private static void collision(String id, String mode, boolean exists) {
        if (exists != mode.equals("replace")) throw new IllegalArgumentException("Lua content " + mode + " conflicts with existing definition: " + id);
    }
}
