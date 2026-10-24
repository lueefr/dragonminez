# Rule API validation — NeoForge 1.21.1

- Java 21 `./gradlew build runGameTestServer`: passed. Six unit tests and all 21 required GameTests passed. Final server log: `/tmp/dmz-data-rules-reviewed-validation.log`.
- Added integration coverage for first-only permanent form/skill-level rewards, mastery, exact whole TP, chained content, deferred Java events, rollback on a failed chain, purchase refusal before debit, first-use persistence/cloning, player-owned hair permissions, NPC location without distant chunk generation, invalid configs and forearm UV/geometry preservation.
- `scripts/check_lang.sh`: passed for all language JSON files.
- Isolated Minecraft client loaded a copy of the packaged JAR with a temporary automation class. The distributed JAR does not contain that harness. Client evidence is under `/home/lucas/Dev/dragonminez-port-tools/rules-smoke/`.
- Client assertions verified that the JSON's hairstyle was ignored for an editable form, and that first-only outline/lightning overrides were enabled on the first session and disabled on the second. Screenshots also show enlarged forearms without a custom body model, a local external fur mask, separate fur/eye-marking/tail colors, both bundled fur layouts, the filtered form selector and the strand editor.
- Initial complete client run: `run.log`, screenshots `2026-10-24_20.02.53.png` through `2026-10-24_20.03.19.png`. Repeats are logged in `run-reviewed.log` and `run-final.log`; the final run also verifies the translated form label in screenshots `2026-10-24_20.16.12.png` and `2026-10-24_20.16.16.png`. The final UI build passed in `/tmp/dmz-data-rules-final-build.log`.

The visual checks use the vanilla rendering path with software OpenGL. They do not establish shader-pack compatibility or full gameplay/optional-mod parity. PNG textures remain client-local. Historical activations before this feature cannot be reconstructed; only a legacy save's currently active forms are seeded as previously used.

The separately reported armor, dragon-ball glow, summoning-cloud, vehicle-scale, lock-on marker, ground-aura projection and eyebrow regressions are outside this API patch and remain scheduled for a separate fix branch.
