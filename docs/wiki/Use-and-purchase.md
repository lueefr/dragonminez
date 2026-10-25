# Purchase and transformation conditions

Purchase conditions and use conditions are independent. Neither needs an addon. Both use the same extensible condition vocabulary.

## Buy freely, restrict use

In a form entry:

```json
{
  "name": "stage",
  "requirements": {
    "unlock": {
      "skillLevel": 1
    },
    "use": {
      "all": [
        {
          "mastery": {
            "form": "path.foundation",
            "min": 100
          }
        },
        {
          "mastery": {
            "form": "path.training",
            "min": 100
          }
        },
        {
          "content": "example:trial"
        }
      ]
    }
  }
}
```

With no `requirements.buy` or global purchase condition, these use conditions do not restrict buying the skill. Existing prices, master range, alignment and unlock requirements still apply. Do not put a use-only condition under `requirements.unlock`: the older unlock gates intentionally retain their previous purchase/unlock behavior.

Checks run on the server for ordinary activation, including the radial/instant path, charged transformations, stacks and the standard character setter used by scripts. A failed use condition leaves the active form unchanged. The ordinary player paths check before removing trigger items or resources. Conditions are checked when entering the form, not continuously while it is active.

The radial shows a short use-lock marker and a hover tooltip describing missing conditions, using synchronized configuration and progression. A stale or modified client cannot authorize use; the server checks again.

## Buy-only conditions

Use `requirements.buy` on a form or global target-level rules in `config/dragonminez/rules.json`:

```json
{
  "schema": 1,
  "purchase": {
    "conditions": {
      "custom_training": {
        "*": {
          "content": "example:eligibility"
        },
        "2": {
          "mastery": {
            "form": "path.foundation",
            "min": 50
          }
        }
      }
    }
  }
}
```

`*` applies at every bought level; numbered keys are the target level. Global rules and form-level purchase conditions are cumulative. These rules are evaluated before TP is debited. They do not become use conditions.

## Condition vocabulary

All keys in an object are ANDed. Use `all`, `any` and `not` to compose objects:

```json
{
  "all": [
    {"content": "example:trial"},
    {"any": [
      {"mastery": {"form": "path.foundation", "min": 100}},
      {"skill": {"id": "custom_training", "level": 3}}
    ]},
    {"not": {"content": "example:restriction"}}
  ]
}
```

| Condition | Value |
| --- | --- |
| `content` | Namespaced content ID completed at least once. |
| `mastery` | `form` as `group.form`, and numeric minimum `min`. |
| `skill` | Skill `id` and minimum `level`. |
| `level` | Minimum player level. |
| `race` | Race ID. |
| `used` | Form reference used at least once. |
| `first` | Boolean: the form's first-use state. |
| `flying`, `charging`, `chargingKi`, `transforming`, `aura`, `grounded`, `sneaking`, `water`, `night` | Boolean context conditions. |
| `all`, `any` | Arrays of condition objects. |
| `not` | One condition object to negate. |

During cross-race fusion, linked racial `mastery` and `used` references resolve in the target form's race/group namespace. Stack references retain their shared namespace. Form names retain the owning race's translation keys.

Mastery values are points in that form's configured mastery scale; a form with a maximum of 100 uses `min: 100` for maximum mastery. Nesting is bounded and unknown registered condition names are rejected at config validation.

## Refusal dialogs and colors

The server sends a translatable message to a centered modal built with the tutorial panel skin and smooth font. OK closes it and restores the previous screen. Long messages can be scrolled.

Each referenced form name carries its own configured hair color. The renderer computes a contrasting outline for each colored span: dark text gets a light outline, light text gets a dark outline. Multiple form names may therefore have different colors in one message. The target form colors the surrounding use-denial message; without a target, the first colored form provides the surrounding style. Messages without any form use the default tutorial style.

Text is not translated into the server's language before transmission. Each receiving client resolves its own selected locale through the config translation layer.

## Java API

- `Conditions.register(key, type)` adds validation, evaluation and translatable descriptions. Register before extension validation completes.
- `Conditions.missing(new Conditions.Context(data, "path.stage"), condition)` returns descriptions of unmet conditions.
- `FormAccess.missing(data, group, form, stack)` describes use locks without changing state or sending a dialog.
- `FormAccess.allow(data, group, form, stack, forced)` checks on the server and emits a refusal or forced-use warning when needed.
- `Character.setActiveForm(group, form)` and `setActiveStackForm(group, form)` enforce use conditions on the logical server.
- `Character.forceSetActiveForm(group, form)` and `forceSetActiveStackForm(group, form)` explicitly bypass use conditions and warn the affected player. Administrative triggers and forced native ritual grants use this path.
- Completion/skill rewards still grant permanent unlocks independently of purchase conditions. Unlocking a skill does not silently bypass a later ordinary use check.

Legacy scripts assigning the group and form separately keep their method signatures: the group is staged until the form is supplied, then the pair is checked atomically. Prefer the two-argument setter for new integrations.

Custom Java integrations should check `FormAccess.allow` before charging their own external costs. Do not directly mutate form fields or load fabricated NBT to implement a transformation.


All native skill purchase/upgrade refusals now use the modal path: missing or unavailable skill, race restriction, fusion, missing offering within 10 blocks, alignment, existing ownership, maximum level, grant-only level, insufficient TP and configured requirements. The server returns a translatable reason before debiting TP or granting progression. It never trusts the cost sent by the client.
