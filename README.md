# DragonMineZ 2.2 — Fabric 26.1.2 port

**Work in progress. This branch does not currently build a playable Fabric mod. No Fabric release has been published.** The extension core tests pass, but the full port still fails compilation. Passing the core tests does not establish gameplay parity.

This branch starts from the working NeoForge 1.21.1 port at `08ad003e`, which preserves the official Forge 1.20.1 version 2.2 source at `d824a29c`. The original source, the official NeoForge branch and other forks are references; all existing mechanics remain in the source tree and must be migrated and verified.

The mod version remains **2.2**. A future verified release must include **26.1.2-fabric** in its GitHub release title. Release publication and hotfix numbering across targets still need to be completed after gameplay parity is verified.

## Development and verification

Requires Java 25. This branch uses Fabric Loader 0.20.1, Fabric API 0.155.3+26.1.2, Loom 1.15.5 and GeckoLib 5.5.2.

```sh
./gradlew verifyExtensionApiCore
./gradlew build
```

The first command tests independent extension algorithms. The second compiles all mod sources and currently fails. See [porting/STATUS.md](porting/STATUS.md) for the remaining platform work. No game mechanics have been removed from compilation to make the build pass.

## Content completion prerequisites

Draft API: `com.dragonminez.api.content.ContentCompletionApi` and `ContentProgress`.

- `ContentCompletionApi.complete(ServerPlayer, contentId, kind)` records completion on the server thread and fires `COMPLETED` with the player entity, ID, kind, count and `firstCompletion()`.
- `ContentCompletionApi.hasCompleted(ServerPlayer, contentId)` queries the flag.
- `StatsData.getContentProgress()` exposes `hasCompleted`, `count`, `snapshot`, and a revision for caches.
- Completion counts are stored in player NBT under `CompletedContent`, included in stat snapshots and copied with player data. Flags are independent of a repeatable quest's current state.
- Source hooks record quest completion under `dragonminez:quest/<quest-key>` (colon separators become slashes). Ritual configuration accepts `completionId`; otherwise it derives a stable ID from its configured group. These hooks still require the unfinished Fabric lifecycle/network port.

Example addition to a form in `config/dragonminez/races/custom_race/forms/example.json`:

```json
{
  "groupName": "custom_group",
  "formType": "superforms",
  "forms": {
    "stage_one": {
      "name": "stage_one",
      "unlockOnSkillLevel": 1,
      "completedContentRequisites": ["custom:ritual_a", "custom:event_a"],
      "strMultiplier": 1.1,
      "defMultiplier": 1.1
    }
  }
}
```

All listed content IDs must have been completed at least once. The group is identified by its internal `groupName`. Existing skill-level, mastery, level, saga, pricing and master purchase requirements continue to be separate gates.

A configurable ritual can use:

```json
{
  "completionId": "custom:ritual_a"
}
```

An integration can call `ContentCompletionApi.complete(player, "custom:event_a", "event")` after successful completion and register a listener through `ContentCompletionApi.COMPLETED.register(...)`. A content ID is namespaced and must match `[a-z0-9_.-]+:[a-z0-9_./-]+`.

## Additive custom NPC definitions

Draft API: `com.dragonminez.api.npc.CustomNpcRegistry`. `reload(configRoot)` atomically reads only `custom_npcs/*.json` and `custom_npc_placements/*.json`; `get(id)`, `definitions()` and `placements()` expose immutable snapshots. Duplicate IDs and unknown placement references fail validation. Existing master/enemy definitions are never read or overwritten by this registry.

**Current limit:** this is a validated definition registry. The custom entity, cosmetic renderer, authoritative service interactions and spawn command are not implemented yet. A JSON file alone does not currently spawn an NPC.

`config/dragonminez/custom_npcs/example.json`:

```json
{
  "id": "custom:guide",
  "baseTexture": "master_male",
  "armor": {"head": "minecraft:iron_helmet", "chest": "minecraft:iron_chestplate"},
  "hairType": "custom_hair",
  "forcedHairCode": "",
  "name": "custom.npc.guide",
  "dialogues": ["custom.dialogue.greeting"],
  "alignment": 50,
  "offers": {
    "skills": ["custom_skill"],
    "quests": ["custom:quest_a"],
    "shop": [{"item": "minecraft:apple", "count": 1, "price": 10}]
  }
}
```

`armor` is a separate cosmetic slot map. Supported schema slots are `head`, `chest`, `legs` and `feet`. Empty offer lists describe an ambient NPC. Actual asset/hair/item resolution and the existing master proximity/alignment checks must be connected when the entity/service layer is ported.

`config/dragonminez/custom_npc_placements/example.json`:

```json
{
  "npc": "custom:guide",
  "dimension": "minecraft:overworld",
  "x": 0.5,
  "y": 70.0,
  "z": 0.5,
  "yaw": 90.0
}
```

## Dynamic form composition

Draft API: `com.dragonminez.api.forms.FormCompositionResolver` and `FormCompositionRuntime`.

- `reload(definitions)` validates static references and cycles before installing an immutable graph.
- `updatePlayer(uuid, PlayerState)` supplies an immutable progression snapshot.
- `resolve(uuid, "group.form")` returns a cached multiplier vector; `removePlayer(uuid)` discards it.
- `baseForm` accepts `best`, `current` or `group.form`. `inheritsMultipliersFrom` accepts a static `group.form` reference.
- A definition uses either `baseForm` or `inheritsMultipliersFrom`. Its own multipliers multiply its resolved parent's values; omitted stats default to 1.
- `best` scores unlocked candidates by the average of the six main combat stats, with player mastery adjustments supplied by the runtime adapter. Resolution excludes itself, active ancestors and candidates whose static chain depends on those ancestors.
- `current` resolves the active form unless doing so would refer back to the resolving chain; that case uses the player's base multiplier of 1.
- Static cycles and unknown references fail with a readable chain. Config reload invalidates caches. The source runtime adapter tracks skill, mastery, content and active-form changes rather than resolving the graph every tick; full in-game integration remains unverified.

A generic dynamic root with two stages:

```json
{
  "groupName": "custom_chain",
  "formType": "superforms",
  "forms": {
    "stage_one": {
      "name": "stage_one",
      "unlockOnSkillLevel": 1,
      "baseForm": "best",
      "strMultiplier": 1.1,
      "defMultiplier": 1.1
    },
    "stage_two": {
      "name": "stage_two",
      "unlockOnSkillLevel": 2,
      "inheritsMultipliersFrom": "custom_chain.stage_one",
      "strMultiplier": 1.2,
      "defMultiplier": 1.2
    }
  }
}
```

For a base strength multiplier of 2, these stages resolve to 2.2 and 2.64. Unlocking a stronger base or reloading its configuration changes the entire chain. The core tests cover this propagation, player isolation, caching, dynamic self-exclusion, `current`, and static cycle rejection.

## Names translated from config JSON

Draft API: `com.dragonminez.api.lang.ConfigTranslations`.

- `reload(configRoot.resolve("lang"))` atomically loads locale files.
- `translate(locale, key)` resolves the locale, then English, then the raw key. `find(locale, key)` returns null if no config translation exists so Minecraft can perform its normal lookup.
- `localSnapshot()`, `replaceRemote(...)`, `clearRemote()` and `revision()` support synchronization and cache invalidation.
- Source hooks synchronize language maps in config batches, overlay client language lookup and invalidate cached translated text when config translations change. These hooks are not yet validated in a running Fabric client.

`config/dragonminez/lang/en_us.json`:

```json
{
  "race.dragonminez.custom_race.group.custom_group": "Example group",
  "race.dragonminez.custom_race.form.custom_group.stage_one": "Example stage",
  "skill.dragonminez.custom_skill": "Example skill",
  "custom.npc.guide": "Example guide",
  "custom.dialogue.greeting": "Welcome."
}
```

`config/dragonminez/lang/pt_br.json`:

```json
{
  "race.dragonminez.custom_race.group.custom_group": "Grupo de exemplo",
  "race.dragonminez.custom_race.form.custom_group.stage_one": "Estágio de exemplo",
  "skill.dragonminez.custom_skill": "Habilidade de exemplo",
  "custom.npc.guide": "Guia de exemplo",
  "custom.dialogue.greeting": "Bem-vindo."
}
```

All values must be strings. No resource pack, texture or JAR edit is involved. Translation keys must match those used by the form/skill UI or the custom NPC definition.

## License and upstream

DragonMineZ is licensed under GPL-3.0. See [LICENSE](LICENSE). The original project is [DragonMineZ/dragonminez](https://github.com/DragonMineZ/dragonminez). The previous port's README is preserved in [porting/reference/README-1.21.1.md](porting/reference/README-1.21.1.md).
