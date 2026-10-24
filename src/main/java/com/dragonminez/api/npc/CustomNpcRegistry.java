package com.dragonminez.api.npc;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Isolated custom definition registry. Never reads or writes built-in NPC definitions. */
public final class CustomNpcRegistry {
    public static final CustomNpcRegistry INSTANCE = new CustomNpcRegistry();
    private static final Gson GSON = new Gson();
    public record Trade(String item, int count, int price) {
        public Trade { requireId(item); if (count < 1 || count > 64 || price < 0) throw new IllegalArgumentException("Invalid shop trade"); }
    }
    public record Offers(List<String> skills, List<String> quests, List<Trade> shop) {
        public Offers {
            skills = skills == null ? List.of() : skills.stream().map(value -> value.toLowerCase(Locale.ROOT)).toList();
            quests = quests == null ? List.of() : List.copyOf(quests);
            shop = shop == null ? List.of() : List.copyOf(shop);
        }
    }
    public record Definition(String id, String baseTexture, Map<String, String> armor,
                             String hairType, @com.google.gson.annotations.SerializedName(value="forcedHairCode", alternate={"hairCode"}) String forcedHairCode, Integer hairPreset, String name,
                             List<String> dialogues, Integer alignment, Offers offers) {
        public Definition {
            requireId(id);
            hairType = hairType == null || hairType.isBlank() ? "base" : hairType;
            if (!hairType.equals("none") && com.dragonminez.common.hair.HairStyleSlot.byHairType(hairType) == null) {
                throw new IllegalArgumentException(id + ": unknown hairType " + hairType);
            }
            hairPreset = hairPreset == null ? 1 : hairPreset;
            if (hairPreset < 0 || (hairPreset > 0 && !com.dragonminez.common.hair.HairPresets.exists(hairPreset))) {
                throw new IllegalArgumentException(id + ": unknown hairPreset " + hairPreset);
            }
            forcedHairCode = CustomNpcHair.normalizeCode(forcedHairCode);
            if (forcedHairCode != null && !forcedHairCode.isBlank()
                    && CustomNpcHair.resolveCode(forcedHairCode, hairType) == null) {
                throw new IllegalArgumentException(id + ": invalid hairCode/forcedHairCode");
            }
            if (baseTexture == null || baseTexture.isBlank()) throw new IllegalArgumentException(id + ": baseTexture is required");
            if (name == null || name.isBlank()) throw new IllegalArgumentException(id + ": name is required");
            alignment = alignment == null ? 50 : alignment;
            if (alignment < 0 || alignment > 100) throw new IllegalArgumentException(id + ": alignment must be 0..100");
            armor = armor == null ? Map.of() : Map.copyOf(armor);
            armor.forEach((slot, item) -> {
                if (!Set.of("head", "chest", "legs", "feet").contains(slot)) throw new IllegalArgumentException(id + ": unknown cosmetic armor slot " + slot);
                requireId(item);
            });
            dialogues = dialogues == null ? List.of() : List.copyOf(dialogues);
            offers = offers == null ? new Offers(null,null,null) : offers;
        }
    }
    public record Placement(String npc, String dimension, double x, double y, double z, float yaw) {
        public Placement {
            requireId(npc); requireId(dimension);
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Float.isFinite(yaw)) {
                throw new IllegalArgumentException("NPC placement coordinates must be finite");
            }
        }
    }
    public static String toJson(Definition definition) {
        String json = GSON.toJson(definition);
        if (json.length() > 30000) throw new IllegalArgumentException(definition.id() + ": NPC definition exceeds 30000 characters");
        return json;
    }
    public static Definition fromJson(String json) { return GSON.fromJson(json, Definition.class); }
    public long revision() { return revision; }
    private volatile long revision;
    private record Snapshot(Map<String, Definition> definitions, Map<String, Placement> placements) {}
    private volatile Snapshot snapshot = new Snapshot(Map.of(), Map.of());

    public synchronized void reload(Path configRoot) throws IOException {
        Map<String, Definition> definitions = new TreeMap<>();
        for (Path path : files(configRoot.resolve("custom_npcs"))) {
            Definition definition = read(path, Definition.class);
            toJson(definition);
            CustomNpcVisuals.get(definition.baseTexture());
            if (definitions.putIfAbsent(definition.id(), definition) != null) throw new IllegalArgumentException("Duplicate custom NPC: " + definition.id());
        }
        Map<String, Placement> placements = new TreeMap<>();
        for (Path path : files(configRoot.resolve("custom_npc_placements"))) {
            Placement placement = read(path, Placement.class);
            if (!definitions.containsKey(placement.npc())) throw new IllegalArgumentException(path + ": unknown custom NPC " + placement.npc());
            placements.put(path.getFileName().toString(), placement);
        }
        snapshot = new Snapshot(Map.copyOf(definitions), Map.copyOf(placements));
        revision++;
    }
    public Optional<Definition> get(String id) { return Optional.ofNullable(snapshot.definitions.get(id)); }
    public Map<String, Definition> definitions() { return snapshot.definitions; }
    public Map<String, Placement> placements() { return snapshot.placements; }
    private static List<Path> files(Path directory) throws IOException {
        Files.createDirectories(directory);
        try (var paths = Files.list(directory)) {
            return paths.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".json")).sorted().toList();
        }
    }
    private static <T> T read(Path path, Class<T> type) throws IOException {
        if (Files.size(path) > 65536) throw new IllegalArgumentException(path + ": NPC JSON exceeds 64 KiB");
        try { return Objects.requireNonNull(GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), type)); }
        catch (RuntimeException ex) { throw new IllegalArgumentException(path + ": " + ex.getMessage(), ex); }
    }
    private static void requireId(String id) {
        if (id == null || id.length() > 128 || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IllegalArgumentException("Expected namespaced id, got " + id);
    }
}
