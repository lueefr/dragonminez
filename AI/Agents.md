# AI Agent Onboarding: DragonMineZ

This is the first local AI entry point for DragonMineZ work. Read this before inspecting code, planning changes, or answering implementation questions. Then read `AI/Memory.md` before acting.

## Operating Rules

- Work from the local repository first. Do not rely on memory when repo files can answer the question.
- Follow the user request exactly. If the user asks for a draft, review, or explanation, do not edit files.
- If you edit, add, delete, or regenerate files, stage changed tracked files before finishing unless the user explicitly says not to stage that file.
- Treat the working tree as shared. Never discard, overwrite, or revert user changes unless explicitly instructed.
- Do not assume backward compatibility is required unless the user asks for it or the change affects public data, save data, player-editable JSON, configs, or addons.
- When backward compatibility is not requested, prefer replacing old code with the new approach instead of layering parallel legacy paths.
- Ask when requirements are ambiguous, destructive, risky, or when compatibility expectations affect the design.
- Preserve user-created content. Many JSON/config systems are intentionally editable by players, addon authors, and server owners.
- Avoid destructive commands such as `git reset --hard`, forced checkout, broad cleanup commands, or recursive deletion.
- Do not edit generated/runtime folders unless the task explicitly targets them: `build/`, `run/`, `run-data/`.
- Keep responses practical, direct, and evidence-based. Avoid vague claims.

## Agent Orchestration

This project may be worked on with a single agent or with an orchestrator plus subagents, depending on how the user started the session. Do not assume a specific AI model, vendor, or quota: the user configures those per session outside this document.

- The main session is the orchestrator. It plans, decomposes, delegates, and verifies. It does not write large implementations directly when subagents are available.
- Workers execute: implementation, exploration, reviews.
- Roles: explorer for read-only codebase mapping before large changes; worker for implementation in closed scope; reviewer for diff checks before reporting done.
- Subagent definitions live in the agent configuration directory of the tool in use (for example `.codex/agents/` in Codex). If the tool has no subagent support, the main session does the work directly and ignores the delegation rules.
- After a worker returns, verify the result before reporting success to the user.
- Only the orchestrator stages files. Workers report changed paths but do not stage.

All Operating Rules and First Steps in this file apply to every spawned subagent, not only to the main session.

## First Steps For Every Task

1. Read this file.
2. Read `AI/Memory.md` for durable user instructions and prohibitions.
3. Check which branch you are on with `git branch --show-current` and read the matching section in `## Repo Snapshot`. Rules differ between branches.
4. Check `git status --short` before editing.
5. Inspect only the files needed for the task.
6. Identify whether the task touches code, assets, generated data, runtime/player-editable JSON, configs, networking, storage, worldgen, localization, or dependencies.
7. Ask for clarification only when the answer changes behavior, compatibility, migration, or risk.
8. Make the smallest coherent change that solves the request.
9. Run fresh verification before claiming success.
10. Stage changed tracked files before finishing, respecting explicit user staging limits.
11. Final response should summarize what changed, what was verified, and any remaining risk.

## Tone And Collaboration

Be concise, technical, and direct.

- State assumptions explicitly.
- Prefer concrete file references over broad descriptions.
- Explain tradeoffs when a choice affects maintainability, compatibility, player data, or addon behavior.
- Do not over-explain routine edits.
- If something cannot be verified locally, say so clearly.
- For reviews, list findings first, ordered by severity, with file and line references.

## Repo Snapshot

DragonMineZ 2.2 is a Minecraft mod inspired by Dragon Ball, in active development. The repository has two active branches with different loaders and rules. The mod version stays 2.2 across branches.

### Branch `main` — NeoForge 1.21.1

- Minecraft `1.21.1` on NeoForge `21.1.238` or later on the 21.1 line.
- Java: `21` (Gradle must run on Java 21).
- Boot path: `DragonMineZ` -&gt; `DMZCommon.init()` -&gt; side init through `DistExecutor`.
- Client init: `DMZClient.init()`. Server init: `DMZServer.init()`.
- Registrations are mostly centralized in `DMZCommon` and `common/init/Main*` classes.
- Persistent player data uses NeoForge serializable attachments, keeping the 2.2 field layout, including double-precision training points and cooldowns.
- Mandatory dependencies: GeckoLib `4.9.2`, TerraBlender `1.21.1-4.1.0.8`, Curios `9.5.1+1.21.1`. MariaDB and HikariCP are bundled in the JAR. JEI is optional.
- Porting notes live in `PORTING-1.21.1.md`; final validation results and log paths live in `PORT-VALIDATION.md`. Read both before architecture-level changes.
- Loading old Forge 1.20.1 worlds and old binary addons is NOT validated on this branch.
- Known hard-incompatibilities from the 1.20.1 version (Legendary Tooltips, Epic Fight, Better Combat) were not revalidated on NeoForge 1.21.1. Do not claim they still crash or still work without checking.

### Branch `port/2.2-fabric-26.1.2` — Fabric 26.1.2 (work in progress)

- Fabric Loader `0.20.1`, Fabric API `0.155.3+26.1.2`, Loom `1.15.5`, GeckoLib `5.5.2`. Java: `25`.
- This branch starts from the NeoForge 1.21.1 port at `08ad003e`, which preserves the official Forge 1.20.1 2.2 source at `d824a29c`. All existing mechanics must be migrated and verified; do not treat NeoForge code as already correct here.
- IMPORTANT: this branch does NOT build a playable mod. `./gradlew build` currently fails compilation. No mechanics have been removed from compilation to make the build pass. The remaining platform work is tracked in `porting/STATUS.md`; read it before planning changes.
- `./gradlew verifyExtensionApiCore` passes and covers extension algorithms and selected native platform modules: inventory persistence, transactional energy, network codecs and hook descriptors. Passing it does not establish gameplay parity.
- Draft APIs exist in `com.dragonminez.api.*` and are NOT validated in a running game: `ContentCompletionApi`, `CustomNpcRegistry` (definitions only; entity, renderer, service interactions and spawn command not implemented), `FormCompositionResolver`/`FormCompositionRuntime`, `ConfigTranslations`. Their source hooks compile but are unverified at runtime.
- Do not claim gameplay parity or a working build on this branch. A future verified release must include `26.1.2-fabric` in its GitHub release title.

## Build And Run Commands

Use the Gradle wrapper from the repository root.

On branch `main` (NeoForge 1.21.1):

```sh
./gradlew build
./gradlew runClient
./gradlew runServer
./gradlew runData
./gradlew runGameTestServer
```

Notes for `main`:

- `runClient` and `runServer` use `run/`.
- `runData` uses `run-data/` and writes to `src/generated/resources/`.
- `src/generated/resources/` is part of the source tree: after changing data providers, run `runData` and include the result.
- `runGameTestServer` runs integration tests with the mod loaded under NeoForge. It covers the nine enchantments, 6x6 Pattern Station recipe codecs, static recipe/loot loading, entity construction, and player stat/cooldown restore.
- `scripts/build-port.sh` wraps the build keeping caches under `~/Downloads`; use it when the user asks for that cache layout.

On branch `port/2.2-fabric-26.1.2` (Fabric):

```sh
./gradlew verifyExtensionApiCore
./gradlew build
```

Notes for the Fabric branch:

- `verifyExtensionApiCore` is the only green check; run it after touching extension algorithms or native platform modules.
- `build` is expected to fail until the port is complete. Report the failure honestly instead of working around it.
- `porting/STATUS.md` tracks what remains.

Language JSON validation helper (both branches): `scripts/check_lang.sh`, which expects `jq`.

## Git Safety

- Check status before editing and before final response.
- Do not revert unrelated changes.
- Do not amend commits unless explicitly requested.
- Do not create commits unless requested.
- If unexpected changes appear, inspect whether they conflict with the task. If they do, stop and ask.
- If the user explicitly says not to stage a file, do not stage it.
- If files were changed and not excluded by the user, stage them before finishing.

Useful commands:

```sh
git branch --show-current
git status --short
git diff
git diff --staged
git add &lt;changed-files&gt;
```

## DragonMineZ-Specific Rules

### Registrations

- Add new gameplay content through the existing registration pattern of the current branch (`common/init/Main*` on main; the Fabric equivalent entrypoints and registries on the Fabric branch).
- Wire registration through the branch's boot path when needed.
- Keep registry names stable once exposed to worlds, saves, recipes, assets, or addons.
- On main, items use 1.21.1 data components (enchantments, books, potions, trims, maps). The nine enchantments have real datapack registry definitions.

### Networking

- Register all gameplay packets in `NetworkHandler.register()` on main. On the Fabric branch, use the Fabric network stack present in source and keep the same encode/decode/handle discipline.
- Use explicit direction and stable encode/decode/handle triplets.
- Packet ids come from a sequential counter: APPEND new registrations at the end; never reorder or insert mid-list.
- Validate server-side packet handling. Never trust client input for stats, unlocks, permissions, quests, wishes, or progression.

### Player State

- Player state centers on `StatsCapability`, `StatsProvider`, and `StatsData`.
- On main, persistent player data uses NeoForge serializable attachments. On the Fabric branch, the Fabric attachment, lifecycle and network adapters exist in source but are not runtime-validated.
- The internal field format keeps the 2.2 layout, including double-precision training points and cooldowns. Inspect the attachment classes before changing persistence behavior.
- Be careful with login, clone, death, dimension change, tick, sync, and save/load behavior.
- Avoid client-only authority for persistent gameplay data.

### Runtime JSON And Addons

Many JSON files are not internal-only. Players and addon authors create and edit them.

- Keep JSON human-readable.
- Preserve clear field names and stable semantics.
- Avoid unnecessary nesting, minification, or opaque encoded values.
- When changing a runtime JSON format, consider migration, defaults, validation errors, and backup behavior.
- Do not silently delete unknown user fields unless strict schema enforcement is explicitly required.
- Prefer helpful logging for malformed user files.

Important systems:

- Quests and sagas load/generate under world-save folders `&lt;world&gt;/dragonminez/{sagas,quests,sidequests}`; existing files are never overwritten by defaults.
- Wishes load/generate under `&lt;world&gt;/dragonminez/wishes/*.json` (world save, NOT `config/`); plain JSON arrays, dragon id = filename stem.
- Config files live under `config/dragonminez`, use semver `configVersion` regeneration, and back up outdated files into `config/dragonminez/oldBackup/`.
- The Fabric branch adds config-driven systems under `config/dragonminez`: custom races with `completedContentRequisites` and dynamic form composition (`baseForm`, `inheritsMultipliersFrom`), custom NPC definitions under `custom_npcs/` and placements under `custom_npc_placements/`, and config translations under `lang/`. These schemas are draft; extend them only in ways compatible with the draft APIs in `com.dragonminez.api.*`.

### Configs

- Preserve `configVersion` meaning.
- If a config schema changes, handle backup/regeneration intentionally.
- Remember configs may be manually edited by server owners or players.

### Storage

- Storage supports `NBT`, `JSON`, and `DATABASE`.
- Inspect `StorageManager` before changing persistence behavior.
- `DatabaseManager` uses MariaDB and HikariCP, bundled in the JAR. On DB failure there is NO active fallback switch — the broken backend silently no-ops and durability comes from the always-on vanilla capability `.dat` persistence.
- Do not log credentials or connection secrets.
- Treat storage changes as high-risk because they can affect existing worlds.

### Worldgen

- Inspect `server/world/*` and common setup before changing regions, surfaces, features, or structures.
- Run data generation when changing providers.
- Keep generated resources in sync when provider changes require it.

### Mixins

- Add mixin entries to `src/main/resources/dragonminez.mixins.json`.
- Keep mixins minimal and defensive.
- Verify dedicated server compatibility for anything touching client classes.

### Optional Integrations

- WorldGuard support is reflective to avoid a hard dependency.
- Keep optional integration code defensive.
- Check the branch's build file and mod manifest together when changing dependencies (`build.gradle.kts` + `META-INF/neoforge.mods.toml` on main; the Fabric `build.gradle`/`fabric.mod.json` on the Fabric branch).

### Localization

- Source locale: `src/main/resources/assets/dragonminez/lang/en_us.json`.
- Keep localization keys stable unless intentionally renaming.
- Update language entries when adding user-visible text.
- Validate language JSON when possible.
- On the Fabric branch, config translations live under `config/dragonminez/lang/*.json` and are resolved by `ConfigTranslations` (locale fallback to English, then raw key). Only string values are allowed.

## Verification Expectations

Run the strongest practical verification for the change.

On branch `main`:

- Use `./gradlew build` for broad code validation.
- Use `./gradlew runData` for datagen/provider changes, and include the regenerated resources.
- Use `./gradlew runGameTestServer` for integration-level checks.
- Use `./gradlew runClient` or `./gradlew runServer` for gameplay-sensitive checks when practical.

On branch `port/2.2-fabric-26.1.2`:

- Use `./gradlew verifyExtensionApiCore` after touching extension algorithms or native platform modules.
- Use `./gradlew build` and report its result honestly; failure is the expected state until `porting/STATUS.md` says otherwise.

Common:

- Use `scripts/check_lang.sh` or equivalent JSON validation for language changes.
- If a check cannot be run, state why.
- Do not say the build passes unless it passed in this session.
- Do not claim the checks equal full gameplay validation, and do not claim old Forge 1.20.1 worlds or addons load; the ports did not validate that.

## When To Ask The User

Ask before proceeding when:

- The task may require backward compatibility or migration.
- A change could alter player-created JSON, configs, saves, or addon behavior.
- A change could break existing worlds.
- The best validation requires launching Minecraft and the user did not request that cost.
- Dependency/version work conflicts with offline Gradle/cache state.
- Requirements imply deleting, regenerating, or overwriting large file sets.
- Unrelated local changes conflict with the task.
- On the Fabric branch: the task touches a draft API area (content completion, custom NPCs, form composition, config translations) — confirm whether the goal is source-level progress or runtime-verified behavior, since the latter may be blocked.

## Final Response Checklist

Before finishing:

- Confirm which branch was touched.
- Confirm whether files were edited.
- If edited, stage changed tracked files unless explicitly told not to.
- State what changed.
- State what verification was run and its result.
- State any verification not run.
- Mention risks or follow-ups only when they matter.
