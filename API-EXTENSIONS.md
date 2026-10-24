# Configurable extension APIs — NeoForge 1.21.1

This fork adds the following extension APIs for NeoForge 1.21.1. See [validation results and coverage limits](API-VALIDATION.md).

## Lifetime content completion

`ContentCompletionApi.complete(ServerPlayer, id, kind)` records a namespaced completion ID in persistent player data and posts `ContentCompletedEvent` with the player, completion count and first-completion status. Read progress with `ContentCompletionApi.hasCompleted` or `StatsData.getContentProgress()`. Progress survives quest resets and player cloning. Saves without the new flags import recorded quest completion history on load without replaying completion events. Past rituals or external events without saved history cannot be inferred. The server is authoritative.

Native quest completion records `dragonminez:quest/<quest-id>` (with namespace separators converted to `/`). A quest JSON may additionally specify `"completionId": "example:trial"`. The existing ritual configuration exposes `completionId` as well. Command functions can record arbitrary configurable content using `/dmzcontent complete <players> example:trial`; `/dmzcontent has <player> example:trial` returns 1 or 0.

Add generic completion prerequisites to a form in its existing group JSON:

```json
{
  "name": "custom_stage",
  "completedContentRequisites": ["example:trial", "example:chapter_complete"]
}
```

Every listed ID must have been completed at least once. Existing skill, level, saga and mastery prerequisites continue to apply. The group is identified by its internal `groupName`, not the filename. These fields require no addon.

## Additive custom NPCs

Place new definitions in `config/dragonminez/custom_npcs/*.json`. This registry does not replace built-in master or enemy definitions.

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
    "shop": [{"item": "minecraft:apple", "count": 2, "price": 5}]
  }
}
```

Skills and quests reference content already registered with their respective systems. Side-quest IDs such as `custom_task` do not contain a colon; `saga:quest` keys retain the existing saga format. Empty offers create an ambient NPC. Armor is cosmetic and occupies separate rendering slots (`head`, `chest`, `legs`, `feet`); it does not grant equipment attributes. Appearance bases include `master_male`, `master_female` and `humanoid_slim`. Hair uses the existing `hairType` and optional `forcedHairCode` system; `hairType: "none"` disables hair. Names and dialogue strings can be translation keys. Alignment uses the existing 0–100 scale and service gates.

Spawn with `/dmzcustomnpc spawn example:guide [x y z]`, or create a separate `config/dragonminez/custom_npc_placements/guide.json`:

```json
{
  "npc": "example:guide",
  "dimension": "minecraft:overworld",
  "x": 0.5, "y": 80, "z": 0.5,
  "yaw": 180
}
```

Placements are checked in loaded chunks without forcing distant chunks to load. Placement filenames determine persistent placement identities. Removing a definition disables its services; it does not delete saved entities. API entry points are `CustomNpcRegistry.INSTANCE` (definitions, placements and reload), `CustomNpcVisuals.register` (additional bundled appearance bases), `CustomNpcApi.spawn` and `CustomNpcServices.purchase`. Spawning and transactions must run on the server thread; purchases recheck range, alignment, price and inventory capacity on the server.

## Recursive form multipliers

Forms can select one composition source using `baseForm` or `inheritsMultipliersFrom`:

```json
{
  "configVersion": "2.2.0",
  "groupName": "custom_path",
  "formType": "superforms",
  "forms": {
    "stage_one": {"name": "stage_one", "baseForm": "best", "strMultiplier": 1.10},
    "stage_two": {"name": "stage_two", "inheritsMultipliersFrom": "custom_path.stage_one", "strMultiplier": 1.15}
  }
}
```

`baseForm` accepts `best`, `current` or `group.form`. `inheritsMultipliersFrom` accepts `group.form` and recursively composes already resolved multipliers. Each local multiplier multiplies its resolved source; omitted multipliers retain the existing form defaults. Use only one source field per form. Existing mastery adjustments remain part of resolution.

`best` resolves against that player's unlocked forms. A form being resolved and forms whose static dependency chain would lead back to it are excluded from dynamic candidates. `current` uses the active source and preserves the preceding source when entering a chain based on the current form. Static cycles and missing references produce configuration errors with the offending dependency path. Resolution is cached per player and invalidated by relevant progression, active-form state and configuration changes.

`FormCompositionResolver` exposes the dependency validation and resolution model. `FormCompositionRuntime` integrates it with player stats. Forms without either field retain their existing multiplier behavior.

## Config translations

Place a flat string-to-string map in `config/dragonminez/lang/pt_br.json`, with English defaults in `config/dragonminez/lang/en_us.json`:

```json
{
  "race.dragonminez.custom_race.form.custom_path.stage_one": "Estágio inicial",
  "race.dragonminez.custom_race.group.custom_path": "Caminho personalizado",
  "npc.example.guide": "Guia",
  "skill.dragonminez.custom_training": "Treinamento personalizado",
  "dialogue.example.welcome": "Bem-vindo."
}
```

The example uses race ID `custom_race` and group `custom_path`; retain the existing UI key pattern for form, group and skill names. Stack forms use `race.dragonminez.stack.form.<group>.<form>` and `race.dragonminez.stack.group.<group>`. Use arbitrary keys in configurable NPC names and dialogue. Config translations override the selected locale, fall back to config English, then allow normal bundled translations and the raw key fallback. Server config translation snapshots are synchronized to connected clients and cleared on disconnect. No resource pack is required. The API is `ConfigTranslations.INSTANCE`, including locale lookup, reload, snapshots and revision tracking.
