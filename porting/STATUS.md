# Fabric 26.1.2 migration status

Status: **incomplete; no playable JAR and no Fabric release**.

## Source and target

- Branch: `port/2.2-fabric-26.1.2`.
- Starting point: the existing NeoForge 1.21.1 port at `08ad003e`.
- Original 2.2 reference: official Forge 1.20.1 source at `d824a29c`.
- Target: Minecraft 26.1.2 / Fabric, Java 25, mod version 2.2.
- Target dependencies are resolved by Loom. The original NeoForge build file is retained under `reference/` for migration comparison.

## Verified independent code

`./gradlew verifyExtensionApiCore` passes with Java 25 against the target Minecraft dependencies:

- Completion counters and flags: NBT round trip, player-data copy, identifier validation.
- Form composition: recursive chains, player isolation, memoization, dynamic base changes, config replacement, dynamic self-exclusion, static cycle rejection, current-form resolution.
- Config translations: local language, English and raw-key fallback; synchronized overlay; atomic rejection of invalid data.
- Custom NPC definitions: separate directories, placement references, duplicate rejection and atomic config replacement.

These tests compile only the independent API classes. They do not compile the platform adapters or the complete mod, and must not be reported as gameplay validation.

## Source integration started

- Player stats save/load/copy and quest/ritual completion hooks include generic lifetime completion flags.
- Form JSON has content-completion requirements, dynamic base references and multiplier inheritance. Transformation/skill gates and a player-owned composition adapter are wired in source.
- Config reload validates static form graphs. Skill/mastery mutation revisions support player-owned caches.
- Config language maps are included in config synchronization. Client language lookup and translated-component cache invalidation mixins are added.
- Custom NPC definitions load independently of existing NPC config. Runtime custom entities/rendering/services/spawn commands are still absent.

## Full mod port still required

The full `compileJava` task fails, reaching its diagnostic limit. In particular:

- Replace NeoForge bootstrap, registry/deferred-holder APIs, lifecycle events and gameplay events with Fabric registration, callbacks and mixins.
- Port all player storage/respawn/dimension/tracking hooks and the complete network transport, preserving authoritative checks and all progression data.
- Finish Minecraft NBT/value-input/value-output, entity, item/equipment, block, recipe and worldgen API migration.
- Migrate GeckoLib 4 behavior/models/controllers/renderers to GeckoLib 5. Class/package renames alone are insufficient.
- Migrate render states, render submissions, player layers, hair, GUI extraction, shaders and all client mixins. The old render pipeline is not compatible.
- Replace or port required loader-specific integrations such as accessories and biome integration. Optional integrations must be evaluated individually.
- Implement the additive custom NPC entity, validated assets/cosmetics, server-authoritative services, independent placement persistence and spawn command.
- Validate all original mechanics in a dedicated server and client, including progression, transformations, skills, quests/sagas, masters, rituals, tournaments, raids, collectible/wish systems and dimensions.
- Add a working Fabric initializer and valid metadata after lifecycle initialization is actually migrated.

No existing mechanic or source directory has been excluded from the main compilation to obtain a successful artifact. No stub gameplay handlers have been substituted for missing mechanics. The branch-specific workflow checks the independent APIs and the complete build. It does not publish releases while the port is incomplete. Release publication with the required `26.1.2-fabric` name and the existing source-version hotfix sequence is still pending.
