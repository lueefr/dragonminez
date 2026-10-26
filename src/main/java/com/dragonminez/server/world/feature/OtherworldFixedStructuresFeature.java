package com.dragonminez.server.world.feature;

import com.dragonminez.Reference;
import com.dragonminez.server.world.dimension.OtherworldDimension;
import com.dragonminez.server.world.dimension.OtherworldTournamentGrounds;
import com.dragonminez.server.world.gen.OtherworldGeneration;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Places the bundled fixed Otherworld builds through the ordinary chunk feature pipeline. */
public final class OtherworldFixedStructuresFeature extends Feature<NoneFeatureConfiguration> {
	private static final String INDEX_PATH = "/data/dragonminez/otherworld/fixed_structure_chunks.txt";
	private static final Map<Long, TemplateVerticalBounds> TEMPLATE_BOUNDS = loadTemplateChunks();

	private record TemplateVerticalBounds(int minY, int maxY) {
	}

	public OtherworldFixedStructuresFeature(com.mojang.serialization.Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		// LOD workers may run this generator in a synthetic level with a different dimension key.
		boolean otherworldLevel = world.getLevel().dimension().equals(OtherworldDimension.OTHERWORLD_KEY);
		boolean otherworldGenerator = context.chunkGenerator() instanceof NoiseBasedChunkGenerator noise
				&& noise.generatorSettings().is(OtherworldGeneration.OTHERWORLD_NOISE_SETTINGS);
		if (!otherworldLevel && !otherworldGenerator) return false;

		ChunkPos chunkPos = new ChunkPos(context.origin());
        boolean tournament = OtherworldTournamentGrounds.placeInGeneratedChunk(world, chunkPos);
		TemplateVerticalBounds templateBounds = TEMPLATE_BOUNDS.get(chunkPos.toLong());
		if (templateBounds == null) return tournament;

		BlockPos origin = new BlockPos(chunkPos.getMinBlockX(), templateBounds.minY(), chunkPos.getMinBlockZ());
		BoundingBox chunkBounds = new BoundingBox(
				chunkPos.getMinBlockX(), templateBounds.minY(), chunkPos.getMinBlockZ(),
				chunkPos.getMaxBlockX(), templateBounds.maxY(), chunkPos.getMaxBlockZ());
		return placeChunkTemplate(world, chunkPos, origin, chunkBounds, context.random()) || tournament;
	}

	static boolean placeChunkTemplate(WorldGenLevel world, ChunkPos sourceChunk, BlockPos origin,
			BoundingBox clippingBounds, RandomSource random) {
		ResourceLocation templateId = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
				"otherworld_chunks/chunk_" + sourceChunk.x + "_" + sourceChunk.z);
		StructureTemplate template = world.getLevel().getStructureManager().getOrCreate(templateId);
		StructurePlaceSettings settings = new StructurePlaceSettings()
				.setKnownShape(true)
				.setIgnoreEntities(true)
				.setBoundingBox(clippingBounds);
		return template.placeInWorld(world, origin, origin, settings, random, 2);
	}

	private static Map<Long, TemplateVerticalBounds> loadTemplateChunks() {
		InputStream stream = OtherworldFixedStructuresFeature.class.getResourceAsStream(INDEX_PATH);
		if (stream == null) throw new IllegalStateException("Missing bundled Otherworld structure index " + INDEX_PATH);

		Map<Long, TemplateVerticalBounds> chunks = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String trimmed = line.trim();
				if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
				String[] coordinates = trimmed.split(",", -1);
				if (coordinates.length != 4) throw new IOException("Invalid chunk entry: " + trimmed);
				int x = Integer.parseInt(coordinates[0].trim());
				int z = Integer.parseInt(coordinates[1].trim());
				int minY = Integer.parseInt(coordinates[2].trim());
				int maxY = Integer.parseInt(coordinates[3].trim());
				if (minY > maxY || minY < OtherworldGeneration.WORLD_BOTTOM || maxY >= OtherworldGeneration.WORLD_TOP) {
					throw new IOException("Invalid vertical bounds in chunk entry: " + trimmed);
				}
				if (chunks.put(ChunkPos.asLong(x, z), new TemplateVerticalBounds(minY, maxY)) != null) {
					throw new IOException("Duplicate chunk entry: " + trimmed);
				}
			}
		} catch (IOException | NumberFormatException exception) {
			throw new IllegalStateException("Could not read bundled Otherworld structure index " + INDEX_PATH, exception);
		}
		return Map.copyOf(chunks);
	}
}
