package com.dragonminez.server.world.feature;

import com.dragonminez.server.world.gen.ColumnCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Runtime checks for Otherworld density wrappers and landmark chunk templates. */
@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class OtherworldFixedStructureGameTests {
    private static final ChunkPos NEGATIVE_FIXTURE = new ChunkPos(-1, 61);
    private static final ChunkPos BLOCK_ENTITY_FIXTURE = new ChunkPos(3, 65);

    @GameTest(template = "port_empty")
    public static void cache2dAndLegacyCodecKeepExactSamples(GameTestHelper helper) {
        DensityFunction coordinateFunction = new DensityFunction() {
            @Override
            public double compute(FunctionContext context) {
                return context.blockX() * 31.0 + context.blockZ() * 7.0;
            }

            @Override
            public void fillArray(double[] values, ContextProvider provider) {
                provider.fillAllDirectly(values, this);
            }

            @Override
            public DensityFunction mapAll(Visitor visitor) {
                return visitor.apply(this);
            }

            @Override
            public double minValue() { return -1_000_000.0; }

            @Override
            public double maxValue() { return 1_000_000.0; }

            @Override
            public net.minecraft.util.KeyDispatchDataCodec<? extends DensityFunction> codec() {
                return DensityFunctions.constant(0).codec();
            }
        };
        DensityFunction cached = DensityFunctions.cache2d(coordinateFunction);
        for (int x : new int[]{-17, -16, -15, -1, 0, 1, 3, 4, 5, 15, 16, 17}) {
            for (int z : new int[]{-17, -16, -15, -1, 0, 1, 3, 4, 5, 15, 16, 17}) {
                var context = new DensityFunction.FunctionContext() {
                    @Override
                    public int blockX() { return x; }
                    @Override
                    public int blockY() { return 0; }
                    @Override
                    public int blockZ() { return z; }
                };
                helper.assertTrue(cached.compute(context) == coordinateFunction.compute(context),
                        "cache_2d changed exact block sample at " + x + "," + z);
            }
        }

        var encoded = ColumnCache.CODEC.codec().codec().encodeStart(com.mojang.serialization.JsonOps.INSTANCE,
                new ColumnCache(DensityFunctions.constant(2.5))).getOrThrow();
        var decoded = ColumnCache.CODEC.codec().codec().parse(com.mojang.serialization.JsonOps.INSTANCE, encoded).getOrThrow();
        helper.assertTrue(decoded.compute(new DensityFunction.FunctionContext() {
            @Override public int blockX() { return -1; }
            @Override public int blockY() { return 0; }
            @Override public int blockZ() { return 17; }
        }) == 2.5, "Legacy dragonminez:column_cache codec must remain decodable");

        var placedRegistry = helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        var placedKey = ResourceLocation.fromNamespaceAndPath("dragonminez", "otherworld_fixed_structures_placed");
        helper.assertTrue(placedRegistry.get(placedKey) != null,
                "Fixed structure placed feature must be present in runtime registries");
        helper.succeed();
    }

    @GameTest(template = "port_empty")
    public static void negativeChunkTemplateClipsAndPlacesAir(GameTestHelper helper) {
        var level = helper.getLevel();
        var sourceTemplate = load(level, NEGATIVE_FIXTURE);
        helper.assertTrue(sourceTemplate.getSize().getX() == 16 && sourceTemplate.getSize().getZ() == 16,
                "Negative source template must be chunk-local");

        var settings = new StructurePlaceSettings();
        List<StructureBlockInfo> solid = sourceTemplate.filterBlocks(BlockPos.ZERO, settings, Blocks.ANDESITE);
        List<StructureBlockInfo> air = sourceTemplate.filterBlocks(BlockPos.ZERO, settings, Blocks.AIR);
        helper.assertTrue(!solid.isEmpty() && !air.isEmpty(), "Negative fixture needs solid and explicit-air entries");

        BlockPos solidTarget = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos airTarget = helper.absolutePos(new BlockPos(2, 1, 1));
        BlockPos untouchedTarget = solidTarget.offset(2, 0, 0);
        level.setBlock(solidTarget, Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
        level.setBlock(airTarget, Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
        level.setBlock(untouchedTarget, Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
        placeOne(helper, NEGATIVE_FIXTURE, solid.getFirst(), solidTarget);
        helper.assertTrue(level.getBlockState(airTarget).is(Blocks.DIAMOND_BLOCK),
                "One-block clipping must not spill from solid placement");
        placeOne(helper, NEGATIVE_FIXTURE, air.getFirst(), airTarget);

        helper.assertTrue(level.getBlockState(solidTarget).is(Blocks.ANDESITE),
                "Chunk-local template must place its source block in the clipped target");
        helper.assertTrue(level.getBlockState(airTarget).isAir(),
                "Explicit air inside the landmark mask must clear its clipped target");
        helper.assertTrue(level.getBlockState(untouchedTarget).is(Blocks.DIAMOND_BLOCK),
                "One-block clipping must not spill to another fresh block");
        helper.succeed();
    }

    @GameTest(template = "port_empty")
    public static void blockEntityTemplateNbtLoadsThroughStructureManager(GameTestHelper helper) {
        var template = load(helper.getLevel(), BLOCK_ENTITY_FIXTURE);
        List<StructureBlockInfo> beds = template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.YELLOW_BED);
        helper.assertTrue(!beds.isEmpty(), "Bundled block-entity fixture should contain yellow beds");
        StructureBlockInfo bed = beds.getFirst();
        helper.assertTrue(bed.nbt() != null && bed.nbt().contains("id"),
                "Template NBT for a block entity must survive extraction");

        BlockPos target = helper.absolutePos(new BlockPos(1, 1, 1));
        placeOne(helper, BLOCK_ENTITY_FIXTURE, bed, target);
        helper.assertTrue(helper.getLevel().getBlockState(target).is(Blocks.YELLOW_BED),
                "Block entity block state must be placed");
        helper.assertTrue(helper.getLevel().getBlockEntity(target) instanceof net.minecraft.world.level.block.entity.BedBlockEntity,
                "Template placement must instantiate the block entity from its NBT");
        helper.succeed();
    }

    private static StructureTemplate load(net.minecraft.server.level.ServerLevel level, ChunkPos sourceChunk) {
        var id = ResourceLocation.fromNamespaceAndPath("dragonminez",
                "otherworld_chunks/chunk_" + sourceChunk.x + "_" + sourceChunk.z);
        return level.getStructureManager().getOrCreate(id);
    }

    private static void placeOne(GameTestHelper helper, ChunkPos sourceChunk, StructureBlockInfo info, BlockPos target) {
        BlockPos origin = target.subtract(info.pos());
        BoundingBox clip = new BoundingBox(target.getX(), target.getY(), target.getZ(),
                target.getX(), target.getY(), target.getZ());
        boolean placed = OtherworldFixedStructuresFeature.placeChunkTemplate(helper.getLevel(), sourceChunk,
                origin, clip, RandomSource.create(0L));
        helper.assertTrue(placed, "Chunk template placement call failed for " + sourceChunk);
    }
}
