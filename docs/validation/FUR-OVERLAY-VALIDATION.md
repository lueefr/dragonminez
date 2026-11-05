# Fur/body-shadow layering validation — NeoForge 1.21.1

## Cause and change

The body musculature shadow and later appearance overlays used the same polygon depth bias. In the reproduced fur transformation, the translucent body shadow won depth ties and appeared on top of the fur.

`ModRenderTypes` now gives the human/Saiyan body-layer-one shadow textures a smaller depth bias (-5 units) than the later appearance overlays (-10 units). This keeps musculature above its base skin and beneath fur without camera-relative model scaling. Texture-based selection also preserves that depth class in transformation-mask, ritual-shell, afterimage and assimilation render-type conversion/cache handling.

## Visual evidence

Isolated Minecraft 1.21.1 runs use a Saiyan character, body type 1, front-facing third-person camera, and the bundled fur form. Render automation is temporary and is not distributed in the mod JAR.

- Before, ordinary rendering: `/tmp/dmz-render-smoke/game-noshader/screenshots/2026-10-24_16.06.18.png`.
- Before, Iris/Complementary: `/tmp/dmz-render-smoke/game-shader/screenshots/2026-10-24_16.07.40.png`.
- After, ordinary rendering: `/tmp/dmz-render-smoke/game-noshader/screenshots/2026-10-24_16.09.10.png`.
- After, Iris/Complementary: `/tmp/dmz-render-smoke/game-shader/screenshots/2026-10-24_16.10.38.png`.

The inspected after captures show red fur covering the arms and sides without the body-shadow patches, while the exposed chest retains its shading. The shader run uses Iris 1.8.0, Sodium 0.6.13 and Complementary Reimagined r5.9.2 with Mesa software rendering. This is evidence for the inspected combinations, not all GPUs/shader packs.

The GitHub Wiki was independently rewritten as an API-only usage manual. Rendering release notes and validation reports are kept out of the Wiki.

## Other model and ordinary-body checks

- The other bundled fur model was captured after the fix without shaders: `/tmp/dmz-render-smoke/game-noshader/screenshots/2026-10-24_16.13.23.png`.
- Reverting to the ordinary body retained its musculature shading: `/tmp/dmz-render-smoke/game-noshader/screenshots/2026-10-24_16.13.30.png`.
- The other fur model with Iris/Complementary: `/tmp/dmz-render-smoke/game-shader/screenshots/2026-10-24_16.14.53.png`.
- Ordinary body after reversion with Iris/Complementary: `/tmp/dmz-render-smoke/game-shader/screenshots/2026-10-24_16.15.00.png`.

Both model/reversion client sequences exited successfully. Derived-effect depth routing was reviewed in the diff; the captures above do not constitute separate visual tests of every afterimage, ritual or assimilation effect.

## Final artifact checks

`test build runGameTestServer` passed: six unit tests, ten required native GameTests, no unit-test failures/errors/skips. Log: `/tmp/dmz-fur-final-validation.log`. The packaged JAR was checked for temporary `portsmoke`/`lodclient` classes; none were present. No assets, progression values or save formats changed.
