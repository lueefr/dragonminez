package com.dragonminez.common.config;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ClothingConfig {
    private List<Entry> rules = new ArrayList<>();

    public static final class Entry {
        private List<String> items = new ArrayList<>();
        private List<String> tags = new ArrayList<>();
        private double minDamage;
        private double maxDamage = 1;
        private boolean hideTop = true;
    }

    public boolean hidesTop(String item, Set<String> tags, double damage) {
        boolean hidden = false;
        for (Entry rule : rules) {
            if (damage < rule.minDamage || damage > rule.maxDamage) continue;
            if (rule.items.contains(item) || rule.tags.stream().anyMatch(tags::contains)) hidden = rule.hideTop;
        }
        return hidden;
    }

    public static ClothingConfig parse(String json) {
        ClothingConfig config = new Gson().fromJson(json, ClothingConfig.class);
        if (config == null || config.rules == null) throw new IllegalArgumentException("Missing clothing rules");
        for (Entry rule : config.rules) {
            if (rule == null || rule.items == null || rule.tags == null || rule.items.isEmpty() && rule.tags.isEmpty())
                throw new IllegalArgumentException("Clothing rule needs items or tags");
            if (!Double.isFinite(rule.minDamage) || !Double.isFinite(rule.maxDamage) || rule.minDamage < 0 || rule.maxDamage > 1 || rule.minDamage > rule.maxDamage)
                throw new IllegalArgumentException("Clothing damage range must be within 0..1");
            for (String id : rule.items) checkId(id);
            for (String id : rule.tags) checkId(id);
        }
        return config;
    }

    private static void checkId(String id) {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IllegalArgumentException("Invalid clothing identifier: " + id);
    }

    public static ClothingConfig defaults() {
        try (var stream = ClothingConfig.class.getResourceAsStream("/data/dragonminez/defaults/appearance/clothing.json")) {
            if (stream == null) throw new IllegalStateException("Missing bundled clothing rules");
            return parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException ex) { throw new IllegalStateException(ex); }
    }

    public static ClothingConfig load(Path file) throws IOException {
        if (!Files.exists(file)) {
            Files.createDirectories(file.getParent());
            try (var stream = ClothingConfig.class.getResourceAsStream("/data/dragonminez/defaults/appearance/clothing.json")) {
                if (stream == null) throw new IOException("Missing bundled clothing rules");
                Files.copy(stream, file);
            }
        }
        if (Files.size(file) > 1048576) throw new IllegalArgumentException("Clothing configuration exceeds 1 MiB");
        return parse(Files.readString(file));
    }
}
