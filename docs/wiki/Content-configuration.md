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

Each new NPC belongs in `config/dragonminez/custom_npcs/*.json`. This registry is separate from built-in masters and enemies.

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

This illustrates the layout; fill `objectives` with supported objective definitions for a playable quest. Objective/reward payloads retain their existing type-specific fields. Existing prerequisite expressions remain supported. Saga manifests group `name` under `identity` and `requirements`/`questFolder` under `progression`.

## Migration and rollback

Legacy files are decoded in memory and validated before replacement. A successful migration writes the nested schema and saves an exact copy of the original bytes next to the file under `oldBackup/<filename>.legacy.bak`. Further backups use numbered names; an existing backup is never overwritten. The log identifies both the converted file and its backup. Adding missing default skills participates in the same transaction, retaining the original backup and unknown addon fields.

Replacement uses an atomic filesystem move. If the filesystem cannot provide it, the original is retained. A file edited during the load transaction is not overwritten. Syntax errors, unsupported schema versions and rejected definitions retain the original file; the loader logs the problem and uses its safe fallback. Invalid custom form groups have no custom fallback forms. An invalid form graph cancels pending migration writes and uses bundled defaults for all form groups in memory, avoiding dangling cross-group references.

Known historical layouts include unversioned files, snake-case aliases, scalar or three-axis model scale, numeric historical hair slots, the intermediate `hairCode` field, renamed skill/mastery fields and old flat 2.2 files. The test corpus records distinct form layouts from repository history. Retired values that have no active setting remain under `legacy`; custom/unknown JSON fields are preserved. Unknown historical numeric hair IDs are rejected rather than guessed.

To roll back manually, stop the server, copy the chosen backup over its original path and use a matching mod build. Keep world/player backups when downgrading; JSON backups do not roll back player progression.
