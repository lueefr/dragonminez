# Content configuration

Content files use `schema: 1` and sections grouped by purpose. Java APIs retain their existing form getters/setters; the JSON layout is handled at the serialization boundary. IDs are stable references, not display names. Display names are translation keys.

## Form groups

Location: `config/dragonminez/races/<race>/forms/*.json`. Stack groups live in `config/dragonminez/forms/*.json`. The group is identified by `identity.groupName`, not by the filename.

```json
{
  "schema": 1,
  "identity": {
    "groupName": "path",
    "formType": "superforms"
  },
  "forms": {
    "stage": {
      "name": "stage",
      "appearance": {
        "hair": {
          "slot": "base",
          "color": "#72B8FF",
          "editable": true
        },
        "body": {
          "scale": [
            0.94,
            0.94,
            0.94
          ]
        }
      },
      "aura": {
        "transitionSeconds": 0.4,
        "absorptionSeconds": 0.4,
        "main": {
          "type3d": "smooth",
          "color": "#72B8FF"
        },
        "state": "sealed"
      },
      "stats": {
        "multipliers": {
          "strength": 2.0,
          "defense": 2.0
        }
      },
      "requirements": {
        "unlock": {
          "skillLevel": 1
        },
        "use": {
          "content": "example:trial"
        }
      },
      "effects": {
        "rules": [
          {
            "if": {
              "chargingKi": true
            },
            "set": {
              "aura": true
            }
          },
          {
            "if": {
              "flying": true
            },
            "set": {
              "aura": true
            }
          }
        ]
      }
    }
  }
}
```

Omitted settings retain engine defaults. `appearance.hair.editable: true` offers this form in Edit Hair and gives the player's saved hairstyle priority; `false` uses the configured hairstyle and hides the editor entry. Fixed hair requires a valid `appearance.hair.code` or `appearance.hair.slot: "none"`.

| Section | Purpose |
| --- | --- |
| `appearance.hair` | Hair slot/code, color and player editing. |
| `appearance.eyes` | Primary/secondary eye colors. |
| `appearance.body` | Model, scale, body colors, extra layers and tint. |
| `appearance.parts`, `appearance.fur`, `appearance.tail`, `appearance.eyes` | Body-part scaling, fur overrides, tail color/visibility and eye markings. |
| `aura` | Main/extra type, color, layer, 3D style, state and transition timing. |
| `effects` | Lightning, outline, status effects, animation and conditional `rules`. |
| `stats.multipliers` | Per-stat local values. |
| `stats.composition` | Dynamic/static base, inheritance and arithmetic mode. |
| `progression.mastery` | Mastery gains, caps, sharing and transform thresholds. |
| `requirements.unlock` | Existing skill, mastery, level, saga and content unlock gates. |
| `requirements.buy` | Extra conditions evaluated only when purchasing. |
| `requirements.use` | Conditions evaluated only when activating a form. |
| `stacking` | Compatibility, stackability and stack mastery. |
| `economy.drains`, `economy.items` | Resource drains and item costs. |
| `controls` | Form combo. |
| `legacy` | Retired historical settings retained for inspection/rollback. |

## Skills

`config/dragonminez/skills.json` groups skill types under `categories`, master offerings under `masters.offerings`, and individual skill definitions under `skills`:

```json
{
  "schema": 1,
  "categories": {
    "kiSkills": [],
    "formSkills": [
      "custom_training"
    ]
  },
  "masters": {
    "offerings": {}
  },
  "skills": {
    "custom_training": {
      "economy": {
        "prices": [
          100,
          200
        ]
      },
      "requirements": {
        "races": []
      }
    }
  }
}
```

Keep existing categories and entries when extending this shared file. Race form skill prices remain in the race character configuration; `-1` means not purchasable, while commands and completion rewards may still grant the level.

## Custom NPCs

Each new NPC belongs in `config/dragonminez/npcs/definitions/*.json`. This registry is separate from built-in masters and enemies.

```json
{
  "schema": 1,
  "id": "example:guide",
  "appearance": {
    "body": {
      "texture": "master_base",
      "model": "master_male"
    },
    "armor": {
      "chest": "minecraft:leather_chestplate"
    },
    "hair": {
      "slot": "base",
      "preset": 1
    }
  },
  "identity": {
    "name": "example.npc.guide",
    "alignment": 50
  },
  "interaction": {
    "dialogues": [
      "example.dialogue.greeting"
    ],
    "offers": {
      "skills": [
        "custom_training"
      ],
      "quests": [],
      "shop": []
    }
  }
}
```

Textures are external PNG files in `config/dragonminez/npc_textures/`, using the filename stem in `baseTexture`. `master_base` is supplied there by the mod. Hair may use a preset or the salon's encoded `appearance.hair.code`; encoded data is a hairstyle, not a texture ID. Cosmetic armor does not replace the external body texture. Existing placement files, spawn commands and `/dmzlocate npc example:guide` continue to use the same NPC ID.

## Quests and sagas

Files remain under `<world>/dragonminez/quests`, `sidequests` and `sagas`; they are world data, not client configuration.

```json
{
  "schema": 1,
  "id": "custom_trial",
  "text": {
    "title": "example.quest.title",
    "description": "example.quest.description"
  },
  "identity": {
    "type": "side",
    "category": "general"
  },
  "progression": {
    "repeatable": false,
    "completionId": "example:trial"
  },
  "objectives": [],
  "rewards": []
}
```

This illustrates the layout; fill `objectives` with supported objective definitions for a playable quest. Objective/reward payloads now group their type-specific fields under `target`, `progress`, `combat`, `spawning`, `appearance`, `navigation`, `grant`, `availability` and `text`. Java parsers still receive their established field names. Existing prerequisite expressions remain supported under `requirements.prerequisites` and `requirements.start`. Saga manifests group `name` under `identity` and `requirements`/`questFolder` under `progression`.

## Migration and rollback

Legacy files are decoded in memory and validated before replacement. A successful migration writes the nested schema and saves an exact copy of the original bytes next to the file under `oldBackup/<filename>.legacy.bak`. Further backups use numbered names; an existing backup is never overwritten. The log identifies both the converted file and its backup. Adding missing default skills participates in the same transaction, retaining the original backup and unknown addon fields.

Replacement uses an atomic filesystem move. If the filesystem cannot provide it, the original is retained. A file edited during the load transaction is not overwritten. Syntax errors, unsupported schema versions and rejected definitions retain the original file; the loader logs the problem and uses its safe fallback. Invalid custom form groups have no custom fallback forms. An invalid form graph cancels pending migration writes and uses bundled defaults for all form groups in memory, avoiding dangling cross-group references.

Known historical layouts include unversioned files, snake-case aliases, scalar or three-axis model scale, numeric historical hair slots, the intermediate `hairCode` field, renamed skill/mastery fields and old flat 2.2 files. The test corpus records distinct form layouts from repository history. Retired values that have no active setting remain under `legacy`; custom/unknown JSON fields are preserved. Unknown historical numeric hair IDs are rejected rather than guessed.

To roll back manually, stop the server, copy the chosen backup over its original path and use a matching mod build. Keep world/player backups when downgrading; JSON backups do not roll back player progression.


## Races, techniques and encounters

Race `character.json` groups body/hair/face defaults under `appearance`, aura defaults under `aura.main`, and form purchase settings under `economy.formSkills`. A legacy price array becomes an object with `buyFromMaster: false` and `prices`; `-1` retains its grant-only meaning.

```json
{
  "schema": 1,
  "appearance": {
    "hair": {"slot": 1, "color": "#72B8FF"},
    "body": {"model": "", "usePlayerSkin": true, "scale": [0.94, 0.94, 0.94]}
  },
  "economy": {
    "formSkills": {
      "custom_training": {"buyFromMaster": true, "prices": [100, 200, -1]}
    }
  }
}
```

Race `stats.json` retains the `classes` map. Each class has `stats.base`, `stats.scaling`, health/energy/stamina `regeneration`, `economy` and `racial` sections. Regeneration names state their units: `basePerFiveSeconds`, `perVitality`, `perEnergy` and `perResistance`.

Techniques group their types under `types.kiAttacks`, `types.strikeAttacks` and `types.evasionAttacks`. Each entry separates `progression`, `economy`, `combat`, `timing` and `appearance.size`. Entity defaults use `stats.byEntity` and `transformation.defaults`. Raids and tournaments separate `identity`, encounter/rules, timing, fighters/arena, appearance and economy. Fighter definitions separate combat stats, appearance, spawning and transformation. These layouts preserve Java APIs, IDs, values and timing units.

## Quest objectives and rewards

Inside a quest entry:

```json
{
  "requirements": {
    "prerequisites": {"operator": "AND", "conditions": []},
    "start": {"operator": "AND", "conditions": []}
  },
  "objectives": [
    {"type": "BIOME", "target": {"biome": "minecraft:plains"}}
  ],
  "rewards": [
    {"type": "TPS", "grant": {"amount": 100}, "availability": {"difficulties": ["NORMAL", "HARD"]}}
  ]
}
```

A historical `hard:` or `normal:` reward type prefix becomes an explicit difficulty list when no explicit difficulty override existed. Unknown addon fields remain in the JSON; they are not dropped to make migration pass.

## NPC placements and dialogue trees

Custom NPC placements group `npc` under `identity` and dimension/coordinates/yaw under `location`. World dialogue trees group the NPC under `identity` and the starting node under `interaction`. Each node has `text.line` and `interaction.choices`; a choice uses `text.label`, `navigation.next`, `requirements` and `effects.actions`.

```json
{
  "schema": 1,
  "identity": {"npc": "example:guide"},
  "interaction": {"start": "greeting"},
  "nodes": {
    "greeting": {
      "text": {"line": "example.dialogue.greeting"},
      "interaction": {"choices": [
        {"text": {"label": "example.dialogue.finish"}, "effects": {"actions": []}}
      ]}
    }
  }
}
```

## Validation before writing

Quest default updates join the same deferred migration queue as layout conversion. Invalid, duplicate or rejected definitions are removed from that queue before any replacement. A failed backup prevents writing. Backups are numbered and retained. Reloading a file after an intentional edit validates the new bytes; an edit after validation still prevents commit.

Malformed built-in quest/saga files use their generated default in memory and retain the malformed original on disk, including when automatic generation of missing defaults is disabled. Custom files without a bundled default are skipped. Disabling default generation still prevents creation of missing quest files.

See [Historical coverage and text auditing](History-and-audit) for the executable compatibility corpus and translation checks.


## Transformation dimensions

Every form accepts `appearance.size`, in addition to the existing per-axis
`appearance.body.scale`. For example:

```json
{
  "appearance": {
    "body": {"scale": [0.9375, 0.9375, 0.9375]},
    "size": {"height": 3, "width": 1.2, "multiplier": 0.8, "eyeRatio": 0.85}
  }
}
```

Height and width use blocks. The example resolves to 2.4 blocks tall and
0.96 blocks wide. `multiplier` defaults to 1 and accepts 0.05..16. Optional
`height` and `width` accept 0.1..32 before that multiplier; resolved model axes
must stay within 0.01..64. Height rescales all axes proportionally, while an
explicit width sets both horizontal axes. Without height/reference the existing
per-axis body scale is multiplied directly.

Instead of height, `reference` can name any registered entity type, including
other mods. It uses that type's registered standing height, without spawning
an entity. Explicit height takes precedence over reference:

```json
{"appearance": {"size": {"reference": "dragonminez:saga_saiyan_oozaru", "multiplier": 0.75}}}
```

New default Oozaru and Golden Oozaru definitions use this reference with
multiplier 1, matching the hostile NPC's four-block registered height. Existing
owner-edited files are preserved; add this block to apply the same sizing there.

`eyeRatio` is the eye position as a fraction of standing height, in 0.05..1.
Without an override the existing proportion is retained. Stack eye overrides
take precedence over the main form. Crouching/swimming still adjust the pose.
Physical dimensions retain the engine's tenth-block rounding, with a minimum
0.1-block body and eyes kept inside it. The square hitbox uses the larger
horizontal axis to contain both X and Z model scales.

These settings enter the existing form/stack size composition and transition
pipeline, including authoritative flight. Main/stack scales keep their existing
composition semantics. Server configs synchronize to clients; no client-specific
size file is needed. Omitting `size` retains the existing body scale.


## Dynamic model poses and camera presentation

Forms can drive registered body parts with the same numeric expressions used
by meters and aura settings. Keep the old `false` (hide), `true` (show), or
`[x,y,z]` scale syntax when no pose overrides are needed. The object syntax
adds relative rotations and offsets:

```json
{
  "appearance": {
    "parts": {
      "right_arm": {
        "scale": [1, 1, 1],
        "rotation": [15, 0, 0],
        "offset": [0, {"multiply": [2, {"source": "attack.charging"}]}, 0]
      }
    },
    "camera": {
      "fov": 0.9,
      "roll": {"multiply": [3, {"source": "attack.charging"}]}
    }
  }
}
```

`scale` multiplies the animated bone scale (bounded 0.05..8 per axis).
`rotation` adds degrees to the current animation (-180..180); `offset` adds
model units, normally 16 per block (-32..32). Omitted axes groups leave the
current pose unchanged. Each group has exactly three values. Supported part
IDs include head, body, left_arm, right_arm, left_leg, right_leg, tail and
left/right_forearm; addons can register more aliases. The pose is restored
after drawing, including GUI previews; it does not change physical hitboxes.
Use `appearance.size` for physical dimensions.

Camera `fov` multiplies the world FOV (0.25..4; final FOV 15..170 degrees), and
`roll` adds -180..180 degrees. Hand/item FOV, authoritative aim yaw/pitch and
projectile direction are untouched. Camera overrides apply only to the local
player's camera with a created character, outside menus and spectator mode.
Shared conditional `effects.rules` can set `camera` and `parts` alongside other
visual properties. Main/stack rules retain their existing merge order.

Sources are synchronized `meter:<namespace:path>`, `state.formTicks`,
`state.stackTicks`, `self.healthPercent` and attack-window values from
[Runtime rules](Runtime-rules). Event-only values such as `event.damage` are
not available while rendering: store them in a meter first. Expressions
compile and cache; missing/nonfinite inputs disable that expression until
configuration reload, log once and use neutral scale/rotation/offset/FOV/roll.
These are presentation bindings: server Lua can declare the form and update
its meters without running Lua or arbitrary graphics code on clients.
