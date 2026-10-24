# DragonMineZ fork: changes and extension API guide

This is an **unofficial fork** of [DragonMineZ](https://github.com/DragonMineZ/dragonminez), created by the original DragonMineZ team. It maintains a NeoForge port and additional fixes and configuration APIs. It does not replace the original project's identity, community or release channels.

This guide describes **Minecraft 1.21.1 / NeoForge, mod version 2.2, through Hotfix 3**. Examples use generic content; their IDs are placeholders unless explicitly described as bundled appearance aliases.

## Contents

- [Changes and release status](#changes-and-release-status)
- [Installation and configuration](#installation-and-configuration)
- [Persistent content completion](#persistent-content-completion)
- [Custom NPCs](#custom-npcs)
- [Recursive form multipliers](#recursive-form-multipliers)
- [Config translations](#config-translations)
- [Java API reference](#java-api-reference)
- [Validation and troubleshooting](#validation-and-troubleshooting)
- [Original project and credits](#original-project-and-credits)

## Changes and release status

### NeoForge 1.21.1 port

The source baseline is the original **Forge 1.20.1 release 2.2**. The port adapts registration, events, networking, entities, menus, dimensions and integrations to NeoForge and Minecraft 1.21.1.

Important platform changes include:

- Serializable NeoForge attachments for player data, retaining the 2.2 internal field layout, double-precision training points and cooldown data.
- Minecraft data components for item data and real datapack registry definitions for the mod's nine enchantments.
- JSON/network recipe codecs retaining Pattern Station recipes up to 6×6.
- Updated datapack paths/formats for recipes, loot, advancements, tags and biome modifiers.
- Adapted rendering, HUD, camera, models and mixins.

The port is based on the existing mod's mechanics. This is not a claim that every race, progression path, multiplayer scenario and optional integration has been exercised end to end. Old Forge worlds and old binary addons have not been validated for migration.

### Additional shipped fixes and APIs

| Area | Fork addition or fix |
| --- | --- |
| Player effects | Aura outline and lightning use consistent camera transforms, keeping deferred effects aligned with the player in the inspected scenes. Deferred effects skip shader shadow passes. |
| Shader skin overlays | Polygon depth bias replaces global model-view scaling for the musculature overlay. |
| Content progression | Persistent completion IDs/counts, native events, quest/ritual hooks, commands and JSON form prerequisites. |
| Custom NPCs | Separate JSON definitions, entity/renderer, cosmetic equipment/hair, services and placements; bundled masters/enemies are not rewritten. |
| Form composition | Recursive multiplier inheritance, dynamic sources, player caching and cycle checks. |
| Translations | Config-based names/dialogue with server synchronization and fallback lookup. |
| NPC validation | Custom skill sales use their current definition and ten-block sphere rather than inherited default master offers. Placements wait for persistent chunk entities before spawning replacements. |

[Hotfix 3 and its JAR](https://github.com/lueefr/dragonminez/releases/tag/v2.2-hotfix-3) contain these APIs and player-rendering fixes. The internal mod version remains **2.2**; hotfix numbering is a release label. The count restarts when porting a different original mod version.

**Otherworld worldgen/LOD work is separate and unfinished**, tracked in [PR #7](https://github.com/lueefr/dragonminez/pull/7). Do not infer universal Voxy, Distant Horizons or SeedGen compatibility from this release. The Fabric 26.1.2 port is also unfinished and is not a playable release. Its future release title must contain `26.1.2-fabric`.

## Installation and configuration

Use Java **21**, Minecraft **1.21.1**, and NeoForge **21.1.238 or later on the 21.1 line**. Install the fork JAR and matching NeoForge dependencies:

| Dependency | Documented version |
| --- | --- |
| GeckoLib | 4.9.2 |
| TerraBlender | 1.21.1-4.1.0.8 |
| Curios | 9.5.1+1.21.1 |

Clients and dedicated servers should use the same fork build. MariaDB and HikariCP are bundled; JEI is optional. See [porting notes](https://github.com/lueefr/dragonminez/blob/main/PORTING-1.21.1.md) for build and dependency details.

The additions use ordinary editable JSON:

```text
config/dragonminez/
  races/<race>/forms/<group-file>.json
  custom_npcs/<definition>.json
  custom_npc_placements/<placement>.json
  lang/en_us.json
  lang/<locale>.json
```

Quest content continues to use the existing **world-save** folders, including `<world>/dragonminez/sidequests/`. It is not moved into the NPC config folder.

After editing configs, an authorized administrator can use `/dmzreload config`. After editing quests, use `/dmzreload story`; `/dmzreload all` reloads both scopes. Check server logs for malformed files, missing references and invalid IDs. A restart is also an option. These are existing administrative commands; server permission rules still apply.

## Persistent content completion

### What it enables

A form can require that a player has finished any named piece of content at least once: a configurable task, a ritual, or an event implemented through commands or an addon. Completion is independent of mastery and the quest's current repeat/reset status.

- IDs use lowercase namespaced syntax, such as `example:trial_complete`.
- Progress is stored per player, including a completion count.
- Repeating completion increments the count; it does not repeatedly become a first completion.
- Progress survives save/load, player cloning and quest resets.
- Completion changes are made on the server thread and synchronized to the player.

### Require completion in a form

Add this to a form entry inside its existing racial group JSON:

```json
{
  "name": "custom_stage",
  "unlockOnSkillLevel": 1,
  "completedContentRequisites": [
    "example:trial_complete",
    "example:chapter_complete"
  ]
}
```

**Every** listed ID must have been completed. This field adds to existing skill, level, saga and mastery requirements; it does not replace them. The group is identified by its internal `groupName`, not the JSON filename. It does not make scripted groups accumulate mastery or change their existing radial-menu rules.

### Record and query completion with commands

These two commands require permission level 2:

```mcfunction
/dmzcontent complete @s example:trial_complete
/dmzcontent has @s example:trial_complete
```

`complete` accepts a player selector and returns the number of targeted players. `has` accepts one player and returns command result **1** if completed, otherwise **0**. `@s` requires a player executor; console callers can use a player name or suitable selector.

A command function can connect a custom event to the prerequisite by running `complete` after successful completion. Recording a flag does not execute or create that event itself.

### Native quests and rituals

Successful native quest completion records an automatic ID: `dragonminez:quest/<quest-id>`, replacing quest-key colons with slashes. Unusual/very long legacy quest keys use a stable encoded UUID path; Java integrations should call `ContentCompletionApi.questContentId(questKey)` instead of rebuilding that rule.

A quest can also declare a reusable alias. For example, save this as `<world>/dragonminez/sidequests/custom_task.json`:

```json
{
  "id": "custom_task",
  "type": "SIDEQUEST",
  "title": "quest.example.custom_task",
  "quest_giver": "example:guide",
  "turn_in": "example:guide",
  "objectives": [
    {"type": "TALK_TO", "npcId": "example:guide"}
  ],
  "completionId": "example:trial_complete"
}
```

This uses the existing quest parser, acceptance and turn-in mechanics. The NPC must offer `custom_task`, as shown below. A side-quest ID cannot contain `:`; saga quest keys retain the existing `saga:quest` format. The completion alias is a namespaced content ID, which is a different kind of identifier.

The existing native ritual configuration also exposes `completionId`; its successful completion records the configured ID. This hook does not create an arbitrary new ritual engine. Other custom content can use the command or Java completion API.

When an old save has quest history but lacks completion flags, recorded completed quests are imported without replaying completion events. Past rituals or external events with no saved history cannot be reconstructed automatically.

## Custom NPCs

### Define an additive NPC

Create `config/dragonminez/custom_npcs/guide.json`:

```json
{
  "id": "example:guide",
  "baseTexture": "master_male",
  "armor": {"chest": "minecraft:iron_chestplate"},
  "hairType": "base",
  "hairPreset": 1,
  "name": "npc.example.guide",
  "dialogues": ["dialogue.example.welcome"],
  "alignment": 50,
  "offers": {
    "skills": ["custom_training"],
    "quests": ["custom_task"],
    "shop": [
      {"item": "minecraft:apple", "count": 2, "price": 5}
    ]
  }
}
```

This registry reads its own files. It does not replace the original masters or enemy definitions.

| Field | Meaning |
| --- | --- |
| `id` | Unique namespaced custom NPC ID. |
| `baseTexture` | Registered appearance alias. Bundled aliases are `master_male`, `master_female`, `humanoid_slim`; they reuse neutral body textures/models, not a complete face/color customization system. |
| `armor` | Cosmetic item IDs for `head`, `chest`, `legs`, `feet`. These are separate from real equipment and grant no equipment attributes. |
| `hairType` | Existing hair-style slot; use `base` for the ordinary slot or `none` to disable hair. |
| `hairPreset` | Existing preset ID; defaults to 1. A positive ID must exist. |
| `forcedHairCode` | Optional encoded hairstyle using the existing hair-code system. It is not an arbitrary filename or texture ID. |
| `name`, `dialogues` | Display strings or translation keys. Dialogues are display text, not a JSON scripting language. |
| `alignment` | Existing 0–100 alignment scale; defaults to 50. Service access follows the mod's alignment gates. |
| `offers.skills` | IDs already configured in the existing skill system. Listing an ID does not define a new skill. |
| `offers.quests` | IDs already loaded by the quest registry. |
| `offers.shop` | Registered item ID, count and training-point price. Count must be 1–64 and fit the item's stack size; price is a nonnegative integer. |

Empty offers produce an ambient NPC. There is no requirement to enable every service.

### Skill pricing and service checks

NPC skill offers reuse existing skill/form pricing and unlock rules. For form skills, the race's `character.json` keeps its `formSkillsCosts` entries with `buyFromMaster` and `prices`; the number of price entries defines the skill's configured maximum level. A price of `-1` is not purchasable. Listing such content on an NPC does not bypass its grant or progression requirements.

The server checks custom NPC range and current definition for skill purchases and quest actions. Shop transactions additionally recheck alignment, training points and inventory capacity. The client cannot choose its own shop price. Cosmetic armor does not change those checks.

### Spawn or place the NPC

After reloading the config, a permission-level-2 operator can run:

```mcfunction
/dmzcustomnpc spawn example:guide
/dmzcustomnpc spawn example:guide 0.5 80 0.5
```

Coordinates are optional; without them the command uses the executor's position. It uses the command's current dimension and rotation.

For persistent placement, create `config/dragonminez/custom_npc_placements/guide.json`:

```json
{
  "npc": "example:guide",
  "dimension": "minecraft:overworld",
  "x": 0.5,
  "y": 80,
  "z": 0.5,
  "yaw": 180
}
```

Placements are checked every 100 level ticks in loaded chunks, after persistent entities have loaded. They never force distant chunks to load. Dimension plus placement filename determines the persistent UUID: renaming a placement file changes its identity and may leave the previous entity in the save. Removing a definition disables its services; it does not delete saved NPCs. Placement edits are not a command to relocate or delete an existing saved entity.

## Recursive form multipliers

### Static and dynamic sources

A form may declare **one** source:

| Field/value | Resolution |
| --- | --- |
| `baseForm: "best"` | Select the best unlocked candidate for this player when resolving the form. |
| `baseForm: "current"` | Use the active source, with the preceding source retained when entering a current-based chain. |
| `baseForm: "group.form"` | Resolve a specific form and multiply its result by this form's local values. |
| `inheritsMultipliersFrom: "group.form"` | Recursively inherit the referenced form's resolved multipliers and apply the local delta. |

Do not put both source fields on the same form. `inheritsMultipliersFrom` takes a specific reference, not `best` or `current`; put a dynamic source at the chain's root instead.

### Example chain

Save a group file under `config/dragonminez/races/<race>/forms/`:

```json
{
  "configVersion": "2.2.0",
  "groupName": "custom_path",
  "formType": "superforms",
  "forms": {
    "stage_one": {
      "name": "stage_one",
      "unlockOnSkillLevel": 1,
      "baseForm": "best",
      "strMultiplier": 1.10
    },
    "stage_two": {
      "name": "stage_two",
      "unlockOnSkillLevel": 2,
      "inheritsMultipliersFrom": "custom_path.stage_one",
      "strMultiplier": 1.15
    }
  }
}
```

This is a form-group example, not a complete race definition. The existing race/skill configuration must still grant access to the required skill levels. Group references use `groupName`, even if the filename differs.

If the selected source's STR multiplier is 4.0, the first stage resolves to `4 × 1.10 = 4.4` and the second to `4 × 1.10 × 1.15 = 5.06`, before any additional applicable runtime modifiers. Changing the source to 6.0 propagates to `6.6` and `7.59` without editing either delta. Existing mastery adjustments participate in resolution; omitted fields retain the form class's defaults.

Supported local multiplier fields are `strMultiplier`, `skpMultiplier`, `stmMultiplier`, `defMultiplier`, `vitMultiplier`, `pwrMultiplier`, `eneMultiplier` and `speedMultiplier`. Resolved values must remain finite and positive.

### Selection, caching and errors

`best` evaluates unlocked racial candidates through the resolver. Its score is the arithmetic mean of resolved STR, SKP, DEF, VIT, PWR and ENE; STM and speed do not enter that ranking. The identity multiplier is the fallback if no stronger eligible candidate is selected.

A form being resolved, and candidates whose static dependencies lead back to the active resolution path, are excluded. Static inheritance cycles, duplicate IDs and unknown references produce configuration errors. Dependency chains are limited to 256 references, preventing unbounded recursion.

The runtime stores the resolver with each player's stats and caches resolved values. Relevant skill/mastery/content progression, active source, race/fusion state and config generations invalidate the state. It does not recompute every chain on every stat lookup while those inputs are unchanged. The current-source anchor is persisted with character data.

Forms without either source field retain their original calculation path. This addition does not remove existing stack-form or compatibility rules.

## Config translations

Create `config/dragonminez/lang/en_us.json`:

```json
{
  "npc.example.guide": "Custom guide",
  "dialogue.example.welcome": "Welcome. Choose a service.",
  "quest.example.custom_task": "Custom task",
  "skill.dragonminez.custom_training": "Custom training",
  "race.dragonminez.custom_race.group.custom_path": "Custom path",
  "race.dragonminez.custom_race.form.custom_path.stage_one": "First stage",
  "race.dragonminez.custom_race.form.custom_path.stage_two": "Second stage"
}
```

Add another locale file, such as `pt_br.json`, with translated values for the same keys. Files are flat string-to-string JSON maps. A translation supplies a label; it does not define a race, form, skill or quest.

The lookup order is:

1. Server-synchronized selected locale.
2. Local config selected locale.
3. Server-synchronized English.
4. Local config English.
5. Normal bundled translations, then the raw key fallback.

NPCs can use arbitrary translation keys. Form/group/skill keys must match the existing UI patterns. Stack form keys use `race.dragonminez.stack.form.<group>.<form>` and `race.dragonminez.stack.group.<group>`.

Server language snapshots are sent with the normal config synchronization. Changed snapshots invalidate cached translated Components. Remote translations are cleared on disconnect so they do not leak into another server's session. **No resource pack or JAR edit is required for these text overrides.**

## Java API reference

JSON and commands cover the examples above without an addon. Java integrations can use the classes under `com.dragonminez.api`:

| Entry point | Purpose |
| --- | --- |
| `content.ContentCompletionApi.complete(ServerPlayer, String, String)` | Record one completion; returns the new count. Call on the server thread after success. |
| `content.ContentCompletionApi.hasCompleted(ServerPlayer, String)` | Query a completion flag. |
| `content.ContentCompletionApi.questContentId(String)` | Build the automatic content ID for a native quest key. |
| `content.ContentCompletedEvent` | NeoForge event containing player, content ID, kind, count and first-completion status. |
| `StatsData.getContentProgress()` | Read counts, snapshots and progression revision. |
| `npc.CustomNpcRegistry.INSTANCE` | Access immutable definition/placement snapshots and reload the separate registry. |
| `npc.CustomNpcApi.spawn(ServerLevel, String, double, double, double, float)` | Spawn a registered custom NPC; server thread only. |
| `npc.CustomNpcServices.purchase(ServerPlayer, CustomNpcEntity, int)` | Validate and perform a shop transaction; server thread only, zero-based offer index, boolean result. |
| `npc.CustomNpcVisuals.register(String, String, String)` | Register another bundled appearance alias from model and texture resource IDs. Duplicate aliases are rejected. |
| `forms.FormCompositionResolver` | Validate a definition graph, supply player state/revisions and resolve multiplier sets. |
| `forms.FormCompositionRuntime` | Bridge configured forms, native player progression and stat calculations. |
| `lang.ConfigTranslations.INSTANCE` | Reload/look up translations, inspect local snapshots and revisions, replace/clear remote snapshots. Normal networking manages client remote state. |

`StatsData` is in `com.dragonminez.common.stats`; the other table paths are relative to `com.dragonminez.api`. Native player stats should normally be obtained through the existing `StatsProvider`/`StatsCapability` interface. Persistence is owned by the mod; avoid writing completion NBT or client-authoritative progression directly.

For example, inside an existing server-side success callback where `player` is a `ServerPlayer`:

```java
int count = ContentCompletionApi.complete(player, "example:trial_complete", "custom_event");
boolean unlocked = ContentCompletionApi.hasCompleted(player, "example:trial_complete");
```

Register an event listener once on the NeoForge game event bus:

```java
import com.dragonminez.api.content.ContentCompletedEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.NeoForge;

public final class CompletionHooks {
    public static void register() {
        NeoForge.EVENT_BUS.addListener(CompletionHooks::onCompleted);
    }

    private static void onCompleted(ContentCompletedEvent event) {
        if (event.isFirstCompletion()
                && event.getContentId().equals("example:trial_complete")) {
            event.getPlayer().sendSystemMessage(Component.literal("First completion recorded."));
        }
    }
}
```

The event is posted after the in-memory persistent player state is updated. It is not a guarantee that a disk/database flush has already completed, and it is not replayed during legacy history migration. Native quest completion can emit one event for its automatic ID and another for a distinct configured alias; filter by ID when handling rewards.

## Validation and troubleshooting

The final API branch passed **six unit tests and ten required native GameTests**. Separate Minecraft runs checked effects with/without Iris and Complementary, custom NPC presets 1–3 with cosmetic armor, services, translation snapshot/disconnect behavior, native quest C2S start/turn-in/completion, and a placement unload/reload with exactly one NPC retaining its UUID. These are scenario checks, not universal shader/GPU/armor coverage or a complete gameplay campaign.

| Symptom | Check |
| --- | --- |
| Form remains locked | Required completion IDs, skill level, existing level/saga/mastery gates, and internal groupName. A label translation does not unlock content. |
| NPC cannot spawn | Definition loaded successfully, unique namespaced ID, known appearance/preset and current command dimension. |
| NPC does not offer a skill | It must already exist in the skill configuration, be race-eligible, purchasable and included in offers. Check server price, range and alignment. |
| Quest is missing | Use the world-save quest folder, reload story, and use a colon-free side-quest ID. Listing a quest in an NPC definition does not register it. |
| Placement appears duplicated after a rename | Placement filenames are part of persistent identity; inspect the old saved entity rather than repeatedly renaming files. |
| Translation shows its key | Verify exact key/locale, valid JSON string values, reload and server config sync. |
| Config inheritance error | Inspect the reported cycle/missing group.form reference. Do not combine both source fields. |
| Otherworld looks wrong in an LOD mod | That follow-up is tracked in PR #7; Hotfix 3 does not establish a completed LOD fix. |

Detailed evidence and limitations: [API validation](https://github.com/lueefr/dragonminez/blob/main/API-VALIDATION.md) and [port validation](https://github.com/lueefr/dragonminez/blob/main/PORT-VALIDATION.md). Short JSON reference: [API-EXTENSIONS.md](https://github.com/lueefr/dragonminez/blob/main/API-EXTENSIONS.md).

## Original project and credits

Original project: [DragonMineZ/dragonminez](https://github.com/DragonMineZ/dragonminez). Original developers: [Yuseix](https://github.com/yuseix300), [ezShokkoh](https://github.com/Shokkoh) and [Bruno](https://github.com/Bruneitor123). Original contributors include Bati2ra, KyoSleep, JotaJoestar, Toji71_, ZoneMC and Darkeryr. The port also used [AgentMelinda's earlier NeoForge port](https://github.com/AgentMelinda/dragonminez-1.21.1) as a reference. Dragon Ball was created by Akira Toriyama.

Support/follow the original team: [Patreon](https://patreon.com/DragonMineZ) and [Discord](https://discord.gg/b5MgRNb3D7). Original downloads: [CurseForge](https://www.curseforge.com/minecraft/mc-mods/dragonminez) and [Modrinth](https://modrinth.com/mod/dragonminez). Fork downloads: [GitHub Releases](https://github.com/lueefr/dragonminez/releases).

The code retains [GPL v3.0-or-later licensing](https://github.com/lueefr/dragonminez/blob/main/LICENSE). See the [README credits](https://github.com/lueefr/dragonminez#original-project-and-credits) and [third-party licenses](https://github.com/lueefr/dragonminez/blob/main/THIRD_PARTY_LICENSES) for library and audio acknowledgments.
