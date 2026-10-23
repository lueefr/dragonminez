package com.dragonminez.common.init;

import com.dragonminez.common.loot.AddMusicDiscModifier;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.resource.v1.DataResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import java.util.List;

public final class MainLootModifiers {
    private static final Identifier CONFIG = Identifier.parse("dragonminez:loot_modifiers/add_music_disc.json");
    private static volatile AddMusicDiscModifier musicDiscs = new AddMusicDiscModifier(List.of(), 0.5F);
    private static boolean installed;
    private MainLootModifiers() {}
    public static synchronized void register() {
        if (installed) return;
        DataResourceLoader.get().registerReloadListener(Identifier.parse("dragonminez:music_disc_loot"), lookups ->
                (ResourceManagerReloadListener) resources -> {
                    var resource = resources.getResource(CONFIG);
                    AddMusicDiscModifier updated = new AddMusicDiscModifier(List.of(), 0.5F);
                    if (resource.isPresent()) {
                        try (var reader = resource.get().openAsReader()) {
                            updated = AddMusicDiscModifier.CODEC.codec().parse(lookups.createSerializationContext(JsonOps.INSTANCE),
                                    JsonParser.parseReader(reader)).getOrThrow();
                        } catch (Exception exception) {
                            throw new IllegalArgumentException("Invalid music disc loot configuration: " + CONFIG, exception);
                        }
                    }
                    musicDiscs = updated;
                });
        LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> table.unwrapKey()
                .ifPresent(key -> musicDiscs.apply(key.identifier(), drops, context)));
        installed = true;
    }
}
