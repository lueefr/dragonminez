# Otherworld / Voxy progress — 2026-10-25

Branch: `fix/2.2-neoforge-1.21.1-lod`, updated with `main` through PR #22. Minecraft 1.21.1, NeoForge 21.1.238, Java 21. Mod version remains 2.2. PR #7 remains a draft.

## Change

- Removed all three startup/dimension-load imports of bundled MCA terrain and the region-repair implementation. Existing saves are never replaced by that loader. Fresh chunks now use the published density graph, surface rules and the existing per-chunk landmark feature.
- Extended the landmark feature guard to recognize Otherworld noise settings in auxiliary levels, while retaining dimension recognition for existing datapack overrides.
- Added two GameTests for fresh noise/surface columns and auxiliary-level landmark generation.
- Added `extract_otherworld_templates.py --check` for a read-only round-trip comparison of existing templates and their index with the original regions.

The original bundled column `(320, 320)` contains no non-air blocks above Y 96. Current procedural generation has both cloud decks there. This confirms a discrepancy between the old imported terrain and the current graph, independently of any renderer.

## Automated validation

- `build runGameTestServer`: 26 unit tests and all 40 required GameTests passed. `BUILD SUCCESSFUL` with normal server shutdown. Initial log: `/tmp/dmz-lod-progress-build-4.log`; final successful validation log: `/tmp/dmz-lod-progress-final-build.log`.
- Read-only template verification passed: 348 templates, 4,831,278 positions (4,244,741 air and 586,537 non-air), 22 block entities and negative chunks; the shipped index and file set matched the source regions. Log: `/tmp/dmz-lod-templates-check.log`.
- Source translation audit and its four Python regression tests passed.
- `git diff --check` passed.

## Minecraft with Neo-Voxy

An isolated client loaded the packaged candidate with Sodium 0.8.13 and the supplied, unmodified Neo-Voxy `0.5.0-alpha.12-mc1.21.1-neoforge`. No SeedGen was loaded. The test used a new disposable world, software OpenGL on a separate virtual display, and normal integrated-server chunk generation.

The actual generated columns at `(320,320)`, `(511,320)`, `(512,320)`, `(-321,-321)` and `(2048,2048)` matched the published noise's solid/air layout above Y 96. Both decks used cloud blocks. The fresh full chunk at `(-1,61)` contained ANDESITE at `(-1,162,980)` through features.

The harness explicitly submitted two full chunks through Voxy's own `tryIngestChunk` API and read its `WorldEngine`/`Mapper` data back. The landmark remained ANDESITE; the `(320,320)` column retained 8 lower-deck cloud blocks and 13 upper-deck cloud blocks with an empty gap. The Voxy render system initialized its normal pipeline and shut down normally. This establishes full-chunk ingestion for those fixtures, not automatic generation of unseen terrain.

Logs: `/home/lucas/Dev/dragonminez-port-tools/lod-voxy-smoke/run.log` and `final.log` in the same directory. Both clients finished with `BUILD SUCCESSFUL`. The second run used the exact packaged checksum below, vanilla distance 4, and waited 20 seconds after submitting the chunks. Both captures show sky, so neither establishes distant rendering coverage. A full-palace cache comparison is being checked separately; the data-ingestion result must not be presented as a visual pass.

## Limits

No existing world terrain or LOD database is automatically rewritten. Old saved chunks may still differ from newly generated chunks. The tournament arena still has a separate incremental runtime builder; this milestone does not claim that every runtime-built structure is available to every distant generator.

The supplied Neo-Voxy alpha.12 / Voxy SeedGen 0.3.0 combination previously failed in SeedGen's required far-plane mixin before the comparison. No third-party JAR was patched and no compatibility claim for that pair is made here.

Historical combined-branch evidence remains in `HOTFIX-VALIDATION.md`; it is not substituted for tests of this updated branch.

Packaged candidate SHA-256: `4f3ba4cc6db6678d685b5975fad845678dfc5d090d7cafc2e3eea945075ff67d`. The distributed JAR contains neither the `lodsmoke` nor `rulessmoke` harness.
