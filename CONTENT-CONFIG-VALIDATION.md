# Content configuration and use conditions — NeoForge 1.21.1

## Automated verification

Java 21 `./gradlew build runGameTestServer --no-daemon --console=plain` passed with 22 unit tests and all 30 required GameTests. Final log: `/tmp/dmz-content-overhaul-sealed-validation.log`.

Coverage includes separate purchase/use gates, nested mastery/content conditions, regular and stack setters, separate group/form script setters, administrative forcing, qualified fusion references, contextual reward translation keys, additive composition, aura sealing, permanent mutant ownership and confirmed revocation. Schema tests cover repository-history fixtures, round trips, unknown fields, malformed files, concurrent edits, backup preservation and default-skill backfilling.

A packaged-client login exposed an additional serialization failure: Gson emits null optional visual sections when constructing adapter trees. The schema walker now treats null optional parents as unset; both schema and native adapter round-trip tests cover the case. GameTests without a real login did not expose this originally.

`scripts/audit-player-text.py` passes: 6,754 bundled keys, 267 component-bearing source files, 1,936 static component calls and 114 dynamic data calls. The audit checks direct literal component calls, duplicate/non-string source entries and missing static translation keys. It does not prove that every dynamic addon key has a translation. `scripts/check_lang.sh` also passes.

## Packaged client verification

- Vanilla renderer: `dragonminez-port-tools/content-overhaul-smoke/reviewed.log` passed login, Portuguese refusal/reward names, per-form black/light-blue text colors, OK dismissal and aura transitions in third person, first person and 2D. Screenshot `2026-10-25_12.57.09.png` shows the native dialog.
- Iris/Sodium with active Complementary Unbound r5.9.3: `dragonminez-port-tools/content-overhaul-shader-smoke/sealed-final.log` passed the same sequence on the final JAR, plus stopped-loop assertions in all three sealed phases. Screenshot `2026-10-25_13.01.42.png` shows the dialog with the shader active. Absorption samples include factors 0.844, 0.5 and 0.156 before zero; same-type form/color changes retain mixed outgoing/incoming layers.
- Aura particle spawning, continuous sound and aura-driven charge shake now consult visual overrides. Existing emitted particles retain their normal lifetime. Lightning and outlines remain independently configurable.
- These are software OpenGL runs in isolated disposable worlds. They establish the tested renderer/configuration paths, not every shader pack or hardware combination. Temporary automation is injected only into external copies of the JAR.

## Scope

The tests exercise the changed APIs and native integration paths. They do not establish complete gameplay parity, arbitrary addon compatibility or support for every historical external configuration. LOD/worldgen and the Fabric port are separate work. Test automation stays outside the distributed JAR.
