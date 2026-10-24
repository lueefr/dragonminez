
> Port 2.2 / Minecraft 1.21.1: [build, dependencies and validation](PORTING-1.21.1.md).
<div align="center">
  <img src=".github/docs/img/dragonminez_big_logo.png" alt="DragonMine Z Banner" width="70%" />
</div>

<div align="center">

[![Discord](https://img.shields.io/discord/1216429657273012415?style=for-the-badge&logo=Discord&logoColor=white&label=Discord&color=orange)](https://discord.gg/b5MgRNb3D7)
[![Patreon](https://img.shields.io/badge/Patreon-Join%20Us-yellow?style=for-the-badge&logo=Patreon)](https://patreon.com/DragonMineZ)
[![Progress Board](https://img.shields.io/badge/GitHub-Progress%20Board-red?style=for-the-badge&logo=GitHub)](https://github.com/orgs/DragonMineZ/projects/4)
[![CurseForge](https://img.shields.io/badge/CurseForge-Download-orange?style=for-the-badge)](https://www.curseforge.com/minecraft/mc-mods/dragonminez)
[![Modrinth](https://img.shields.io/badge/Modrinth-Download-green?style=for-the-badge)](https://modrinth.com/mod/dragonminez)

</div>

<div align="center">

**English** · [Español](README_ES.md) · [Português](README_PT.md)

</div>

---

## About

**DragonMine Z** is an immersive **Work-In-Progress Minecraft NeoForge 1.21.1 mod** inspired by Akira Toriyama's most-famous work, [Dragon Ball](https://en.dragon-ball-official.com/).

Our goal is to bring the full Dragon Ball experience into Minecraft: custom characters, races, transformations, skills, stats, story content, dimensions, NPCs, structures, and a revamped survival experience.

The project is still in active development, so features may change as we keep improving the mod.

---

## ✨ Features

- Create your own Dragon Ball-inspired character.
- Choose from multiple races, including Saiyan, Human, Namekian, Frost Demon, Bio-Android, and Majin.
- Customize your appearance with hair, eyes, skin, aura colors, and more.
- Progress through stats such as Strength, Strike Power, Resistance, Vitality, Ki Power, and Energy.
- Unlock skills, transformations, combat abilities, and new ways to grow stronger.
- Explore new locations, dimensions, NPC encounters, structures, and story content.
- Experience Minecraft survival with a Dragon Ball progression system layered on top.

---

## 🖼️ Showcase

<table>
  <tr>
    <td align="center" width="50%">
      <img src=".github/docs/img/red-ribbon-robots.png" alt="Red Ribbon robot enemies showcase" width="100%" />
      <br />
      <strong>Red Ribbon Robots</strong>
      <br />
      Preview of two Red Ribbon robot enemies being in DragonMine Z as part of the enemy roster.
    </td>
    <td align="center" width="50%">
      <img src=".github/docs/img/namek-background.png" alt="Namek background showcase" width="100%" />
      <br />
      <strong>Namek Environment Preview</strong>
      <br />
      Background showcase inspired by Namek, presenting the atmosphere and visual direction of the mod.
    </td>
  </tr>
</table>

---

## 🚀 Download

The original DragonMine Z from the mod pages:

- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/dragonminez)
- [Modrinth](https://modrinth.com/mod/dragonminez)

You can only download the port from GitHub:
- [GitHub Releases](https://github.com/lueefr/dragonminez/releases/latest)
Release notes are available in the [changelog](CHANGELOG.md) and are included with each GitHub release.

---

## 🫴 Support the Project

Want early previews and extra behind-the-scenes updates?

Join the [Patreon](https://patreon.com/DragonMineZ) to support the original mod development and get access to previews and similar perks. Your support helps us keep improving DragonMine Z and creating more content for the community.

You can also join their [Discord server](https://discord.gg/b5MgRNb3D7) to follow development, report bugs, suggest ideas, and talk with the community.

---

## 🗺️ Roadmap

<div align="center">
  <img src=".github/docs/img/roadmap.png" alt="DragonMine Z Roadmap" />
</div>

You can also follow their public [GitHub Progress Board](https://github.com/orgs/DragonMineZ/projects/4).

---

## 🤝 Contributing

Would you like to help? Cool! Check out the [contribution guide](.github/CONTRIBUTING.md) to get started.

You can help with:

- Code contributions
- Bug reports
- Feature suggestions
- Translations
- Documentation
- Models, animations, builds, and other creative work

---

## 🎯 Use of Third Parties

### Sounds

Some sounds are used from [Zapsplat](https://www.zapsplat.com/) and [Freesound](https://freesound.org/):

- [Dragon Ball Scouter/Tracker Remade.wav](https://freesound.org/s/518004/) by Pablobd | License: Attribution 3.0
- [A Symphony for Akira Toriyama](https://www.youtube.com/watch?v=xNVEkSerkU0) by GLADIUS | License: CC-BY License

### Acknowledgments

This project includes code from [GeckoLib](https://github.com/bernie-g/geckolib), which is licensed under the MIT License.

Copyright © 2025 GeckoThePecko. See the [`THIRD_PARTY_LICENSES`](THIRD_PARTY_LICENSES) file for details.

---

## ✨ Authors

### Developers

- [Yuseix](https://github.com/yuseix300) | *Co-Founder & Programmer*
- [ezShokkoh](https://github.com/Shokkoh) | *Co-Founder & Programmer*
- [Bruno](https://github.com/Bruneitor123) | *Co-Founder, Programmer & Community Admin*

### Contributors

- [Bati2ra](https://github.com/Bati2ra) | *Programmer*
- [KyoSleep](https://github.com/KyoSleep1) | *Programmer*
- JotaJoestar | *Modeler and Animator*
- Toji71_ | *Builder*
- ZoneMC | *Textures*
- Darkeryr | *Textures*

---

## License

2026, DragonMine Z.

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License or, at your option, any later version.

[GNU General Public License v3.0](https://github.com/DragonMineZ/dragonminez/blob/main/LICENSE)

### Configurable extension API (NeoForge 1.21.1)

This branch adds the following extension surfaces. Runtime and compatibility validation is still in progress; see [API validation](API-VALIDATION.md) before treating a development build as a release.

#### Lifetime content completion

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

#### Additive custom NPCs

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

#### Recursive form multipliers

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

#### Config translations

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


Player aura alignment, lightning and shader skin-overlay fixes are included in this API branch. LOD and Otherworld worldgen fixes are maintained separately on [`fix/2.2-neoforge-1.21.1-lod`](https://github.com/lueefr/dragonminez/tree/fix/2.2-neoforge-1.21.1-lod).
