# 2.2 Hotfix 7 validation

## Confirmed results

- Clean NeoForge 1.21.1 build succeeded. All 8/8 GameTests passed. Packaged JAR SHA-256: `63a7985d22ed1ca3fdf671b9c0d00652966d5f4a734550a775bce147c6f560be`.
- Aura outlines and sparks stayed aligned with the player in screenshots captured without shaders and with Iris plus Complementary Shaders r5.9.2. The muscle texture remained visible with `bodyType=1` in both checks.
- Exact round-trip comparison passed for all 348 structure-template NBT files.
- Distant Horizons 3.3.1 with DH Seedgen 0.8.0 using server generation generated Otherworld chunk `(-1, 61)`. The full chunk contained an ANDESITE landmark at `(-1, 162, 980)`. Its decoded SeedGen FullData column did not contain that block at the landmark's expected height, so the tested SeedGen path does not represent the landmark correctly in LOD data.
- Distant Horizons 3.3.1 native generation was tested with `chunkGeneratorMode=INTERNAL_SERVER` and `generatorPlan=CHUNKS_ONLY`; DH SeedGen was loaded but disabled (`enabled=false`, `serverGeneration=false`). The full chunk again contained ANDESITE at `(-1, 162, 980)`. The persisted FullData cell decoded successfully and its mapping included the ANDESITE state, but local column `(63, 20)` had AIR at Y 162–163 and ANDESITE only at Y 226–228; the point above the target was Otherworld cloud at Y 163–174. Thus this native path also did not represent the landmark at its correct vertical position in the tested LOD column.

## Interpretation limits

Both Distant Horizons 3.3.1 server-generation routes were tested: DH SeedGen 0.8.0 using server generation, and native `INTERNAL_SERVER`/`CHUNKS_ONLY` generation with SeedGen disabled. In both, the full chunk had the landmark, but decoded LOD FullData did not place it at the expected height. The native test had no player and captured no visual overlay, so it does not establish visual LOD compatibility. Voxy, Voxy SeedGen, other SeedGen versions, other addons, and universal LOD compatibility were not tested. An addon that skips custom chunk features may also omit landmarks.

Cached LOD data may need rebuilding through the relevant addon's own tools; this update does not call for deleting world saves or region files.
