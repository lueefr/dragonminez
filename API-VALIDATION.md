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

## Remaining verification

- Check native composition with actual player progression and active forms.
- Check NPC alignment, skill/quest offerings and placement unload/reload.
- Capture NPC rendering with hair and cosmetic armor.
- Verify client translation refresh and disconnect behavior.
- Run the relevant multiplayer and gameplay checks before merging.

No release or merge is justified solely by the historical combined-branch checks.
