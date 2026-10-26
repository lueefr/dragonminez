package com.dragonminez.server.world.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class OtherworldTournamentGenerationGameTests {
    @GameTest(template = "port_empty")
    public static void tournamentSchematicPlacesOnlyTheRequestedNegativeChunk(GameTestHelper helper) {
        int y = helper.absolutePos(new BlockPos(1, 1, 1)).getY();
        CompoundTag root = fixture(2, 17, -1, y - 355);
        var schematic = OtherworldTournamentSchematic.parse(root);
        BlockPos inside = new BlockPos(-1, y, -1200);
        BlockPos outside = inside.east();
        helper.getLevel().setBlock(outside, Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
        helper.assertTrue(schematic.place(helper.getLevel(), new ChunkPos(-1, -75)), "Negative chunk must be present");
        helper.assertTrue(helper.getLevel().getBlockState(inside).is(Blocks.STONE), "Requested chunk must get its schematic blocks");
        helper.assertTrue(helper.getLevel().getBlockState(outside).is(Blocks.DIAMOND_BLOCK), "Worldgen must not spill into its neighbor");
        helper.assertTrue(schematic.place(helper.getLevel(), new ChunkPos(0, -75)), "Neighbor must generate independently");
        helper.assertTrue(helper.getLevel().getBlockState(outside).is(Blocks.STONE), "Neighbor must get the matching schematic state");
        helper.succeed();
    }

    @GameTest(template = "port_empty")
    public static void tournamentSchematicAcceptsV3AndRejectsTruncatedBlocks(GameTestHelper helper) {
        CompoundTag v3 = fixture(3, 1, -1, -100);
        CompoundTag wrapped = new CompoundTag();
        wrapped.put("Schematic", v3);
        var parsed = OtherworldTournamentSchematic.parse(wrapped);
        helper.assertTrue(parsed.bounds().minX() == -1 && parsed.bounds().minY() == 255,
                "Sponge v3 must preserve its placement offset");
        helper.assertTrue(parsed.state(0).is(Blocks.STONE) && parsed.chunks().size() == 1,
                "Sponge v3 Blocks palette/data must be read");
        CompoundTag truncated = fixture(2, 2, 0, -100);
        truncated.putByteArray("BlockData", new byte[]{0});
        boolean rejected = false;
        try { OtherworldTournamentSchematic.parse(truncated); }
        catch (IllegalArgumentException expected) { rejected = true; }
        helper.assertTrue(rejected, "A truncated schematic must not become partial worldgen content");
        helper.succeed();
    }

    @GameTest(template = "port_empty")
    public static void bundledTournamentHasAWorldgenBlueprintWithoutAnActiveBuild(GameTestHelper helper) {
        var schematic = OtherworldTournamentSchematic.load(helper.getLevel().getServer().getResourceManager()).orElseThrow();
        var bounds = schematic.bounds();
        helper.assertTrue(bounds.minX() == -110 && bounds.minY() == 257 && bounds.minZ() == -1310
                        && bounds.maxY() == 382 && schematic.chunks().size() > 1,
                "Bundled tournament must be readable without an Otherworld player, tickets or SavedData");
        helper.succeed();
    }

    private static CompoundTag fixture(int version, int width, int xOffset, int yOffset) {
        CompoundTag root = new CompoundTag();
        root.putInt("Version", version);
        root.putShort("Width", (short) width);
        root.putShort("Height", (short) 1);
        root.putShort("Length", (short) 1);
        CompoundTag palette = new CompoundTag();
        palette.putInt("minecraft:stone", 0);
        if (version >= 3) {
            CompoundTag blocks = new CompoundTag();
            blocks.put("Palette", palette);
            blocks.putByteArray("Data", new byte[width]);
            root.put("Blocks", blocks);
            root.putIntArray("Offset", new int[]{xOffset, yOffset, 0});
        } else {
            root.put("Palette", palette);
            root.putByteArray("BlockData", new byte[width]);
            CompoundTag metadata = new CompoundTag();
            metadata.putInt("WEOffsetX", xOffset);
            metadata.putInt("WEOffsetY", yOffset);
            root.put("Metadata", metadata);
        }
        return root;
    }
}
