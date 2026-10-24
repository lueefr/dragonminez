# NeoForge 1.21.1 extension API validation

Scope: `feature/2.2-neoforge-1.21.1-api`. LOD, worldgen and rendering changes are maintained on a separate branch.

## Checks from the combined development snapshot

- Six unit tests passed: recursive form composition, progression/config invalidation, current-form anchors, cycle rejection, player isolation and translation fallbacks.
- Eleven GameTests passed. Three extension tests cover completion events/flags, save/load and cloning, native quest completion, legacy history migration without replaying events, NPC cosmetic persistence and authoritative shop transactions. Eight other tests included the existing port/worldgen checks.
- A full build passed before the branch split. These historical checks do not replace validation of the separated API branch.

## Independent API branch checks — 2026-10-24

- `./gradlew test build runGameTestServer` passed after removing the LOD/worldgen/rendering changes.
- Six unit tests and eight required GameTests passed (five base port tests and three extension tests).
- Log: `/tmp/dmz-api-split-validation.log`.
- Compared all 49 API source/test/build files against the pre-split snapshot; source partitioning preserved their contents exactly. LOD-only code/assets are absent from this branch's diff against main.

## Additional native progression and service checks

- Nine required GameTests passed after adding a real StatsData composition scenario. It loads JSON groups by internal groupName despite differing filenames, checks a best-based inheritance chain, unlocks a stronger source through content completion, changes its multiplier via config reload, and verifies a current-based source after save/load.
- Shop coverage now exercises both alignment gates and integer prices above float's exact-integer range. Rejected purchases preserve the balance; successful large-price purchases preserve the exact remaining integer points.
- The cosmetic persistence test also checks that the configured NPC translation key is used for its name.
- Log: `/tmp/dmz-api-native-validation.log`.

## Client checks — 2026-10-24

- An isolated Minecraft client rendered the three custom appearance bases and cosmetic armor with hair disabled. The custom services screen opened and displayed configured names, translated dialogue, skill and quest actions.
- The actual ClientLanguage hook and an already cached Component resolved a new remote translation, refreshed when it changed, and fell back to the raw key after clearing remote translations.
- Screenshots: `/tmp/dmz-api-visual/game-client/screenshots/2026-10-24_12.26.22.png` and `/tmp/dmz-api-visual/game-client/screenshots/2026-10-24_12.26.25.png`. Log: `/tmp/dmz-api-visual/run-no-hair-control.log`.
- Hair-enabled runs did not finish the capture and were interrupted. A standalone geometry probe emitted finite, bounded meshes for presets 1–3 (2,184–5,880 vertices, 0–7 ms in the inspected runs), so these results do not establish an unbounded CPU mesh or prove a GPU fault. The hair layer now captures head transforms during bone traversal and emits geometry afterward, matching the player renderer's buffer lifetime; hair-enabled runtime validation remains unresolved.
- `./gradlew build` passed after the renderer/name changes; log: `/tmp/dmz-api-visual/build.log`. Client automation and diagnostic probes exist only in temporary test copies, not in the distributed JAR.

## Remaining verification

- Check skill/quest offerings and placement unload/reload.
- Resolve and capture hair-enabled NPC rendering; verify appearance defaults and armor/hair combinations.
- Verify translation snapshots across separate multiplayer clients and actual disconnect events (local component refresh/clear checks passed).
- Run the relevant multiplayer and gameplay checks before merging.

No release or merge is justified solely by the historical combined-branch checks.
