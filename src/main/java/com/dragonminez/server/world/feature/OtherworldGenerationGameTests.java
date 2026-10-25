package com.dragonminez.server.world.feature;

import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.server.world.gen.OtherworldGeneration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import com.dragonminez.server.world.biome.OtherworldBiomes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Checks actual generated chunks, without importing pre-generated region files. */
@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class OtherworldGenerationGameTests {
    @GameTest(template = "port_empty", timeoutTicks = 600)
    public static void generatedCloudColumnsMatchPublishedNoise(GameTestHelper helper) {
        var level = helper.getLevel();
        var access = level.registryAccess();
        var biomes = access.registryOrThrow(Registries.BIOME);
        var biome = biomes.getHolderOrThrow(OtherworldBiomes.OTHERWORLD);
        var settings = access.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(OtherworldGeneration.OTHERWORLD_NOISE_SETTINGS);
        var generator = new NoiseBasedChunkGenerator(new FixedBiomeSource(biome), settings);
        var randomState = RandomState.create(settings.value(), access.lookupOrThrow(Registries.NOISE), 12345L);
        var height = LevelHeightAccessor.create(OtherworldGeneration.WORLD_BOTTOM, OtherworldGeneration.WORLD_TOP - OtherworldGeneration.WORLD_BOTTOM);
        // Inside the former imported region, across its edge, negative coordinates and far away.
        for (int[] at : new int[][]{{320, 320}, {511, 320}, {512, 320}, {-321, -321}, {2048, 2048}}) {
            var published = generator.getBaseColumn(at[0], at[1], height, randomState);
            var chunk = new ProtoChunk(new ChunkPos(at[0] >> 4, at[1] >> 4), UpgradeData.EMPTY, height, biomes, null);
            chunk.fillBiomesFromNoise(new FixedBiomeSource(biome), randomState.sampler());
            var filling = generator.fillFromNoise(Blender.empty(), randomState, level.structureManager(), chunk);
            // Vanilla asks the server for structure starts from the worker thread.
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
            level.getServer().managedBlock(() -> {
                level.getChunkSource().pollTask();
                if (System.nanoTime() > deadline) throw new IllegalStateException("Timed out filling test chunk");
                return filling.isDone();
            });
            filling.join();
            generator.buildSurface(chunk, new WorldGenerationContext(generator, height), randomState,
                    level.structureManager(), new BiomeManager((x, y, z) -> biome, 12345L), biomes, Blender.empty());
            int lowerClouds = 0;
            int upperClouds = 0;
            for (int y = OtherworldGeneration.HELL_CEILING; y < OtherworldGeneration.WORLD_TOP; y++) {
                BlockPos pos = new BlockPos(at[0], y, at[1]);
                var generated = chunk.getBlockState(pos);
                helper.assertTrue(published.getBlock(y).isAir() == generated.isAir(),
                        "Published noise differs from generated terrain at " + pos);
                if (!generated.isAir()) {
                    helper.assertTrue(generated.is(MainBlocks.OTHERWORLD_CLOUD.get()),
                            "Cloud terrain must use cloud blocks instead of a stone wall at " + pos);
                    if (y < OtherworldGeneration.PALACE_LEVEL) lowerClouds++;
                    else upperClouds++;
                }
                if (y >= OtherworldGeneration.PALACE_LEVEL && y < OtherworldGeneration.TOURNAMENT_LEVEL - 16) {
                    helper.assertTrue(generated.isAir(), "The gap between cloud decks must remain empty at " + pos);
                }
            }
            helper.assertTrue(lowerClouds > 0 && upperClouds > 0,
                    "Both separated cloud decks must generate at " + at[0] + "," + at[1]);
        }
        helper.succeed();
    }

    @GameTest(template = "port_empty")
    public static void syntheticLevelFeatureUsesGeneratorSettings(GameTestHelper helper) {
        var level = helper.getLevel();
        var access = level.registryAccess();
        var biome = access.registryOrThrow(Registries.BIOME).getHolderOrThrow(OtherworldBiomes.OTHERWORLD);
        var settings = access.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(OtherworldGeneration.OTHERWORLD_NOISE_SETTINGS);
        var generator = new NoiseBasedChunkGenerator(new FixedBiomeSource(biome), settings);
        BlockPos landmark = new BlockPos(-1, 162, 980);
        var feature = OtherworldFeatures.FIXED_STRUCTURES.get();
        boolean placed = feature.place(new FeaturePlaceContext<>(java.util.Optional.empty(), level, generator,
                net.minecraft.util.RandomSource.create(0L), new BlockPos(-16, 0, 976), NoneFeatureConfiguration.INSTANCE));
        helper.assertTrue(placed && level.getBlockState(landmark).is(Blocks.ANDESITE),
                "Otherworld FEATURES must work in a synthetic level identified by generator settings");
        boolean unrelated = feature.place(new FeaturePlaceContext<>(java.util.Optional.empty(), level,
                level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(0L),
                new BlockPos(-16, 0, 976), NoneFeatureConfiguration.INSTANCE));
        helper.assertTrue(!unrelated, "An unrelated level/generator must not receive fixed Otherworld builds");
        helper.succeed();
    }
}
