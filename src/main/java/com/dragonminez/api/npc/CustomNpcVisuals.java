package com.dragonminez.api.npc;

import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Neutral appearance aliases. Addons may register further bundled model/texture pairs at startup. */
public final class CustomNpcVisuals {
    public record Base(ResourceLocation model, ResourceLocation texture) {}
    private static final Map<String, Base> BASES = new ConcurrentHashMap<>();
    static {
        register("master_male", "dragonminez:geo/entity/races/human.geo.json", "dragonminez:textures/entity/races/humansaiyan/bodytype_male_1_layer1.png");
        register("master_female", "dragonminez:geo/entity/races/hbuffed_fem.geo.json", "dragonminez:textures/entity/races/humansaiyan/bodytype_female_1_layer1.png");
        register("humanoid_slim", "dragonminez:geo/entity/races/human_slim.geo.json", "dragonminez:textures/entity/races/humansaiyan/bodytype_male_1_layer1.png");
    }
    private CustomNpcVisuals() {}
    public static void register(String id, String model, String texture) {
        if (BASES.putIfAbsent(id,new Base(ResourceLocation.parse(model), ResourceLocation.parse(texture))) != null) {
            throw new IllegalArgumentException("Duplicate custom NPC appearance base: " + id);
        }
    }
    public static Base get(String id) {
        Base base=BASES.get(id);
        if (base==null) throw new IllegalArgumentException("Unknown custom NPC appearance base: " + id);
        return base;
    }
    public static Map<String, Base> bases() { return Map.copyOf(BASES); }
}
