# Aura type transitions — NeoForge 1.21.1

The renderer previously blended numeric style parameters but immediately replaced the geometry/texture selected by the aura type. It now crossfades outgoing and incoming types over 0.4 seconds with smoothstep easing. Each layer/type keeps its own motion and style state, so an outgoing shape does not inherit the incoming shader style. Rapid reversals restart from the current visible weights, without resetting opacity.

The shared path serves world, first-person and GUI auras. It is type-agnostic, covering the three built-in 3D types and valid 2D texture types, plus stack/extra layer appearance and removal. Ground pulses crossfade with same-layer geometry changes while advancing their animation once per frame. Switching the entire 2D/3D renderer or GUI preview owner resets that cache. Existing aura growth, gameplay state, JSON and save formats are unchanged.

## Verification

- Java 21 `build runGameTestServer`: successful. Nine unit tests (including three new aura transition tests) and all 26 required GameTests passed. Log: `/tmp/dmz-aura-transition-final-validation.log`.
- Unit tests cover every directed pair of six identifiers, conserved opacity, monotonic interpolation, immediate reversal, a third target during an unfinished blend, independent layer addition/removal, pause and cache reset.
- Packaged-JAR client: all six directed changes among smooth/god/sparking, both 2D texture directions, rapid 2D reversals and first-person 3D changes. Native renderer probes recorded intermediate opacities, e.g. outgoing god 0.5667 / incoming smooth 0.4333. Captures include `2026-10-25_02.22.50_2.png` and final types. Evidence: `dragonminez-port-tools/aura-transition-smoke/candidate.log` and `game/screenshots/`.
- Iris/Sodium with Complementary Unbound r5.9.3 also completed the same sequence. Native mixed weights were observed for 3D, 2D and a three-way retarget in first person. Evidence: `dragonminez-port-tools/aura-transition-shader-smoke/candidate.log`; the shader pack is confirmed active. Software rendering can skip intermediate frames at low FPS; the frame-rate-independent transition still finishes at its target.
- Client automation installs generic temporary form definitions and samples the actual renderer cache. It does not replace production rendering or alpha values. Automation is injected only into JAR copies outside the distributed build.

## Release publication

The previous three fixes were merged in PR #17 but no release was published. Release preparation only triggers on changes to `gradle.properties`, while publication requires a separate dispatch. This patch keeps the mod version at 2.2 and publishes Hotfix 10 with those fixes and the aura transition. No workflow or external notification service is changed.
