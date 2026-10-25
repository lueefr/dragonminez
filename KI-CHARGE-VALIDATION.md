# Ki charge anchoring — NeoForge 1.21.1

## Behavior

Charging blast and wave offsets use an upright body frame. Pitch does not move the charge, and its growing radius does not push it toward the crosshair. Horizontal offsets turn with the caster; overhead offsets remain directly above the caster. Height and width scaling remain supported.

The existing fired-projectile placement, aiming and continuous-beam steering remain unchanged. The large overhead blast no longer needs a negative forward offset to cancel the former automatic clearance.

No configuration, API, save-data or network schema changes are required.

## Verification

- Java 21 `build runGameTestServer`: successful; six unit tests and all 24 required GameTests passed. Log: `/tmp/dmz-ki-charge-validation.log`.
- Two new GameTests exercise overhead charging across yaw/pitch, growth, caster movement and scaling; hand charging across pitch, growth and yaw; and the released beam origin along the original aim direction.
- A packaged-JAR client reproduced the defect against Hotfix 8, then exercised the fix in first and third person. The overhead charge remained at `(0.5, 87.4, 0.5)` while the player at `(0.5, 81, 0.5)` changed yaw and pitch. The hand charge retained Y=82 through upward/downward aim changes. The released beam resumed aim-relative placement.
- Isolated client evidence: `dragonminez-port-tools/ki-charge-smoke/{baseline,candidate}.log` and its `game/screenshots/` directory. Candidate captures include `2026-10-24_21.31.01.png` (overhead charge, third person) and `2026-10-24_21.31.24.png` (released beam).

- The same sequence also ran with Iris `1.8.14-beta.1+mc1.21.1`, Sodium `0.8.13+mc1.21.1` and Complementary Unbound `r5.9.3`. Evidence: `dragonminez-port-tools/ki-charge-shader-smoke/candidate.log`; the log confirms the shader pack is active. Overhead and hand charge positions match the vanilla-renderer run.

The client fixture calls the native setup methods and samples synchronized entity positions; it does not replace production positioning or rendering. Automation classes are injected only into a temporary JAR copy. Tests cover representative blast/wave charging and release, not every technique in every modpack.
