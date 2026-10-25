# Otherworld and distant terrain (NeoForge 1.21.1)

This page describes the work in progress on `fix/2.2-neoforge-1.21.1-lod` / PR #7. It is not a compatibility claim for a released build.

## Generation contract

Otherworld uses vanilla `NoiseBasedChunkGenerator`, a fixed biome, density functions and surface rules. Its separated cloud decks are solid volumes in the density graph, with air between them. The surface rules replace the noise generator's default stone with the cloud block above the hell ceiling. Reading only the default block or only one height per column cannot reproduce this dimension.

Fixed landmarks are bundled as 348 chunk-local vanilla structure templates. The biome's `otherworld_fixed_structures_placed` feature places them during `FEATURES`. Explicit air and block-entity NBT are retained. Negative coordinates use ordinary chunk coordinates. A worker must run features to obtain the landmarks; noise sampling alone only describes natural terrain.

The feature recognizes either the Otherworld dimension or the `dragonminez:otherworld` noise-settings key, allowing a worker to use an auxiliary level with a different dimension key. Existing datapacks can still change the generator inside Otherworld. An unrelated level/generator pair is not accepted.

## Region files and existing worlds

The mod no longer copies bundled terrain regions into a world at startup, dimension load or server start. Previously, those pre-generated chunks bypassed current terrain generation. For example, the bundled saved column at `(320, 320)` has no cloud blocks above Y 96, whereas current generated terrain contains both cloud decks there. Mixing those saved chunks with newly generated distant terrain produced inconsistent inputs.

Existing region and entity files are left untouched. There is no automatic terrain repair or overwrite. Existing chunks retain the terrain and player changes already saved in them, including differences from current generation. A new world is the cleanest comparison environment; use a disposable copy when inspecting an existing world. Rebuilding an addon's LOD cache does not regenerate saved Minecraft chunks.

Bundled MCA files remain as the source/reference for the extracted landmarks. They are not copied into the save. Friendly Otherworld NPCs continue to use the normal placement manager.

## LOD integrations

An addon that generates complete chunks through surface rules and features can read the same terrain and landmarks as normal chunk generation. An addon using a single surface or skipping features needs its own suitable generation path. Voxy by itself does not promise unseen terrain generation just because a world generator is supported.

The optional Distant Horizons guards preserve the visible cloud volume and request complete columns for Otherworld. The optional DH SeedGen guard avoids its single-surface approximation in this dimension. These are scoped compatibility measures, not a universal LOD API.

For comparison, [Tectonic publishes its terrain through density functions and noise settings](https://github.com/Apollounknowndev/tectonic/tree/rewrite-squared/src/common/main/resources/resourcepacks/tectonic/data/minecraft/worldgen). [Tellus supplies addon-specific generation and a Voxy ingest bridge](https://github.com/Yucareux/Tellus/tree/main/src/main/java/com/yucareux/tellus/integration). The relevant principles here are a reproducible generation graph, complete vertical columns, features included at the right stage, and explicit separation between terrain generation and a renderer's cache ingestion. No Tellus or Tectonic implementation was copied.

## Checks

```sh
# Java 21
./gradlew build runGameTestServer --no-daemon --console=plain

# Read-only comparison of shipped templates, index and source regions
python3 scripts/worldgen/extract_otherworld_templates.py --check
```

The regression checks fill fresh protochunks with vanilla noise and surface generation, compare published columns at both sides of a former region boundary and at negative/distant coordinates, assert both cloud decks and the empty gap, and place a landmark in an auxiliary level using the Otherworld generator. They do not require importing bundled regions.

The current Neo-Voxy comparison passed full-chunk cache readback for selected cloud and landmark fixtures. After explicitly ingesting all 80 palace-area chunks, the palace appeared in the near capture but was absent in the far capture at vanilla distance 4. Distant visual compatibility has therefore not passed. The cause of that rendering failure is still under investigation.

Detailed local runtime evidence and remaining limits are recorded in `LOD-VALIDATION.md` at the repository root.
