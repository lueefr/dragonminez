package com.dragonminez.common.hair;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Client-owned presets are independent of server, character identity and race. */
public final class HairLibrary {
    public record Entry(String id, String name) {}
    private final Path folder;
    public HairLibrary(Path folder) { this.folder = folder; }

    public List<Entry> list() throws IOException {
        if (!Files.isDirectory(folder)) return List.of();
        var result = new ArrayList<Entry>();
        try (var files = Files.list(folder)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".dat")).sorted().limit(256).toList()) {
                String name = file.getFileName().toString();
                String id = name.substring(0, name.length() - 4);
                try {
                    UUID.fromString(id);
                    CompoundTag tag = read(id);
                    result.add(new Entry(id, tag.getString("Name")));
                } catch (IllegalArgumentException | IOException ex) {
                    com.dragonminez.LogUtil.warn(com.dragonminez.Env.CLIENT, "Cannot read hair preset {}: {}", name, ex.getMessage());
                }
            }
        }
        result.sort(Comparator.comparing(Entry::name).thenComparing(Entry::id));
        return List.copyOf(result);
    }

    public Entry save(String name, EnumMap<HairStyleSlot, CustomHair> styles, boolean base) throws IOException {
        if (name == null || name.isBlank() || name.length() > 64) throw new IllegalArgumentException("Preset name must contain 1..64 characters");
        if (list().size() >= 256) throw new IOException("Hair preset library is full");
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", 1); tag.putString("Name", name.trim()); tag.putBoolean("HairBase", base);
        for (var slot : HairStyleSlot.values()) {
            var hair = styles.get(slot);
            if (hair != null) tag.put(slot.getNbtKey(), hair.save());
        }
        Files.createDirectories(folder);
        String id = UUID.randomUUID().toString();
        Path temp = Files.createTempFile(folder, "preset-", ".tmp");
        try {
            NbtIo.writeCompressed(tag, temp);
            Files.move(temp, path(id), StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temp, path(id));
        } finally { Files.deleteIfExists(temp); }
        return new Entry(id, name.trim());
    }

    public CompoundTag read(String id) throws IOException {
        CompoundTag tag = NbtIo.readCompressed(path(id), NbtAccounter.create(16L << 20));
        if (tag.getInt("Version") != 1) throw new IOException("Unsupported hair preset version");
        return tag;
    }

    public EnumMap<HairStyleSlot, CustomHair> styles(String id) throws IOException {
        CompoundTag tag = read(id);
        var styles = new EnumMap<HairStyleSlot, CustomHair>(HairStyleSlot.class);
        for (var slot : HairStyleSlot.values()) {
            if (tag.contains(slot.getNbtKey(), 10)) styles.put(slot, CustomHair.fromTag(tag.getCompound(slot.getNbtKey())));
        }
        if (styles.isEmpty()) throw new IOException("Hair preset contains no styles");
        return styles;
    }

    public void delete(String id) throws IOException { Files.deleteIfExists(path(id)); }
    private Path path(String id) { return folder.resolve(UUID.fromString(id).toString() + ".dat"); }
}
