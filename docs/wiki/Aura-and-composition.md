# Aura behavior and multiplier composition

## Smooth aura transitions

Main and extra aura layers blend across type or form changes. The outgoing shell remains visible while the incoming shell appears. This includes changes between forms using the same texture/geometry with a different color. Reversing during a transition starts from the visible mixture rather than snapping to an endpoint.

Per-form timing lives under `aura`:

```json
{
  "aura": {
    "transitionSeconds": 0.4,
    "absorptionSeconds": 0.4
  }
}
```

Times are seconds, bounded to 0.01–10 at rendering time. Omitted values use 0.4 seconds. The existing 2D and 3D backends retain their own appearance. Switching the entire backend resets its rendering cache; a type/form transition within a backend blends normally.

## Sealed aura

`aura.state: false` hides the aura through its normal fade. `aura.state: "sealed"` is a separate visual state: the existing shell contracts toward the player's body and is removed after the absorption completes. There is no permanently rendered fully transparent shell. Sealing also stops new aura dust/rubble emissions, the aura sound loop and aura-driven charging shake. Already emitted particles finish their normal lifetimes; lightning and outlines remain independent visual settings.

```json
{
  "aura": {
    "state": "sealed"
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
```

When either context becomes true the aura appears. When neither applies it is absorbed again. `true` and `"visible"` explicitly show it. Rules run in declaration order, and stack overrides run after the racial form. The existing visual system can independently control lightning, outline, body parts, tail, eye markings and fur.

`chargingKi` means ki charging; `charging` retains the existing action-charge meaning. `transforming` identifies charging a normal or stack transformation. Java addons may register additional boolean/context conditions through `Conditions.register`.

## Dynamic inheritance

The local values are relative multipliers in the default `multiply` mode:

```json
{
  "stats": {
    "multipliers": {
      "strength": 1.2,
      "defense": 1.2
    },
    "composition": {
      "base": "best"
    }
  }
}
```

`stats.composition.base` accepts:

- `best`: the best unlocked form for this player at resolution time.
- `current`: the active form, using the persisted pre-composition anchor when the active form is already in the same chain.
- `group.form`: a specific form reference.

A descendant uses `stats.composition.inherits` to reference another form's already resolved values. Use a base at the root or an inherited reference for a child, not both fields on one form.

## Additive deltas

Select `stats.composition.mode: "add"` to add the local values to the resolved parent per stat:

```json
{
  "name": "advanced",
  "stats": {
    "multipliers": {
      "strength": 5,
      "strikePower": 0,
      "stamina": 0,
      "defense": 5,
      "vitality": 0,
      "kiPower": 0,
      "energy": 0,
      "speed": 0
    },
    "composition": {
      "inherits": "path.foundation",
      "mode": "add"
    }
  }
}
```

A parent strength multiplier of 16 and a child strength delta of 5 resolve to 21. Another child adding 5 resolves to 26. The same applies when the chain's root is dynamic. With `add`, omitted per-stat values are zero, so a stat without a configured delta is unchanged. Deltas must be finite and nonnegative; zero preserves the inherited stat. The final resolved multiplier remains positive. Without a parent reference the base is the identity multiplier, 1.

`multiply` remains the default. Its local multipliers must be positive; omitted values remain 1. Existing mastery adjustments apply to local configured values before composition. Set the mastery scaling intentionally if the desired delta must remain fixed at every mastery level.

The resolver caches results separately for each player. Unlock, mastery, race, relevant progression and config revisions invalidate the applicable cache. Static cycles are rejected during configuration validation with a reference path. Dynamic resolution excludes the form currently being resolved and its descendants from candidate selection, avoiding self-selection through `best`/`current`.

Java integrations use `FormCompositionResolver.Definition`, `PlayerState`, `reload`, `updatePlayer` and `resolve`. The four-argument `Definition` constructor retains multiplicative behavior; the constructor with a `mode` argument selects `"add"` or `"multiply"`. Pure resolver callers must supply all eight local stat values, including explicit zero deltas where appropriate.
