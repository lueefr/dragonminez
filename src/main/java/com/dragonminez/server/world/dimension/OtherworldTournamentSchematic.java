package com.dragonminez.server.world.dimension;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** Immutable chunk-local data shared by worldgen workers; never owns a live level or build cursor. */
final class OtherworldTournamentSchematic {
    private static final int MAX_PALETTE = 1 << 14;
    private static final int MAX_HEIGHT = 1 << 10;
    private static final long MAX_VOLUME = 16_000_000L;
    private static final Map<ResourceManager, Optional<OtherworldTournamentSchematic>> CACHE = new WeakHashMap<>();
    private final BlockState[] palette;
    private final BoundingBox bounds;
    private final List<ChunkColumn> chunks;
    private final Map<Long, ChunkColumn> indexed;

    record ChunkColumn(int chunkX, int chunkZ, IntList entries) {}

    private OtherworldTournamentSchematic(BlockState[] palette, BoundingBox bounds, List<ChunkColumn> chunks) {
        this.palette = palette;
        this.bounds = bounds;
        this.chunks = List.copyOf(chunks);
        Map<Long, ChunkColumn> index = new HashMap<>();
        for (var column : chunks) index.put(ChunkPos.asLong(column.chunkX(), column.chunkZ()), column);
        this.indexed = Map.copyOf(index);
    }

    static Optional<OtherworldTournamentSchematic> load(ResourceManager resources) {
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(resources, manager -> {
                try (InputStream in = manager.getResourceOrThrow(OtherworldTournamentGrounds.SCHEMATIC).open()) {
                    return Optional.of(parse(NbtIo.readCompressed(in, NbtAccounter.create(64L * 1024 * 1024))));
                } catch (Exception e) {
                    LogUtil.error(Env.SERVER, "Otherworld tournament: failed to read schematic {}", OtherworldTournamentGrounds.SCHEMATIC, e);
                    return Optional.empty();
                }
            });
        }
    }

    BoundingBox bounds() { return new BoundingBox(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ()); }
    List<ChunkColumn> chunks() { return chunks; }
    BlockState state(int index) { return palette[index]; }

    boolean place(WorldGenLevel world, ChunkPos chunk) {
        ChunkColumn column = indexed.get(chunk.toLong());
        if (column == null) return false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int packed : column.entries()) {
            pos.set(chunk.getMinBlockX() + (packed & 15), bounds.minY() + (packed >>> 8 & 0x3FF),
                    chunk.getMinBlockZ() + (packed >>> 4 & 15));
            if (world.isOutsideBuildHeight(pos)) continue;
            world.setBlock(pos, palette[packed >>> 18], Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        return true;
    }

    static OtherworldTournamentSchematic parse(CompoundTag root) {
        CompoundTag schematic = root.contains("Schematic", Tag.TAG_COMPOUND) ? root.getCompound("Schematic") : root;
        int version = schematic.getInt("Version");
        int width = schematic.getShort("Width") & 0xFFFF;
        int height = schematic.getShort("Height") & 0xFFFF;
        int length = schematic.getShort("Length") & 0xFFFF;

        CompoundTag paletteTag;
        byte[] data;
        int[] offset;
        if (version >= 3) {
            CompoundTag blocks = schematic.getCompound("Blocks");
            paletteTag = blocks.getCompound("Palette");
            data = blocks.getByteArray("Data");
            offset = schematic.getIntArray("Offset");
        } else {
            paletteTag = schematic.getCompound("Palette");
            data = schematic.getByteArray("BlockData");
            CompoundTag meta = schematic.getCompound("Metadata");
            offset = new int[]{meta.getInt("WEOffsetX"), meta.getInt("WEOffsetY"), meta.getInt("WEOffsetZ")};
        }
        if (offset.length < 3) offset = new int[3];
        long volumeLong = (long) width * length * height;
        if (width == 0 || length == 0 || height == 0 || volumeLong > MAX_VOLUME) {
            throw new IllegalArgumentException("Invalid tournament schematic dimensions");
        }
        if (height > MAX_HEIGHT) throw new IllegalStateException("schematic is taller than " + MAX_HEIGHT + " blocks");

        int paletteSize = 0;
        for (String key : paletteTag.getAllKeys()) {
            int id = paletteTag.getInt(key);
            if (id < 0 || id >= MAX_PALETTE) throw new IllegalArgumentException("Invalid tournament palette id " + id);
            paletteSize = Math.max(paletteSize, id + 1);
        }
        if (paletteSize > MAX_PALETTE) throw new IllegalStateException("schematic palette has more than " + MAX_PALETTE + " states");
        BlockState[] palette = new BlockState[paletteSize];
        for (String key : paletteTag.getAllKeys()) palette[paletteTag.getInt(key)] = parseState(key);

        BlockPos min = OtherworldTournamentGrounds.RING_CENTER.offset(offset[0], offset[1], offset[2]);
        Long2ObjectOpenHashMap<IntArrayList> buckets = new Long2ObjectOpenHashMap<>();
        int layer = width * length;
        int volume = layer * height;
        int index = 0;
        int cursor = 0;
        while (cursor < data.length && index < volume) {
            int value = 0;
            int shift = 0;
            byte b;
            do {
                if (cursor >= data.length || shift >= 35) throw new IllegalArgumentException("Invalid tournament block varint");
                b = data[cursor++];
                if (shift == 28 && (b & 0xF0) != 0) throw new IllegalArgumentException("Overflowing tournament block varint");
                value |= (b & 0x7F) << shift;
                shift += 7;
            } while ((b & 0x80) != 0);

            if (value < 0 || value >= palette.length || palette[value] == null) {
                throw new IllegalArgumentException("Unknown tournament palette index " + value);
            }
            BlockState state = palette[value];
            if (state != null && !state.isAir() && !state.is(Blocks.STRUCTURE_VOID)) {
                int y = index / layer;
                int rest = index % layer;
                int worldX = min.getX() + rest % width;
                int worldZ = min.getZ() + rest / width;
                long key = ChunkPos.asLong(worldX >> 4, worldZ >> 4);
                IntArrayList bucket = buckets.get(key);
                if (bucket == null) {
                    bucket = new IntArrayList();
                    buckets.put(key, bucket);
                }
                bucket.add((worldX & 15) | (worldZ & 15) << 4 | y << 8 | value << 18);
            }
            index++;
        }

        if (index != volume) throw new IllegalArgumentException("Truncated tournament block data");
        List<ChunkColumn> chunks = new ArrayList<>(buckets.size());
        for (Long2ObjectOpenHashMap.Entry<IntArrayList> entry : buckets.long2ObjectEntrySet()) {
            ChunkPos pos = new ChunkPos(entry.getLongKey());
            chunks.add(new ChunkColumn(pos.x, pos.z, IntLists.unmodifiable(entry.getValue())));
        }
        chunks.sort(Comparator.comparingInt(ChunkColumn::chunkZ).thenComparingInt(ChunkColumn::chunkX));
        BoundingBox bounds = new BoundingBox(min.getX(), min.getY(), min.getZ(),
        min.getX() + width - 1, min.getY() + height - 1, min.getZ() + length - 1);
        return new OtherworldTournamentSchematic(palette, bounds, chunks);
    }

    private static BlockState parseState(String key) {
        try {
            return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), key, false).blockState();
        } catch (CommandSyntaxException e) {
            LogUtil.warn(Env.SERVER, "Otherworld tournament: unknown block state '{}' in schematic, skipping it", key);
            return Blocks.AIR.defaultBlockState();
        }
    }

}
