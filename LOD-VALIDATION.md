# Otherworld / distant terrain validation — 2026-10-25

Branch: `fix/2.2-neoforge-1.21.1-lod`, updated with `main` through PR #22. Minecraft 1.21.1, NeoForge 21.1.238, Java 21. Mod version remains 2.2. The final packaged candidate passed the Neo-Voxy and DH matrices below.

## Generation

Bundled MCA terrain is no longer imported or repaired at startup. Existing saved regions and player changes remain untouched. Fresh chunks use the published vanilla density graph, surface rules and per-chunk feature pipeline. The original saved `(320,320)` column had no clouds above Y 96; fresh generation has two separated decks there.

All 348 fixed landmark templates retain explicit air and block-entity NBT. The feature accepts the Otherworld dimension or the Otherworld noise-settings key in an auxiliary worldgen level. Unrelated generators are rejected.

The tournament schematic now also runs during `FEATURES`, with immutable chunk-local columns shared with the existing incremental builder. Runtime progression readiness, protection bounds and NPC placement retain the existing builder. Unfinished builder tickets and live level references are released at server shutdown. A fresh client test generated STONE_BRICKS at `(0,354,-1200)` before any player entered Otherworld, with `playerCount=0` and `runtimeBuilt=false`.

## Automated checks

- Java 21 `build runGameTestServer`: 26 unit tests and all 43 required GameTests passed, including three new tournament schematic tests. Initial arena log: `/tmp/dmz-lod-arena-release-build.log`; initial stencil log: `/tmp/dmz-lod-stencil-build.log`. Final packaged validation: `/tmp/dmz-lod-stencil-final-build.log`.
- Read-only template comparison passed: 348 templates, 4,831,278 positions (4,244,741 air, 586,537 non-air), 22 block entities, negative chunks and the complete index/file set. Log: `/tmp/dmz-lod-arena-templates-check.log`.
- Translation audit: 6,786 bundled keys, 272 files, 2,063 static calls and 115 dynamic data calls. Its four Python regression tests passed; bundled language consistency passed.
- All 14 release-tool tests passed. `git diff --check` passed.

## Distant Horizons 3.3.3

An isolated client used the final packaged stencil candidate, Sodium 0.8.13 and the supplied unmodified DH 3.3.3 JAR, without SeedGen. The world was a disposable copy of the earlier freshly generated comparison world. No user world was modified. Settings: vanilla distance 4, DH distance 32, internal-server generation, CHUNKS_ONLY plan and two generator threads. Software OpenGL ran on a separate virtual display.

The native terrain-data API, read off the render thread with a fresh cache per query, confirmed:

- Palace: LIME_TERRACOTTA at `(0,160,0)`; distant palace visible in captures.
- Complete `(320,320)` column above Y 96: exact solid/air match, eight lower-deck cloud blocks, thirteen upper-deck cloud blocks and the empty gap preserved.
- Negative-coordinate landmark: ANDESITE at `(-1,162,980)`.
- Tournament: STONE_BRICKS at `(0,354,-1200)`; the arena is visible in the capture.
- After resizing to 1100×680 and travelling Otherworld → Overworld → Otherworld, the palace data and distant image returned correctly.

The run emitted `DH_FINAL_PASS` and finished with `BUILD SUCCESSFUL` and normal shutdown. The harness waited for actual API completion rather than treating a short fixed delay as a pass or failure. Recorded delays include software rendering, generation and harness query timeouts; this is not a performance benchmark.

Log: `/home/lucas/Dev/dragonminez-port-tools/lod-dh-smoke/stencil-matrix-final.log`. Captures in that harness's `game/screenshots/`:

- `2026-10-25_21.31.34.png`: distant palace.
- `2026-10-25_21.32.05.png`: cloud scene and complete-column readback.
- `2026-10-25_21.32.36.png`: negative-coordinate landmark scene.
- `2026-10-25_21.33.06.png`: tournament arena.
- `2026-10-25_21.34.12.png`: distant palace after the round trip.

The complete matrix passed twice: initially on the arena candidate (`matrix-columns.log`, SHA-256 `6fdab397961cc88472c4b2349f4c8cc66bd0ce5479a097f0274b281aa51378dd`), then again on the final stencil candidate documented here. The final run also emitted `DH_FINAL_PASS` and ended normally with `BUILD SUCCESSFUL`.

## Neo-Voxy alpha.12: renderer regression

Tests use the supplied unmodified Neo-Voxy `0.5.0-alpha.12-mc1.21.1-neoforge` and Sodium 0.8.13, without SeedGen. Voxy alone does not generate unseen chunks; the harness generates normal full chunks and explicitly ingests them through Voxy's own API. This is not a claim of automatic unseen-terrain generation.

Full-chunk cache readback preserves the palace, ANDESITE fixture and both cloud decks with the empty gap. Earlier cached-world captures (`2026-10-25_17.16.00.png`, `17.16.20.png`) showed the distant palace. Fresh-world rendering and window resizing exposed a repeatable failure: geometry was prepared and present in the cache, but all visible-section/quad counts dropped to zero.

A current-candidate comparison reproduced the trigger precisely. Opening the saved fresh world showed the distant palace at vanilla distance 4 (`2026-10-25_21.18.25.png`, 15,152 green roof pixels and 6,336 red gate pixels in the central crop). Resizing the window made it disappear. Restoring the stencil write mask to 255 once, without changing the world, cache, Voxy JAR or renderer, immediately restored the distant palace.

DragonMineZ's aura cleanup left stencil writes disabled. Voxy's depth/stencil initialization then could not initialize newly allocated stencil data as expected. The production fix scopes and restores stencil state in aura cleanup, aura model rendering and the aura render-state layer, including separate front/back masks, comparisons, operations and test enablement. It restores Minecraft's cached state and native GL state; no third-party JAR or Voxy renderer workaround is shipped.

Pre-fix logs: `fresh-raw-readback.log`, `fresh-native-readback.log`, `reopen-fresh-cache.log` under `/home/lucas/Dev/dragonminez-port-tools/lod-voxy-smoke/`. Manual near-camera diagnostic captures do not count as distant passes.

The final stencil candidate passed in a new disposable world in `stencil-fresh-final.log`, without manual camera changes, renderer reloads, debug agents or mask resets:

- Native GL assertions preserved different front/back write masks, the comparison/reference and test enablement after aura cleanup and render-state setup/cleanup.
- Before entry: arena generation passed with no Otherworld players and no completed runtime build.
- Fresh distant view at `(80,210,-110)`, yaw 15 / pitch 18, vanilla distance 4: 15,332 green roof pixels and 6,171 red gate pixels.
- After resizing to 1100×680: 29,220 green and 10,933 red pixels.
- After Otherworld → Overworld → Otherworld: 29,169 green and 10,970 red pixels. The harness also requested aura activation on return.
- Each image required empty ingest/model/mesh queues, actual visible Voxy sections, the fixed distant camera, and successful cache readback of the palace, negative landmark and arena.
- `LOD_FINAL_PASS` and `BUILD SUCCESSFUL`; normal shutdown. Captures: `2026-10-25_21.28.37.png`, `21.29.00.png`, `21.29.28.png` in the Voxy harness's `game/screenshots/`.

The pre-fix cached-world image and the post-fix fresh-world image are distinct tests. The passing final run does not rely on the earlier manual diagnostic probes.

## Limits and artifact

Existing Minecraft chunks and addon LOD databases are not automatically rewritten. Saved terrain can retain differences from current generation. The supplied Voxy SeedGen 0.3.0 pair previously failed in its required far-plane injection; no third-party JAR was patched, and compatibility with that pair is not claimed.

Final stencil candidate SHA-256: `b3eb1a8b4df31690d93de19a564b60ae2ea4286318d063d889c644b2396139a3`. The distributed JAR contains neither `lodsmoke` nor `rulessmoke` harness classes. Historical combined-branch checks remain in `HOTFIX-VALIDATION.md`; they do not replace this branch's tests. Developer diagnostics stay in this repository, outside the customization wiki.
