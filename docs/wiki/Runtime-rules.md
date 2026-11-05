# Runtime meters, form behavior and reactions

Edit `config/dragonminez/progression/runtime.json` on the server. The generated default is empty: no particular transformation or combat mechanic is enabled automatically. Apply changes with `/dmzreload scripts` (runtime configuration and referenced scripts), `/dmzreload config`, or restart. Administrative permissions are required. Clients receive configuration and live values through normal synchronization.

## Accumulate a combat meter

```json
{
  "combatTicks": 200,
  "meters": {
    "example:resolve": {"scope": "combat", "initial": 0, "min": 0, "max": 100, "decayPerTick": 0.05}
  },
  "rules": [{
    "event": "dmz:damage_received",
    "when": {"content": "example:training"},
    "actions": [{"meter": "example:resolve", "operation": "add", "value": {"source": "event.damage"}}]
  }]
}
```

Meter IDs are namespaced. `character` scope survives saving, reconnecting and cloning; `combat` resets after the configured time without damaging contact; `form` resets when either the main or stack form changes. `combatTicks` accepts 1..72000 ticks. A tick is 1/20 second at the normal server rate. Only persistent character meters survive reload; they are clamped to updated bounds. Removed IDs stay saved so restoring a definition can recover their values.

`decayPerTick` moves a meter toward its initial value. Changes clamp to its bounds. Actions accept `add` or `set`. All conditions and expressions in an event read the same pre-event state; writes commit in declaration order. Invalid event processing rolls back its meter changes and disables that character's runtime rules until reload. Read the server log for the cause.

Use a meter in any shared condition:

```json
{"meter": {"id": "example:resolve", "min": 50, "max": 100}}
```

Add translations for meter IDs used in requirement descriptions. Compose conditions with `all`, `any` and `not` as usual.

## Numeric expressions

Expressions accept numbers, `{"source":"meter:example:resolve"}`, and `add`, `multiply`, `min`, `max` arrays. Curves map an input to configured points:

```json
{"curve": {"input": {"source": "meter:example:resolve"}, "points": [[0, 1], [100, 1.5]], "interpolation": "smooth"}}
```

Points must have increasing inputs; endpoints clamp outside their range. Interpolation is `linear` (default) or `smooth`. Source availability depends on the consumer; missing sources are errors.

## Stat factors and form maintenance

Add a `behaviors` object to the same file:

```json
{
  "behaviors": {
    "modifiers": [{
      "when": {"content": "example:training"},
      "factors": {"PWR": {"curve": {"input": {"source": "meter:example:resolve"}, "points": [[0, 1], [100, 1.5]]}}}
    }],
    "forms": {
      "path.focused": {"when": {"energyPercent": {"min": 10}}, "durationTicks": 600, "cooldownTicks": 100, "fallback": "path.previous"}
    }
  }
}
```

Replace form IDs with existing forms. Factors multiply the normal result; supported targets are `STR`, `SKP`, `RES`, `DEF`, `STM`, `VIT`, `PWR`, `ENE`, `SPEED`, `energyDrain`, `staminaDrain` and `healthDrain`. Factors must be nonnegative. Available inputs are declared meters, `state.formTicks`, `state.stackTicks`, `self.healthPercent` and the attack sources below.

Maintenance checks on the server each tick. A failed condition or expired duration clears the form/stack, starts its cooldown and attempts the optional fallback using ordinary activation requirements. `durationTicks: -1` means unlimited. Main and stack ages are separate. An omitted maintenance entry adds no maintenance rule.

## Attack windows

```json
{"behaviors": {"attacks": {"your_technique_id": {"activeTicks": 2, "recoveryTicks": 12}}}}
```

Use the technique's actual registry key; existing keys may be unnamespaced. Defaults are one active tick and no recovery. Active accepts 1..72000 and recovery 0..72000 ticks. These windows provide rule inputs; they do not extend projectile lifetime or add an input lock.

`attack.charging`, `attack.active` and `attack.recovery` are 0/1. `attack.ticks` is elapsed time in the current phase. `attack.charge` is charge percent, 0..200, retained through active/recovery. A new charge replaces the current window. Phases synchronize to clients and reset on character load/clone or runtime reload.

## Native events and inputs

| Event | Trigger |
| --- | --- |
| `dmz:tick` | Created character tick after meter decay |
| `dmz:form_changed` | Main or stack change, after form-scope reset |
| `dmz:combat_enter` | First damaging contact out of combat |
| `dmz:combat_exit` | Combat inactivity timeout |
| `dmz:damage_received`, `dmz:damage_dealt` | Committed server damage |
| `dmz:evaded` | Successful configured dodge |
| `dmz:attack_started`, `dmz:attack_released` | Charged Ki technique start/release |
| `dmz:attack_cancelled` | Charge interrupted |
| `dmz:attack_recovery`, `dmz:attack_finished` | Configured phase transition |

Damage events expose `event.damage`, `event.originalDamage` and `event.blockedDamage`; `event.damage` is final committed damage. JSON event actions can read declared meters and attack sources. Attack cancellation/finish events observe idle. Attack adapters currently cover charged Ki techniques, not all strike/basic-blast paths.

## Force-relative dodge

```json
{
  "reactions": [{
    "id": "example:instinct",
    "when": {"content": "example:instinct"},
    "attacks": ["melee", "projectile"],
    "powerSource": "battlePower",
    "hitChance": {"curve": {"input": {"source": "event.powerRatio"}, "points": [[0.5, 0], [1, 0.5], [2, 1]]}},
    "distance": 1,
    "staminaCost": 0.02,
    "energyCost": 0,
    "cooldownTicks": 10
  }]
}
```

This is the probability of being hit, not of evading. `event.powerRatio` is attacker power divided by defender power: a stronger defender lowers hit chance. Curve endpoints can produce 0% and 100%. Both powers zero yields ratio 1. Expressions may also read declared meters. `powerSource` accepts `battlePower`, `STR`, `PWR` or `SPEED`. Other living entities use native battle power when supplied, otherwise max health as a fallback score.

The first matching reaction replaces the native speed-dodge roll for that attack category. Environment damage is excluded. Successful evasion needs sufficient resources, cooldown readiness and a loaded, bounded, collision-free displacement path; a wall cannot grant free damage cancellation. Energy/stamina costs are fractions of maximum resources in 0..1; distance accepts 0.1..4 blocks and cooldown 0..72000 ticks. Animation and position are synchronized by the server.

For optional scripts that use the same meters, see [Lua scripts](Lua-scripts).
