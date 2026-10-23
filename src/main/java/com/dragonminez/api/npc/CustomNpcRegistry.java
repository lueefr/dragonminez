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
        public Trade { requireId(item); if (count < 1 || price < 0) throw new IllegalArgumentException("Invalid shop trade"); }
    }
    public record Offers(List<String> skills, List<String> quests, List<Trade> shop) {
        public Offers {
            skills = skills == null ? List.of() : List.copyOf(skills);
            quests = quests == null ? List.of() : List.copyOf(quests);
            shop = shop == null ? List.of() : List.copyOf(shop);
        }
    }
    public record Definition(String id, String baseTexture, Map<String, String> armor,
                             String hairType, String forcedHairCode, String name,
                             List<String> dialogues, int alignment, Offers offers) {
        public Definition {
            requireId(id);
            if (baseTexture == null || baseTexture.isBlank()) throw new IllegalArgumentException(id + ": baseTexture is required");
            if (name == null || name.isBlank()) throw new IllegalArgumentException(id + ": name is required");
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
    private record Snapshot(Map<String, Definition> definitions, Map<String, Placement> placements) {}
    private volatile Snapshot snapshot = new Snapshot(Map.of(), Map.of());

    public synchronized void reload(Path configRoot) throws IOException {
        Map<String, Definition> definitions = new TreeMap<>();
        for (Path path : files(configRoot.resolve("custom_npcs"))) {
            Definition definition = read(path, Definition.class);
            if (definitions.putIfAbsent(definition.id(), definition) != null) throw new IllegalArgumentException("Duplicate custom NPC: " + definition.id());
        }
        Map<String, Placement> placements = new TreeMap<>();
        for (Path path : files(configRoot.resolve("custom_npc_placements"))) {
            Placement placement = read(path, Placement.class);
            if (!definitions.containsKey(placement.npc())) throw new IllegalArgumentException(path + ": unknown custom NPC " + placement.npc());
            placements.put(path.getFileName().toString(), placement);
        }
        snapshot = new Snapshot(Map.copyOf(definitions), Map.copyOf(placements));
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
        try { return Objects.requireNonNull(GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), type)); }
        catch (RuntimeException ex) { throw new IllegalArgumentException(path + ": " + ex.getMessage(), ex); }
    }
    private static void requireId(String id) {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IllegalArgumentException("Expected namespaced id, got " + id);
    }
}
