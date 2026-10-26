# Otherworld and distant terrain (NeoForge 1.21.1)

This developer guide describes `fix/2.2-neoforge-1.21.1-lod` / PR #7. It is not a compatibility claim for a released build.

## Generation contract

Otherworld uses vanilla `NoiseBasedChunkGenerator`, a fixed biome, density functions and surface rules. Its separated cloud decks are solid volumes in the density graph, with air between them. The surface rules replace the noise generator's default stone with the cloud block above the hell ceiling. Reading only the default block or only one height per column cannot reproduce this dimension.

Fixed landmarks are bundled as 348 chunk-local vanilla structure templates. The biome's `otherworld_fixed_structures_placed` feature places them during `FEATURES`. Explicit air and block-entity NBT are retained. Negative coordinates use ordinary chunk coordinates. A worker must run features to obtain the landmarks; noise sampling alone only describes natural terrain.

The tournament schematic also participates in this feature. Its non-air blocks are indexed into immutable chunk-local columns, matching the existing schematic placement semantics. Generation needs no active player or incremental build cursor. The existing incremental builder remains available for older saves, progression readiness and NPC placement. Unfinished build tickets and live level references are released when the server stops.

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

Neo-Voxy alpha.12 cache readback retains the landmark, both cloud decks and the empty gap, and cached-world captures show the palace beyond vanilla distance 4. A fresh-world and resize comparison identified a separate render-state defect: aura rendering left stencil writes disabled, preventing Voxy from initializing newly allocated stencil data. Restoring the stencil write mask recovered the distant image immediately. The branch now restores the caller's complete stencil state around aura operations, without patching Voxy or changing its renderer.

The final packaged regression sequence passed native stencil-state checks, fresh ingestion, actual distant pixels, window resizing and a dimension round trip. A fixed delay or a successful data read alone is not a visual pass.

Distant Horizons 3.3.3 with its internal server generator also renders the distant palace and tournament arena. Its terrain-data API preserves the complete cloud column and gap, the palace, the negative-coordinate landmark and the arena; the final packaged run passed resize and dimension-transition checks. Reads in the validation harness run asynchronously, with a terrain-data cache, so the harness does not block the render thread. These observations apply to the tested versions and settings; they are not a claim about every third-party generator.

Detailed local runtime evidence and remaining limits are recorded in `LOD-VALIDATION.md` at the repository root.
