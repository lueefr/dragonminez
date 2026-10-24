# Rendering regression validation — NeoForge 1.21.1

The patch addresses the nine screenshots reported on October 24, 2026. Images 3/4 describe the same vehicle-scale regression; images 7/8 describe the same armor-texture regression.

| Area | Change and verification |
| --- | --- |
| Gathered dragon balls | Removed the enlarged additive copy of the overlapping cube mesh. A single textured pass retains full-bright lighting and a smooth brightness pulse. Client probes confirmed all seven blocks had `gathered=true`; successive vanilla and shader captures cover the active glow. |
| Summoning clouds | Custom cloud passes use the callback's actual model-view matrix. Captured the darkened Overworld sky at two camera orientations, and checked the Iris path. Configured cloud heights are unchanged. |
| Vehicles | Use GeckoLib 4.9's six-argument rotation hook so configured scale, offset and tilt are applied. Captured the capsule empty and occupied; its configured 2.5 scale now matches the rider. |
| Lock-on | Use one explicit view/projection transform and a two-sided textured quad. The old immediate draw multiplied the view a second time; face culling also discarded the icon. Final captures exercise first-person and third-person cameras. |
| Ground aura | Raycast against collision shapes beneath interpolated feet; render at the hit surface while following X/Z. Both 2D and 3D captures show the pulse at 1.5 blocks, its disappearance at 2.1 blocks, and its return at a new horizontal position. The body aura stays active. |
| Armor | Correct NeoForge's texture override signature and route GeckoLib armor layers through `ClientHooks.getArmorTexture`, including NPCs. All 291 registered mod armor pieces passed normal/damaged worn-texture checks: 582 checks, no missing textures. Captures cover standard armor, damaged armor, cape armor, the patroller model, a female body, inventory preview and vanilla iron armor. |
| Eyebrows | Omit the regular brow layer for the existing browless hair type, including extracted skin-pixel brows. Keep the skin-colored face detail and respect player-owned editable hairstyles. Captured the face close-up with and without the shader pack. |

## Checks and evidence

- Java 21 build, six unit tests and all 22 required GameTests. Final run: `/tmp/dmz-render-release-validation.log`.
- The new ground-projection GameTest covers standing, hovering at 0.5/1.99/2 blocks, rejection at 2.01 blocks, a slab and horizontal movement into empty space.
- Packaged-JAR clients use isolated directories under `/home/lucas/Dev/dragonminez-port-tools/`; the temporary automation classes are injected into copies, never the distributed JAR.
- `render-smoke/baseline.log`: reproduction against Hotfix 7, including missing armor-material textures, the small capsule and absent lock-on marker.
- `render-smoke/candidate.log` and `reviewed.log`: complete scene sequence and the 582-texture audit.
- `render-targeted/run.log`: explicit 3D/2D aura checks on a contrasting floor, with ground hit positions, active pulse timestamps and visual captures. First 3D hover capture: `2026-10-24_20.52.28.png`; first 2D hover capture: `2026-10-24_20.53.14.png`. The subsequent above-limit captures omit the ground pulse. Equipment packets synchronize the fixture's server-selected armor to the local player.
- `render-targeted/lock-final.log`: final camera alignment checks after removing the duplicate view transform; screenshots `2026-10-24_20.56.28.png` (first person) and `2026-10-24_20.56.34.png` (third person).
- `render-shader-smoke/candidate.log`, `reviewed.log` and `lock-final.log`: Iris `1.8.14-beta.1+mc1.21.1`, Sodium `0.8.13+mc1.21.1` and Complementary Unbound `r5.9.3`, copied from local installations. Logs confirm the shader pack was active.

The clients run on Mesa software OpenGL in Xvfb. These checks cover the reported scenes and the named shader combination; they cannot establish compatibility with every GPU, shader pack or optional mod. The separate LOD/world-generation branch and Fabric port are outside this patch. No world-generation, progression, JSON schemas or player saves are changed here.
