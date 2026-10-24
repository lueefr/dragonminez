# 2.2 Hotfix 7 validation

## Confirmed results

- Clean NeoForge 1.21.1 build succeeded. All 8/8 GameTests passed. Packaged JAR SHA-256: `63a7985d22ed1ca3fdf671b9c0d00652966d5f4a734550a775bce147c6f560be`.
- Aura outlines and sparks stayed aligned with the player in screenshots captured without shaders and with Iris plus Complementary Shaders r5.9.2. The muscle texture remained visible with `bodyType=1` in both checks.
- Exact round-trip comparison passed for all 348 structure-template NBT files.
- Distant Horizons 3.3.1 with DH Seedgen 0.8.0 generated Otherworld chunk `(-1, 61)`. The full chunk contained an ANDESITE landmark at `(-1, 162, 980)`, and the full chunk plus a matching Distant Horizons FullData cell persisted.

## Interpretation limits

The Distant Horizons result applies to version 3.3.1 with DH Seedgen 0.8.0 in the tested setup. Voxy, Voxy Seedgen, other Seedgen versions, and universal LOD compatibility were not tested. An addon that skips custom chunk features may still omit landmarks.

The FullData cell was confirmed to persist alongside the generated full chunk, but its blob was not decoded to verify that it represents the exact ANDESITE landmark visually. Cached LOD data may need rebuilding through the relevant addon's own tools; this update does not call for deleting world saves or region files.
