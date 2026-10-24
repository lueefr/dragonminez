# 2.2 Hotfix 7 validation

## Confirmed results

- Clean NeoForge 1.21.1 build succeeded. All 8/8 GameTests passed. Packaged JAR SHA-256: `63a7985d22ed1ca3fdf671b9c0d00652966d5f4a734550a775bce147c6f560be`.
- Aura outlines and sparks stayed aligned with the player in screenshots captured without shaders and with Iris plus Complementary Shaders r5.9.2. The muscle texture remained visible with `bodyType=1` in both checks.
- Exact round-trip comparison passed for all 348 structure-template NBT files.
- Distant Horizons 3.3.1 with DH SeedGen 0.8.0 using server generation generated Otherworld chunk `(-1, 61)`, whose full chunk contained an ANDESITE landmark at `(-1, 162, 980)`. The SeedGen FullData is being rechecked with the dimension's vertical coordinate offset; its landmark representation remains unconfirmed.
- Distant Horizons 3.3.1 native generation was tested with `chunkGeneratorMode=INTERNAL_SERVER` and `generatorPlan=CHUNKS_ONLY`; DH SeedGen was loaded but disabled (`enabled=false`, `serverGeneration=false`). The full chunk contained ANDESITE at `(-1, 162, 980)`. In decoded FullData local column `(63, 20)`, raw vertical coordinates are relative to Otherworld's `minY=-64`: ANDESITE at raw Y 226–228 maps to world Y 162–164, matching the landmark at world Y 162–163. Other exposed blocks in the column also align with the template. This establishes that the inspected native FullData coordinates are consistent with the landmark after normalization, but no visual DH overlay was captured.

## Interpretation limits

The native Distant Horizons 3.3.1 `INTERNAL_SERVER`/`CHUNKS_ONLY` FullData inspection is consistent with the landmark after normalizing its vertical coordinates by Otherworld's `minY=-64`. This was a server-only test with no player and no visual overlay capture, so it does not establish visual LOD compatibility. DH SeedGen 0.8.0 FullData is still being rechecked with the same normalization; no conclusion about its landmark representation is recorded yet. Voxy, Voxy SeedGen, other SeedGen versions, other addons, and universal LOD compatibility were not tested. An addon that skips custom chunk features may omit landmarks.

Cached LOD data may need rebuilding through the relevant addon's own tools; this update does not call for deleting world saves or region files.

## Extension API development branch — 2026-10-24

Branch: `feature/2.2-neoforge-1.21.1-api`. These results describe development work after the original Hotfix 7 checks above; the original JAR checksum does not identify this API build.

- `compileJava test` passed, including five form-composition tests and one config-translation test. The tests cover recursive dynamic composition, progression/config invalidation, persisted current-form anchors, cycle rejection, per-player state, and translation fallbacks.
- `runGameTestServer` passed all 11 required tests. New integration coverage checks completion events carrying the player, JSON completion requirements, repeated completion counts, save/load and clone persistence, native quest completion, lifetime flags surviving quest resets, migration of existing quest history without replaying events, and custom NPC definition/cosmetic equipment persistence. The additional shop test passed valid purchases and rejected forged offer indices, insufficient balance, full inventory and out-of-range interaction without charging for failed purchases.
- A subsequent full `build` passed and produced `build/libs/dragonminez-1.21.1-2.2.jar`.
- An isolated Minecraft client with the supplied DH 3.3.3 and DH SeedGen 0.9.1 loaded both optional compatibility mixins. SeedGen logged that its surface approximation is disabled specifically for Otherworld. The client completed its run. Screenshot `/tmp/dmz-lod-client-api/game-client/screenshots/2026-10-24_11.15.10.png` shows the bridge and terrain again, but false walls remain at the sides. This is a partial improvement, not a passing LOD validation. A second run restricting native DH generation to complete columns in Otherworld produced `/tmp/dmz-lod-client-api/game-client/screenshots/2026-10-24_11.20.27.png`: the false side walls are absent, with the bridge, planet and separated cloud decks visible. Both clients completed successfully. This validates that comparison scenario, not every distance, camera position or LOD implementation.
- Custom NPC alignment-specific transactions, placement reload/unload behavior, rendered hair/armor, multiplayer translation refresh, full gameplay parity, Voxy and Voxy SeedGen still require further verification. No release or merge is justified by these checks alone.

Local diagnostic logs: `/tmp/dmz-api-resume-test.log`, `/tmp/dmz-api-resume-gametest.log`, `/tmp/dmz-api-resume-build.log`, and `/tmp/dmz-lod-client-api/run.log`. Temporary diagnostic paths are not distributed artifacts.

### Supplied Voxy pair diagnostic

Neo-Voxy `0.5.0-alpha.12-mc1.21.1-neoforge` plus Voxy SeedGen `0.3.0` failed before the visual comparison. The failing third-party injection is `voxyseedgen.mixins.json:VoxyFarPlaneMixin`, method `voxyseedgen$reachAsFarAsWeDraw`: its required `48000.0f` constant is absent from Neo-Voxy's `computeProjectionMat`. Bytecode inspection confirms that method computes its projection from float parameters. This combination is not validated or claimed compatible. The diagnostic does not establish that DragonMineZ caused the third-party injection failure; the separate Voxy-only run completed successfully and captured `/tmp/dmz-voxy-client-api/game-client/screenshots/2026-10-24_11.32.06.png`. That screenshot does not show the distant landmark or establish distant cloud/structure coverage; it is a launch/gameplay smoke check only. Original supplied JARs were not modified.

The isolated Gradle runtime also needed Sodium's nested modules and common library laid out separately to avoid selecting its library wrapper as the Sodium mod. This diagnostic layout is temporary and is not included in DragonMineZ releases. Logs: `/tmp/dmz-voxy-client-api/run-sodium-dev-layout.log` and `/tmp/dmz-voxy-client-api/run-voxy-alone.log`.
