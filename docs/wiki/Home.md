# Configuration API

Use the API to add completion requirements to forms, define custom NPCs, compose form multipliers and translate custom text. JSON configuration covers these features without an addon. Java entry points are available for integrations.

## Configuration files and reload

| Content | Location |
| --- | --- |
| Racial form groups | `config/dragonminez/races/<race>/forms/*.json` |
| Custom NPC definitions | `config/dragonminez/custom_npcs/*.json` |
| Custom NPC placements | `config/dragonminez/custom_npc_placements/*.json` |
| Config translations | `config/dragonminez/lang/<locale>.json` |
| Side quests | `<world>/dragonminez/sidequests/*.json` |

After editing config files, run `/dmzreload config`. After editing quests, run `/dmzreload story`. Use `/dmzreload all` for both. These commands require the server's administrative permissions. Invalid JSON, unknown references and dependency cycles are reported in the server log.

The examples below use generic IDs. Replace them with your content. Adding a name to an NPC's offers does not register the corresponding skill or quest.

## 1. Require completed content to unlock a form

### Add a completion requirement

Inside a form entry, add `completedContentRequisites`:

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

Every listed ID must have been completed at least once by that player. Existing skill, level, saga and mastery requirements still apply. This is a form entry, not a complete group or race definition.

Completion IDs use lowercase namespaced syntax: `namespace:path`. Progress belongs to the player, survives save/load and cloning, and is retained when a repeatable quest is reset. Completing the same ID again increments its count.

### Record completion from a command function

After your custom event succeeds, run:

```mcfunction
/dmzcontent complete @s example:trial_complete
```

Query the flag with:

```mcfunction
/dmzcontent has @s example:trial_complete
```

Both commands require permission level 2. `complete` accepts a player selector and returns the number of targeted players. `has` accepts one player and returns command result `1` if completed, otherwise `0`. For console execution, replace `@s` with a player name or suitable selector.

These commands record progress; they do not execute the custom event itself.

### Record completion from a quest

Add `completionId` to a quest JSON. For example, create `<world>/dragonminez/sidequests/custom_task.json`:

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

Configure the NPC below to offer `custom_task`. Successful native quest completion records the alias `example:trial_complete`, satisfying that form requirement.

Side-quest IDs such as `custom_task` cannot contain `:`. Existing saga quest keys use `saga:quest`. A quest ID and a namespaced completion ID are different identifiers.

Native quests also record an automatic completion ID. Use `ContentCompletionApi.questContentId(questKey)` in Java to obtain it. The existing native ritual configuration also accepts `completionId`; other configurable events can record progress through commands or Java.

Existing saved quest history is imported when loading a save without completion flags, without replaying completion events. Past events that left no saved history cannot be inferred.

### Use the completion API in Java

Call this from a successful server-side callback, with a `ServerPlayer`:

```java
import com.dragonminez.api.content.ContentCompletionApi;

// Inside your existing server-side callback:
int count = ContentCompletionApi.complete(player, "example:trial_complete", "custom_event");
boolean completed = ContentCompletionApi.hasCompleted(player, "example:trial_complete");
```

`complete` must run on the server thread. It updates persistent player state, posts a `ContentCompletedEvent` and requests synchronization. Posting the event does not guarantee that a disk/database flush has already finished.

Register a listener once on the NeoForge game event bus:

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
            event.getPlayer().sendSystemMessage(Component.literal("Trial completed."));
        }
    }
}
```

The event exposes `getPlayer()`, `getContentId()`, `getKind()`, `getTimesCompleted()` and `isFirstCompletion()`. A native quest can emit events for both its automatic ID and a distinct alias, so filter by ID when applying a reward.

For direct reads, `StatsData.getContentProgress()` provides `hasCompleted(id)`, `count(id)`, `snapshot()` and `revision()`. Let the mod manage persistence rather than editing player NBT manually.

## 2. Define a custom NPC

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

Each file defines a separate custom NPC. Built-in masters and enemies are not replaced.

| Field | Usage |
| --- | --- |
| `id` | Unique namespaced NPC ID. |
| `baseTexture` | Registered appearance alias: `master_male`, `master_female` or `humanoid_slim`. These provide neutral body models/textures, not a face/color editor. |
| `armor` | Cosmetic item IDs in `head`, `chest`, `legs` and `feet`. Cosmetic items grant no equipment attributes. |
| `hairType` | Existing hair-style slot; `base` selects the ordinary slot and `none` disables hair. |
| `hairPreset` | Existing preset ID; defaults to 1. Positive IDs must exist. |
| `forcedHairCode` | Optional hairstyle encoded with the existing hair-code system. |
| `name` | Display name or translation key. |
| `dialogues` | Display strings or translation keys; these are not executable scripts. |
| `alignment` | Value from 0 to 100; defaults to 50. Existing alignment service gates apply. |
| `offers.skills` | Skills already defined in the existing skill configuration. |
| `offers.quests` | Quests already loaded by the quest registry. |
| `offers.shop` | Registered item ID, item count and training-point price. |

Use empty offers for an ambient NPC. Shop prices must be nonnegative integers. Counts must be 1–64 and fit the item's stack size.

Skill offers reuse existing pricing and progression rules. Form skill prices remain in the race's `character.json`, under `formSkillsCosts`, with `buyFromMaster` and `prices`. The number of prices determines the configured maximum skill level; `-1` means that level cannot be purchased. Listing content on an NPC does not bypass its requirements.

Skill purchases and quest actions require the custom NPC's live definition and proximity within ten blocks. Shop purchases also validate alignment, balance and inventory capacity on the server. The client does not supply the purchase price.

### Spawn the NPC

Reload config, then use either command with permission level 2:

```mcfunction
/dmzcustomnpc spawn example:guide
/dmzcustomnpc spawn example:guide 0.5 80 0.5
```

Without coordinates, the executor's position is used. The command uses its current dimension and rotation.

### Create a persistent placement

Create `config/dragonminez/custom_npc_placements/guide.json`:

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

Placements are checked every 100 level ticks in loaded chunks, after persistent entities finish loading. They do not force distant chunks to load.

The dimension and placement filename determine its persistent identity. Renaming the file changes that identity and can leave the old NPC in the save. Removing a definition disables its services but does not delete saved entities. Editing placement coordinates does not relocate an existing saved NPC automatically.

### NPC Java entry points

All classes below are in `com.dragonminez.api.npc`:

| Entry point | Usage |
| --- | --- |
| `CustomNpcRegistry.INSTANCE` | Read definition/placement snapshots, look up IDs and reload the separate registry. |
| `CustomNpcApi.spawn(level, id, x, y, z, yaw)` | Spawn a registered NPC on the server thread; returns its entity. |
| `CustomNpcServices.purchase(player, npc, offerIndex)` | Perform a validated shop purchase on the server thread. The index is zero-based; the result is boolean. |
| `CustomNpcVisuals.register(id, modelResource, textureResource)` | Register an additional bundled appearance alias at startup. Duplicate aliases are rejected. |

## 3. Compose form multipliers

Add a source to a form to multiply its local values by another form's resolved values. Sources can be static or depend on the player.

| Source | Meaning |
| --- | --- |
| `"baseForm": "best"` | Use the best eligible unlocked racial form for this player. |
| `"baseForm": "current"` | Use the current source; when entering a current-based chain, retain the preceding source as its anchor. |
| `"baseForm": "group.form"` | Use a specific form as the source. |
| `"inheritsMultipliersFrom": "group.form"` | Inherit the referenced form's recursively resolved result and apply the local delta. |

Use only one source field per form. `inheritsMultipliersFrom` takes a specific form reference. To create dynamic inheritance, put `best` or `current` at the root and reference that root from later stages.

### Define a dynamic chain

Create a group under `config/dragonminez/races/<race>/forms/`:

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

The race and skill must already be configured to grant access to those levels. References use the internal `groupName`, not the filename.

If the selected source's STR multiplier is 4.0:

- `stage_one`: `4 × 1.10 = 4.4`.
- `stage_two`: `4 × 1.10 × 1.15 = 5.06`.

If that source becomes 6.0, the chain becomes 6.6 and 7.59 without editing the local deltas. Existing mastery adjustments participate in resolution; other applicable runtime modifiers still apply.

The supported fields are `strMultiplier`, `skpMultiplier`, `stmMultiplier`, `defMultiplier`, `vitMultiplier`, `pwrMultiplier`, `eneMultiplier` and `speedMultiplier`. Values must resolve to finite positive numbers. Omitted fields retain existing form defaults.

### Resolution rules

- `best` ranks candidates by the mean of resolved STR, SKP, DEF, VIT, PWR and ENE. STM and speed do not contribute to the ranking. Identity multipliers are the fallback when no stronger candidate is selected.
- The form being resolved and candidates whose static dependencies lead back to the active path are excluded from dynamic selection.
- Static cycles, duplicate IDs and missing references produce configuration errors. Chains are limited to 256 references.
- Resolved values are cached per player. Relevant progression/source/config changes invalidate that state, so a chain does not need to be recalculated on every unchanged stat lookup.
- The current-source anchor is saved with character data.
- Forms without a source field keep their existing calculation path. Composition does not grant access to locked content or change scripted-form mastery/menu behavior.

For Java integrations, `com.dragonminez.api.forms.FormCompositionResolver` exposes graph validation, `reload`, `updatePlayer`, `resolve` and `removePlayer`. Supply an updated `PlayerState` revision when progression changes. `FormCompositionRuntime` connects configured forms to the native player stats system.

## 4. Translate custom text through config

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

Create another locale file, such as `pt_br.json`, with translated values for the same keys. Each file must be a flat string-to-string JSON object. Reload config after editing. No resource pack or JAR edit is required.

Use arbitrary translation keys in NPC names/dialogues. Forms, groups and skills must follow the existing UI key pattern. Stack form keys are `race.dragonminez.stack.form.<group>.<form>` and `race.dragonminez.stack.group.<group>`.

Lookup order:

1. Server-synchronized selected locale.
2. Local config selected locale.
3. Server-synchronized English.
4. Local config English.
5. Normal bundled translations, then the raw key.

Server snapshots are synchronized to connected clients. Changes invalidate cached translated Components, and remote translations are cleared on disconnect. A translated label does not register the content it names.

Java integrations use `com.dragonminez.api.lang.ConfigTranslations.INSTANCE` for `reload`, `find`, `translate`, `localSnapshot` and `revision`. Normal config networking manages remote snapshots; integrations should not use client translations as progression authority.
