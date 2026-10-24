package com.dragonminez.client.util;

import com.dragonminez.api.npc.CustomNpcVisuals;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Client-local PNGs; no texture bytes are accepted from entity sync or downloaded from a server. */
public final class CustomNpcTextures {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation FALLBACK = CustomNpcVisuals.texture("master_base");
    private static final int MAX_BYTES = 4 * 1024 * 1024;
    private static final int MAX_DIMENSION = 2048;
    private static final Map<String, Entry> CACHE = new LinkedHashMap<>(16, .75f, true);
    private static boolean initialized;
    private static final class Entry {
        long checked, modified = Long.MIN_VALUE, size = -1;
        ResourceLocation location = FALLBACK;
        boolean loaded;
    }
    private CustomNpcTextures() {}

    public static void initialize() {
        Path directory = FMLPaths.CONFIGDIR.get().resolve("dragonminez/npc_textures");
        if (!initialized) {
            try {
                Files.createDirectories(directory);
                Path base = directory.resolve("master_base.png");
                if (!Files.exists(base)) {
                    try (var source = CustomNpcTextures.class.getResourceAsStream("/assets/dragonminez/textures/entity/custom/master_base.png")) {
                        if (source == null) throw new IOException("Bundled master_base.png is missing");
                        try { Files.copy(source, base); }
                        catch (FileAlreadyExistsException ignored) { /* Preserve an existing user file. */ }
                    }
                }
                initialized = true;
            } catch (IOException ex) {
                // Retry on a later render if the filesystem was temporarily unavailable.
                if (CACHE.isEmpty()) LOGGER.warn("Cannot create custom NPC texture folder {}", directory, ex);
            }
        }
    }

    public static ResourceLocation resolve(String name) {
        CustomNpcVisuals.texture(name); // Validate before resolving a filesystem path.
        initialize();
        Path directory = FMLPaths.CONFIGDIR.get().resolve("dragonminez/npc_textures");
        Entry entry = CACHE.computeIfAbsent(name, ignored -> new Entry());
        if (CACHE.size() > 128) {
            var oldest = CACHE.entrySet().iterator();
            var evicted = oldest.next();
            if (evicted.getValue().loaded) Minecraft.getInstance().getTextureManager().release(evicted.getValue().location);
            oldest.remove();
        }
        long now = System.currentTimeMillis();
        if (entry.checked != 0 && now - entry.checked < 1000) return entry.location;
        entry.checked = now;
        Path file = directory.resolve(name + ".png");
        try {
            long modified = Files.exists(file) ? Files.getLastModifiedTime(file).toMillis() : -1;
            long size = Files.exists(file) ? Files.size(file) : -1;
            if (modified == entry.modified && size == entry.size) return entry.location;
            entry.modified = modified; entry.size = size;
            if (size < 24 || size > MAX_BYTES) throw new IOException("Missing PNG or file outside 24 byte–4 MiB limit");
            byte[] bytes;
            try (var input = Files.newInputStream(file)) { bytes = input.readNBytes(MAX_BYTES + 1); }
            if (bytes.length < 24 || bytes.length > MAX_BYTES) throw new IOException("Invalid PNG size");
            ByteBuffer header = ByteBuffer.wrap(bytes);
            if (header.getLong() != 0x89504e470d0a1a0aL || header.getInt() != 13 || header.getInt() != 0x49484452) {
                throw new IOException("Expected PNG signature and IHDR");
            }
            int width = header.getInt(), height = header.getInt();
            if (width < 1 || height < 1 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
                throw new IOException("PNG dimensions must be 1..2048 pixels");
            }
            NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath("dragonminez", "external_npc/" + name);
            if (entry.loaded) Minecraft.getInstance().getTextureManager().release(entry.location);
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
            entry.location = location; entry.loaded = true;
        } catch (IOException | RuntimeException ex) {
            if (entry.loaded) Minecraft.getInstance().getTextureManager().release(entry.location);
            entry.location = FALLBACK; entry.loaded = false;
            LOGGER.warn("Cannot load custom NPC texture {}; using master_base: {}", file, ex.getMessage());
        }
        return entry.location;
    }
}
