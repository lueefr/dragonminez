# Content configuration and use conditions — NeoForge 1.21.1

## Automated verification

Java 21 `./gradlew build runGameTestServer --no-daemon --console=plain` passed with 22 unit tests and all 30 required GameTests. Final log: `/tmp/dmz-content-overhaul-sealed-validation.log`.

Coverage includes separate purchase/use gates, nested mastery/content conditions, regular and stack setters, separate group/form script setters, administrative forcing, qualified fusion references, contextual reward translation keys, additive composition, aura sealing, permanent mutant ownership and confirmed revocation. Schema tests cover repository-history fixtures, round trips, unknown fields, malformed files, concurrent edits, backup preservation and default-skill backfilling.

A packaged-client login exposed an additional serialization failure: Gson emits null optional visual sections when constructing adapter trees. The schema walker now treats null optional parents as unset; both schema and native adapter round-trip tests cover the case. GameTests without a real login did not expose this originally.

`scripts/audit-player-text.py` passes: 6,754 bundled keys, 267 component-bearing source files, 1,936 static component calls and 114 dynamic data calls. The audit checks direct literal component calls, duplicate/non-string source entries and missing static translation keys. It does not prove that every dynamic addon key has a translation. `scripts/check_lang.sh` also passes.

## Scope

The tests exercise the changed APIs and native integration paths. They do not establish complete gameplay parity, arbitrary addon compatibility or support for every historical external configuration. LOD/worldgen and the Fabric port are separate work. Test automation stays outside the distributed JAR.
